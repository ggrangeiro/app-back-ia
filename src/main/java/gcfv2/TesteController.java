package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

import gcfv2.dto.ProfessorDTO;

@Controller("/api/usuarios")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app" })
public class TesteController {

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private ExerciseRepository exerciseRepository;

    @Inject
    private UsuarioExercicioRepository usuarioExercicioRepository;

    @Inject
    private TreinoRepository treinoRepository;

    @Inject
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Inject
    private EmailService emailService;

    @Inject
    private CreditConsumptionHistoryRepository creditHistoryRepository;

    @Inject
    private ActivityLogService activityLogService;

    @Inject
    private AtividadeProfessorRepository atividadeProfessorRepository;

    /**
     * CADASTRO DE USUÁRIO
     * Suporta criação de alunos (USER) e professores (PROFESSOR)
     * 
     * Para criar professor:
     * - requesterRole deve ser PERSONAL ou ADMIN
     * - body.role deve ser "professor"
     * - body.managerId deve ser o ID do personal (ou requesterId se PERSONAL)
     */
    @Post("/")
    @Transactional
    public HttpResponse<?> cadastrar(
            @Body Usuario usuario,
            @Nullable @QueryValue Long requesterId,
            @Nullable @QueryValue String requesterRole) {
        try {
            if (usuarioRepository.existsByEmail(usuario.getEmail())) {
                return HttpResponse.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "Este e-mail já está em uso."));
            }

            // Lógica para criação de PROFESSOR
            if ("PROFESSOR".equalsIgnoreCase(usuario.getRole())) {
                // Apenas PERSONAL ou ADMIN podem criar professores
                if (!"PERSONAL".equalsIgnoreCase(requesterRole) && !"ADMIN".equalsIgnoreCase(requesterRole)) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message",
                                    "Apenas Personal Trainers ou Administradores podem cadastrar professores."));
                }

                // Definir managerId
                if ("PERSONAL".equalsIgnoreCase(requesterRole)) {
                    usuario.setManagerId(requesterId);
                } else if (usuario.getManagerId() == null) {
                    return HttpResponse.badRequest(Map.of("message", "managerId é obrigatório para criar professor."));
                }

                usuario.setRole("PROFESSOR");
                usuario.setAccessLevel("FULL");
            }
            // Lógica para criação de USER (aluno)
            else if ("PERSONAL".equalsIgnoreCase(requesterRole)) {
                usuario.setRole("USER");
                usuario.setPersonalId(requesterId);
            }
            // Professor criando aluno
            else if ("PROFESSOR".equalsIgnoreCase(requesterRole)) {
                usuario.setRole("USER");

                // Buscar o professor para obter o managerId (Personal)
                Optional<Usuario> professorOpt = usuarioRepository.findById(requesterId);
                if (professorOpt.isPresent() && professorOpt.get().getManagerId() != null) {
                    usuario.setPersonalId(professorOpt.get().getManagerId());
                } else {
                    return HttpResponse.badRequest(Map.of("message", "Erro: Professor não possui Personal vinculado."));
                }
            } else if ("ADMIN".equalsIgnoreCase(requesterRole)) {
                if (usuario.getRole() == null)
                    usuario.setRole("USER");
            } else {
                if ("PERSONAL".equalsIgnoreCase(usuario.getRole())) {
                    usuario.setRole("PERSONAL");
                } else {
                    usuario.setRole("USER");
                }
            }

            // Hash da senha com BCrypt antes de salvar
            String rawPassword = usuario.getSenha();
            if (rawPassword != null && !rawPassword.isEmpty()) {
                usuario.setSenha(BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
            }

            usuario.setCredits(0);

            // Definir plano FREE por padrão para novos usuários
            if (usuario.getPlanType() == null) {
                usuario.setPlanType("FREE");
            }
            if (usuario.getAccessLevel() == null) {
                if ("PERSONAL".equalsIgnoreCase(requesterRole) && !"PROFESSOR".equalsIgnoreCase(usuario.getRole())) {
                    usuario.setAccessLevel("READONLY");
                } else {
                    usuario.setAccessLevel("FULL");
                }
            }
            if (usuario.getSubscriptionStatus() == null) {
                usuario.setSubscriptionStatus("INACTIVE");
            }

            Usuario novoUsuario = usuarioRepository.save(usuario);

            // Não copia exercícios para professores
            if (!"PROFESSOR".equalsIgnoreCase(novoUsuario.getRole())) {
                List<Exercise> exerciciosCatalogo = exerciseRepository.findByActiveTrueOrderByNameAsc();
                Set<String> processedExercises = new HashSet<>();
                for (Exercise ex : exerciciosCatalogo) {
                    if (ex.getName() != null && !processedExercises.contains(ex.getName())) {
                        UsuarioExercicio ue = new UsuarioExercicio();
                        ue.setExercicio(ex.getName());
                        ue.setUsuario(novoUsuario);
                        usuarioExercicioRepository.save(ue);
                        processedExercises.add(ex.getName());
                    }
                }
            }

            // Registrar atividade se professor criou aluno
            if ("PROFESSOR".equalsIgnoreCase(requesterRole) && "USER".equalsIgnoreCase(novoUsuario.getRole())) {
                activityLogService.logActivity(
                        requesterId,
                        requesterRole,
                        "STUDENT_CREATED",
                        novoUsuario.getId(),
                        novoUsuario.getNome(),
                        "USER",
                        novoUsuario.getId());
            }

            // Enviar e-mail de boas-vindas
            emailService.sendWelcomeEmail(novoUsuario.getEmail(), novoUsuario.getNome(), novoUsuario.getRole(),
                    rawPassword);

            return HttpResponse.created(novoUsuario);

        }

        catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao cadastrar: " + e.getMessage()));
        }
    }

    /**
     * ATUALIZAR USUÁRIO
     */
    @Put("/{id}")
    @Transactional
    public HttpResponse<?> atualizar(
            @PathVariable Long id,
            @Body Usuario atualizacao,
            @Nullable @QueryValue Long requesterId,
            @Nullable @QueryValue String requesterRole) {

        try {
            if (requesterId == null || requesterRole == null) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "requesterId e requesterRole são obrigatórios."));
            }

            if (!usuarioRepository.hasPermission(requesterId, requesterRole, id.toString())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
            }

            return usuarioRepository.findById(id).map(user -> {
                if (atualizacao.getNome() != null)
                    user.setNome(atualizacao.getNome());

                if (atualizacao.getMethodology() != null)
                    user.setMethodology(atualizacao.getMethodology());

                if (atualizacao.getCommunicationStyle() != null)
                    user.setCommunicationStyle(atualizacao.getCommunicationStyle());

                // Apenas ADMIN muda Role
                if (atualizacao.getRole() != null && "ADMIN".equalsIgnoreCase(requesterRole)) {
                    user.setRole(atualizacao.getRole());
                }

                // Personal/Admin mudam AccessLevel
                boolean isPrivileged = "PERSONAL".equalsIgnoreCase(requesterRole)
                        || "ADMIN".equalsIgnoreCase(requesterRole);
                if (isPrivileged && atualizacao.getAccessLevel() != null) {
                    user.setAccessLevel(atualizacao.getAccessLevel());
                }

                if (atualizacao.getPrimaryColor() != null)
                    user.setPrimaryColor(atualizacao.getPrimaryColor());
                if (atualizacao.getSecondaryColor() != null)
                    user.setSecondaryColor(atualizacao.getSecondaryColor());
                if (atualizacao.getBackgroundColor() != null)
                    user.setBackgroundColor(atualizacao.getBackgroundColor());
                if (atualizacao.getSurfaceColor() != null)
                    user.setSurfaceColor(atualizacao.getSurfaceColor());

                Usuario salvo = usuarioRepository.update(user);
                return HttpResponse.ok(salvo);
            }).orElse(HttpResponse.notFound());

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao atualizar: " + e.getMessage()));
        }
    }

    /**
     * CONSUMIR CRÉDITO
     * Lógica por plano:
     * - STARTER: 10 gerações gratuitas de dieta/treino por mês, depois cobra 1
     * crédito
     * - PRO: Cada dieta/treino custa 3 créditos
     * - STUDIO: Cada dieta/treino custa 2 créditos
     * - FREE: Sempre cobra 1 crédito
     * - ANALISE: Sempre cobra 1 crédito (independente do plano)
     * 
     * Prioridade de débito: subscription_credits primeiro, depois purchased_credits
     * 
     * Para PROFESSOR: consome créditos do Personal (manager)
     */
    @Post("/consume-credit/{userId}")
    @Transactional
    public HttpResponse<?> consumirCredito(
            @PathVariable Long userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole,
            @QueryValue String reason,
            @Nullable @QueryValue String analysisType) {

        // Validar reason
        if (reason == null || (!reason.equals("DIETA") && !reason.equals("TREINO") && !reason.equals("ANALISE"))) {
            return HttpResponse
                    .badRequest(Map.of("message", "Parâmetro 'reason' inválido. Use: DIETA, TREINO ou ANALISE"));
        }

        // Validar analysisType para ANALISE
        if ("ANALISE".equals(reason) && (analysisType == null || analysisType.isBlank())) {
            return HttpResponse
                    .badRequest(Map.of("message", "Parâmetro 'analysisType' é obrigatório quando reason=ANALISE"));
        }

        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }

        return usuarioRepository.findById(userId).map(user -> {
            // Check Access Level
            if ("USER".equalsIgnoreCase(requesterRole) && "READONLY".equalsIgnoreCase(user.getAccessLevel())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message",
                                "Seu nível de acesso não permite esta ação. Solicite ao seu Personal."));
            }

            // Determinar quem é o dono dos créditos
            // PROFESSOR usa créditos do Personal (manager)
            Long creditOwnerId = requesterId;
            Usuario creditOwner = null;

            if ("PROFESSOR".equalsIgnoreCase(requesterRole)) {
                Optional<Usuario> professorOpt = usuarioRepository.findById(requesterId);
                if (professorOpt.isPresent() && professorOpt.get().getManagerId() != null) {
                    creditOwnerId = professorOpt.get().getManagerId();
                }
            }

            Optional<Usuario> creditOwnerOpt = usuarioRepository.findById(creditOwnerId);
            if (creditOwnerOpt.isEmpty()) {
                return HttpResponse.serverError(Map.of("message", "Erro ao identificar proprietário dos créditos."));
            }
            creditOwner = creditOwnerOpt.get();

            // Determinar o plano a considerar:
            // Para PROFESSOR, sempre usar plano do manager
            String planTypeToCheck = creditOwner.getPlanType() != null ? creditOwner.getPlanType() : "FREE";
            boolean isPrivileged = "PERSONAL".equalsIgnoreCase(requesterRole)
                    || "ADMIN".equalsIgnoreCase(requesterRole)
                    || "PROFESSOR".equalsIgnoreCase(requesterRole);

            // Para PERSONAL/ADMIN gerando para outro (não professor)
            if (("PERSONAL".equalsIgnoreCase(requesterRole) || "ADMIN".equalsIgnoreCase(requesterRole))
                    && !requesterId.equals(userId)) {
                Optional<Usuario> requesterOpt = usuarioRepository.findById(requesterId);
                if (requesterOpt.isPresent()) {
                    planTypeToCheck = requesterOpt.get().getPlanType() != null ? requesterOpt.get().getPlanType()
                            : "FREE";
                    creditOwner = requesterOpt.get();
                    creditOwnerId = requesterId;
                }
            }

            int creditsToCharge = 1; // Default: 1 crédito
            boolean wasFree = false;
            String creditSourceType = "FREE";

            // Lógica por motivo e plano
            if ("DIETA".equals(reason) || "TREINO".equals(reason)) {
                // STUDIO: 2 créditos por geração
                if ("STUDIO".equalsIgnoreCase(planTypeToCheck)) {
                    creditsToCharge = 2;
                }
                // PRO: 3 créditos por geração
                else if ("PRO".equalsIgnoreCase(planTypeToCheck)) {
                    creditsToCharge = 3;
                }
                // STARTER: 4 créditos por geração
                else if ("STARTER".equalsIgnoreCase(planTypeToCheck)) {
                    creditsToCharge = 4;
                }
                // FREE: 5 créditos por geração
                else {
                    creditsToCharge = 5;
                }
            }
            // ANALISE: sempre 1 crédito (já é o default)

            int creditsConsumed = 0;

            if (creditsToCharge > 0) {
                int subCredits = creditOwner.getSubscriptionCredits() != null ? creditOwner.getSubscriptionCredits()
                        : 0;
                int purCredits = creditOwner.getPurchasedCredits() != null ? creditOwner.getPurchasedCredits() : 0;
                int totalCredits = subCredits + purCredits;

                if (totalCredits < creditsToCharge) {
                    return HttpResponse.status(HttpStatus.PAYMENT_REQUIRED)
                            .body(Map.of("message", "Saldo insuficiente. Você precisa de " + creditsToCharge
                                    + " créditos, mas tem apenas " + totalCredits + "."));
                }

                // Debitar créditos (prioriza subscription, depois purchased)
                int remaining = creditsToCharge;
                while (remaining > 0) {
                    // Recalcular saldos atuais
                    Optional<Usuario> currentOwner = usuarioRepository.findById(creditOwnerId);
                    if (currentOwner.isEmpty())
                        break;

                    int currentSub = currentOwner.get().getSubscriptionCredits() != null
                            ? currentOwner.get().getSubscriptionCredits()
                            : 0;
                    int currentPur = currentOwner.get().getPurchasedCredits() != null
                            ? currentOwner.get().getPurchasedCredits()
                            : 0;

                    if (currentSub > 0) {
                        usuarioRepository.consumeSubscriptionCredit(creditOwnerId);
                        creditSourceType = "SUBSCRIPTION";
                    } else if (currentPur > 0) {
                        usuarioRepository.consumePurchasedCredit(creditOwnerId);
                        creditSourceType = "PURCHASED";
                    }
                    remaining--;
                }
                creditsConsumed = creditsToCharge;
            }

            // Registrar histórico de consumo (sempre no usuário alvo para tracking)
            CreditConsumptionHistory history = new CreditConsumptionHistory(
                    userId,
                    reason,
                    analysisType,
                    creditsConsumed,
                    wasFree,
                    creditSourceType);
            creditHistoryRepository.save(history);

            // Calcular novo saldo do creditOwner para resposta
            Optional<Usuario> updatedOwner = usuarioRepository.findById(creditOwnerId);
            int novoSaldo = 0;
            if (updatedOwner.isPresent()) {
                int subCredits = updatedOwner.get().getSubscriptionCredits() != null
                        ? updatedOwner.get().getSubscriptionCredits()
                        : 0;
                int purCredits = updatedOwner.get().getPurchasedCredits() != null
                        ? updatedOwner.get().getPurchasedCredits()
                        : 0;
                novoSaldo = subCredits + purCredits;
            }

            return HttpResponse.ok(Map.of(
                    "message", creditsConsumed + " crédito(s) debitado(s) com sucesso",
                    "novoSaldo", novoSaldo,
                    "creditsConsumed", creditsConsumed,
                    "reason", reason,
                    "creditSource", creditSourceType,
                    "creditOwnerId", creditOwnerId));
        }).orElse(HttpResponse.notFound());
    }

    /**
     * HISTÓRICO DE CONSUMO DE CRÉDITOS
     * Retorna histórico completo e resumo de uso
     */
    @Get("/credit-history/{userId}")
    public HttpResponse<?> getCreditHistory(
            @PathVariable Long userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }

        return usuarioRepository.findById(userId).map(user -> {
            List<CreditConsumptionHistory> history = creditHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);

            long totalConsumed = creditHistoryRepository.sumCreditsConsumedByUserId(userId);
            long freeGenerationsUsed = creditHistoryRepository.countFreeGenerationsThisMonth(userId);

            String planType = user.getPlanType() != null ? user.getPlanType() : "FREE";
            int freeGenerationsRemaining = "STARTER".equalsIgnoreCase(planType)
                    ? Math.max(0, 10 - (int) freeGenerationsUsed)
                    : ("PRO".equalsIgnoreCase(planType) || "STUDIO".equalsIgnoreCase(planType) ? -1 : 0);

            return HttpResponse.ok(Map.of(
                    "history", history,
                    "summary", Map.of(
                            "totalConsumed", totalConsumed,
                            "freeGenerationsUsed", freeGenerationsUsed,
                            "freeGenerationsRemaining", freeGenerationsRemaining,
                            "consumedThisMonth", creditHistoryRepository.sumCreditsConsumedThisMonth(userId))));
        }).orElse(HttpResponse.notFound());
    }

    /**
     * RECARGA DE CRÉDITOS (ADMIN APENAS)
     * CORREÇÃO: Removido o uso de .save() para evitar erro de Duplicate Entry no
     * Email.
     */
    @Post("/admin/add-credits/{userId}")
    @Transactional
    public HttpResponse<?> addCredits(
            @PathVariable Long userId,
            @Body Map<String, Integer> body,
            @QueryValue String requesterRole) {

        if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Apenas administradores."));
        }

        Integer amount = body.get("amount");
        if (amount == null || amount <= 0) {
            return HttpResponse.badRequest(Map.of("message", "Quantidade inválida."));
        }

        return usuarioRepository.findById(userId).map(user -> {
            // CORREÇÃO: Usando a query de créditos avulsos que atualiza tanto
            // purchased_credits quanto credits (legado)
            usuarioRepository.addPurchasedCredits(userId, amount);

            // Recalcular saldo para resposta
            int novoSaldo = (user.getCredits() != null ? user.getCredits() : 0) + amount;

            return HttpResponse.ok(Map.of(
                    "message", "Recarga realizada com sucesso",
                    "novoSaldo", novoSaldo));
        }).orElse(HttpResponse.notFound());
    }

    /**
     * LOGIN
     * Suporta senhas em texto puro (legado) e BCrypt.
     * Quando login com texto puro, migra automaticamente para BCrypt.
     * 
     * Para PROFESSOR: retorna créditos e plano do seu Personal (manager)
     */
    @Post("/login")
    @Transactional
    public HttpResponse<?> login(@Body Map<String, String> credentials) {
        String email = credentials.get("email");
        String senha = credentials.get("senha");

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String storedPassword = usuario.getSenha();

            boolean senhaValida = checkPassword(senha, storedPassword);

            if (senhaValida) {
                // Auto-migração: se senha ainda está em texto puro, converte para BCrypt
                if (storedPassword != null && !storedPassword.startsWith("$2")) {
                    String hashedPassword = BCrypt.hashpw(senha, BCrypt.gensalt());
                    usuarioRepository.updatePassword(usuario.getId(), hashedPassword);
                }

                // Definir latestWorkout (mantido da lógica anterior)
                List<Treino> treinos = treinoRepository.findByUserIdOrderByCreatedAtDesc(usuario.getId().toString());
                if (!treinos.isEmpty()) {
                    usuario.setLatestWorkout(treinos.get(0));
                }

                // Preparar resposta com Plano e Usage (padronizado com /api/me)
                // generationsLimit: -1 significa custo em créditos (não ilimitado)
                // generationCost: custo em créditos por geração
                Map<String, Map<String, Object>> PLANS = Map.of(
                        "FREE", Map.of("generationsLimit", -1, "generationCost", 5),
                        "STARTER", Map.of("generationsLimit", -1, "generationCost", 4),
                        "PRO", Map.of("generationsLimit", -1, "generationCost", 3),
                        "STUDIO", Map.of("generationsLimit", -1, "generationCost", 2));

                // PROFESSOR usa créditos do Personal (manager)
                Usuario creditSource = usuario;
                Long managerId = null;
                if ("PROFESSOR".equalsIgnoreCase(usuario.getRole()) && usuario.getManagerId() != null) {
                    managerId = usuario.getManagerId();
                    Optional<Usuario> managerOpt = usuarioRepository.findById(usuario.getManagerId());
                    if (managerOpt.isPresent()) {
                        creditSource = managerOpt.get();
                    }
                }

                String userPlanType = creditSource.getPlanType() != null ? creditSource.getPlanType() : "FREE";
                Map<String, Object> planInfo = PLANS.getOrDefault(userPlanType, PLANS.get("FREE"));
                int generationsLimit = (int) planInfo.get("generationsLimit");

                int subCredits = creditSource.getSubscriptionCredits() != null ? creditSource.getSubscriptionCredits()
                        : 0;
                int purCredits = creditSource.getPurchasedCredits() != null ? creditSource.getPurchasedCredits() : 0;
                int totalCredits = subCredits + purCredits;

                // Se for aluno, buscar a logo do personal para o login
                String brandLogo = usuario.getBrandLogo();
                if ("USER".equalsIgnoreCase(usuario.getRole()) && usuario.getPersonalId() != null
                        && brandLogo == null) {
                    brandLogo = usuarioRepository.findById(usuario.getPersonalId())
                            .map(Usuario::getBrandLogo)
                            .orElse(null);
                }
                // Professor também herda brandLogo do manager
                if ("PROFESSOR".equalsIgnoreCase(usuario.getRole()) && usuario.getManagerId() != null
                        && brandLogo == null) {
                    brandLogo = usuarioRepository.findById(usuario.getManagerId())
                            .map(Usuario::getBrandLogo)
                            .orElse(null);
                }

                // Construir resposta - incluir managerId para professor
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("id", usuario.getId());
                response.put("name", usuario.getNome() != null ? usuario.getNome() : "");
                response.put("email", usuario.getEmail() != null ? usuario.getEmail() : "");
                response.put("role", usuario.getRole() != null ? usuario.getRole() : "USER");
                response.put("avatar", usuario.getAvatar() != null ? usuario.getAvatar() : "");
                response.put("brandLogo", brandLogo != null ? brandLogo : "");
                response.put("accessLevel", usuario.getAccessLevel() != null ? usuario.getAccessLevel() : "FULL");
                response.put("methodology", usuario.getMethodology());
                response.put("communicationStyle", usuario.getCommunicationStyle());

                if (managerId != null) {
                    response.put("managerId", managerId);
                }

                if (usuario.getPersonalId() != null) {
                    response.put("personalId", usuario.getPersonalId());
                }

                response.put("plan", Map.of(
                        "type", creditSource.getPlanType() != null ? creditSource.getPlanType() : "FREE",
                        "status",
                        creditSource.getSubscriptionStatus() != null ? creditSource.getSubscriptionStatus()
                                : "INACTIVE",
                        "renewsAt",
                        creditSource.getSubscriptionEndDate() != null ? creditSource.getSubscriptionEndDate().toString()
                                : ""));

                int generationCost = (int) planInfo.get("generationCost");

                response.put("usage", Map.of(
                        "credits", totalCredits,
                        "subscriptionCredits", subCredits,
                        "purchasedCredits", purCredits,
                        "generations",
                        creditSource.getGenerationsUsedCycle() != null ? creditSource.getGenerationsUsedCycle() : 0,
                        "generationsLimit", generationsLimit,
                        "generationCost", generationCost));

                return HttpResponse.ok(response);
            }
        }
        return HttpResponse.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Credenciais inválidas."));
    }

    /**
     * STATUS (PLANO E CRÉDITOS) - REFRESH
     * Endpoint dedicado para atualizar a UI com dados recentes de plano e créditos
     * sem a necessidade de fazer logout/login.
     */
    @Get("/status")
    public HttpResponse<?> getStatus(@QueryValue Long requesterId) {
        return usuarioRepository.findById(requesterId).map(usuario -> {

            // 1. Definição dos Planos (Mesma lógica do Login)
            // generationsLimit: -1 significa custo em créditos (não ilimitado)
            // generationCost: custo em créditos por geração
            Map<String, Map<String, Object>> PLANS = Map.of(
                    "FREE", Map.of("generationsLimit", -1, "generationCost", 5),
                    "STARTER", Map.of("generationsLimit", -1, "generationCost", 4),
                    "PRO", Map.of("generationsLimit", -1, "generationCost", 3),
                    "STUDIO", Map.of("generationsLimit", -1, "generationCost", 2));

            // 2. Determinar Fonte de Créditos (Professor -> Personal)
            Usuario creditSource = usuario;
            if ("PROFESSOR".equalsIgnoreCase(usuario.getRole()) && usuario.getManagerId() != null) {
                Optional<Usuario> managerOpt = usuarioRepository.findById(usuario.getManagerId());
                if (managerOpt.isPresent()) {
                    creditSource = managerOpt.get();
                }
            }

            // 3. Calcular Dados
            String currentPlan = creditSource.getPlanType() != null ? creditSource.getPlanType() : "FREE";
            Map<String, Object> planInfo = PLANS.getOrDefault(currentPlan, PLANS.get("FREE"));
            int generationsLimit = (int) planInfo.get("generationsLimit");

            int subCredits = creditSource.getSubscriptionCredits() != null ? creditSource.getSubscriptionCredits() : 0;
            int purCredits = creditSource.getPurchasedCredits() != null ? creditSource.getPurchasedCredits() : 0;

            // Legacy Fix: Se sub + pur != total, assumir que a diferença é crédito comprado
            // (legado)
            int totalCreditsColumn = creditSource.getCredits() != null ? creditSource.getCredits() : 0;
            if (subCredits + purCredits != totalCreditsColumn) {
                purCredits = Math.max(0, totalCreditsColumn - subCredits);
            }

            int totalCredits = subCredits + purCredits;

            // 4. Montar Resposta
            Map<String, Object> response = new java.util.HashMap<>();

            response.put("id", usuario.getId());
            response.put("role", usuario.getRole());
            response.put("accessLevel", usuario.getAccessLevel() != null ? usuario.getAccessLevel() : "FULL");
            response.put("methodology", usuario.getMethodology());
            response.put("communicationStyle", usuario.getCommunicationStyle());

            if (usuario.getPersonalId() != null) {
                response.put("personalId", usuario.getPersonalId());
            }

            response.put("plan", Map.of(
                    "type", currentPlan,
                    "status",
                    creditSource.getSubscriptionStatus() != null ? creditSource.getSubscriptionStatus() : "INACTIVE",
                    "renewsAt",
                    creditSource.getSubscriptionEndDate() != null ? creditSource.getSubscriptionEndDate().toString()
                            : ""));

            int generationCost = (int) planInfo.get("generationCost");

            response.put("usage", Map.of(
                    "credits", totalCredits,
                    "subscriptionCredits", subCredits,
                    "purchasedCredits", purCredits,
                    "generations",
                    creditSource.getGenerationsUsedCycle() != null ? creditSource.getGenerationsUsedCycle() : 0,
                    "generationsLimit", generationsLimit,
                    "generationCost", generationCost));

            return HttpResponse.ok(response);

        }).orElse(HttpResponse.notFound());
    }

    /**
     * LISTAGEM DE USUÁRIOS
     * 
     * Para PROFESSOR: retorna todos os alunos do ecossistema do seu Personal
     * (alunos diretos do personal + alunos de todos os professores do personal)
     */
    @Get("/")
    public HttpResponse<?> listar(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        if ("ADMIN".equalsIgnoreCase(requesterRole))
            return HttpResponse.ok(usuarioRepository.findAll());

        if ("PERSONAL".equalsIgnoreCase(requesterRole)) {
            List<Usuario> usuarios = new ArrayList<>();
            // 1. Buscar alunos diretos
            usuarios.addAll(usuarioRepository.findByPersonalId(requesterId));
            // 2. Buscar professores da equipe (que têm manager_id = requesterId)
            usuarios.addAll(usuarioRepository.findProfessorsByManagerId(requesterId));
            return HttpResponse.ok(usuarios);
        }

        if ("PROFESSOR".equalsIgnoreCase(requesterRole)) {
            // Professor vê todos os alunos do ecossistema do seu Personal
            var professorOpt = usuarioRepository.findById(requesterId);
            if (professorOpt.isEmpty() || professorOpt.get().getManagerId() == null) {
                return HttpResponse.ok(usuarioRepository.findByPersonalId(requesterId));
            }

            Long managerId = professorOpt.get().getManagerId();

            // Buscar todos os alunos: do personal + de todos os professores do personal
            List<Usuario> todosAlunos = new ArrayList<>();

            // Alunos diretos do personal
            todosAlunos.addAll(usuarioRepository.findByPersonalId(managerId));

            // Alunos de todos os professores do mesmo personal
            List<Usuario> professores = usuarioRepository.findProfessorsByManagerId(managerId);
            for (Usuario prof : professores) {
                List<Usuario> alunosProf = usuarioRepository.findByPersonalId(prof.getId());
                todosAlunos.addAll(alunosProf);
            }

            return HttpResponse.ok(todosAlunos);
        }

        return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Sem permissão."));
    }

    /**
     * LISTAGEM DE PROFESSORES DE UM PERSONAL
     * 
     * GET /api/usuarios/professors?managerId={personalId}
     * 
     * Retorna lista de professores com estatísticas:
     * - studentsCount: número de alunos
     * - lastActivity: última atividade registrada
     */
    @Get("/professors")
    public HttpResponse<?> listarProfessores(
            @QueryValue Long managerId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        // Apenas o próprio personal ou admin pode listar seus professores
        if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
            if (!"PERSONAL".equalsIgnoreCase(requesterRole) || !requesterId.equals(managerId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado."));
            }
        }

        try {
            List<Usuario> professors = usuarioRepository.findProfessorsByManagerId(managerId);

            List<ProfessorDTO> result = new ArrayList<>();
            for (Usuario prof : professors) {
                long studentsCount = usuarioRepository.countStudentsByProfessorId(prof.getId());

                String lastActivity = null;
                var lastAct = atividadeProfessorRepository.findLastActivityByProfessorId(prof.getId());
                if (lastAct.isPresent() && lastAct.get().getCreatedAt() != null) {
                    lastActivity = lastAct.get().getCreatedAt().toString();
                }

                ProfessorDTO dto = new ProfessorDTO(
                        prof.getId(),
                        prof.getNome(),
                        prof.getEmail(),
                        prof.getRole(),
                        prof.getManagerId(),
                        prof.getCredits(),
                        studentsCount,
                        lastActivity,
                        prof.getAvatar());

                result.add(dto);
            }

            return HttpResponse.ok(Map.of(
                    "professors", result,
                    "total", result.size()));

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao listar professores: " + e.getMessage()));
        }
    }

    /**
     * DETALHES DO USUÁRIO
     */
    @Get("/{id}")
    public HttpResponse<?> obterUsuario(
            @PathVariable Long id,
            @Nullable @QueryValue Long requesterId,
            @Nullable @QueryValue String requesterRole) {

        if (requesterId != null && requesterRole != null) {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, id.toString())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
            }
        }

        return usuarioRepository.findById(id).map(user -> {
            // Garantir valor default se nulo (embora a entidade já trate)
            if (user.getAccessLevel() == null)
                user.setAccessLevel("FULL");
            return HttpResponse.ok(user);
        }).orElse(HttpResponse.notFound());
    }

    /**
     * LISTAR EXERCÍCIOS DO USUÁRIO
     */
    @Get("/exercises/{userId}")
    public HttpResponse<?> listarExercicios(
            @PathVariable Long userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }

        return usuarioRepository.findById(userId).map(user -> {
            List<UsuarioExercicio> lista = usuarioExercicioRepository.findByUsuario(user);
            List<Map<String, Object>> formatado = lista.stream().map(ue -> {
                String nomeBruto = ue.getExercicio();
                String nomeFormatado = (nomeBruto != null) ? nomeBruto.replace("_", " ").toLowerCase() : "Exercício";
                return Map.<String, Object>of(
                        "id", ue.getId(),
                        "exercicio", nomeBruto != null ? nomeBruto : "",
                        "nomeExibicao", nomeFormatado.substring(0, 1).toUpperCase() + nomeFormatado.substring(1));
            }).toList();
            return HttpResponse.ok(formatado);
        }).orElse(HttpResponse.notFound());
    }

    // ==================== PASSWORD MANAGEMENT ====================

    /**
     * MUDANÇA DE SENHA - Usuário logado muda sua própria senha
     * Body: { "userId": 1, "senhaAtual": "...", "novaSenha": "..." }
     */
    @Post("/change-password")
    @Transactional
    public HttpResponse<?> changePassword(@Body Map<String, Object> body) {
        Long userId = ((Number) body.get("userId")).longValue();
        String senhaAtual = (String) body.get("senhaAtual");
        String novaSenha = (String) body.get("novaSenha");

        if (userId == null || senhaAtual == null || novaSenha == null) {
            return HttpResponse.badRequest(Map.of("message", "userId, senhaAtual e novaSenha são obrigatórios."));
        }

        if (novaSenha.length() < 6) {
            return HttpResponse.badRequest(Map.of("message", "A nova senha deve ter no mínimo 6 caracteres."));
        }

        return usuarioRepository.findById(userId).map(user -> {
            // Verificar senha atual (suporta texto puro legado ou BCrypt)
            boolean senhaValida = checkPassword(senhaAtual, user.getSenha());
            if (!senhaValida) {
                return HttpResponse.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Senha atual incorreta."));
            }

            // Atualizar para nova senha com hash
            String hashedPassword = BCrypt.hashpw(novaSenha, BCrypt.gensalt());
            usuarioRepository.updatePassword(userId, hashedPassword);

            return HttpResponse.ok(Map.of("message", "Senha alterada com sucesso."));
        }).orElse(HttpResponse.notFound());
    }

    /**
     * ESQUECI MINHA SENHA - Envia e-mail com link de reset
     * Body: { "email": "user@example.com" }
     */
    @Post("/forgot-password")
    @Transactional
    public HttpResponse<?> forgotPassword(@Body Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return HttpResponse.badRequest(Map.of("message", "E-mail é obrigatório."));
        }

        // Sempre retorna sucesso para não revelar se e-mail existe
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);

        if (usuarioOpt.isPresent()) {
            Usuario user = usuarioOpt.get();

            // Deletar tokens antigos do usuário
            passwordResetTokenRepository.deleteByUserId(user.getId());

            // Criar novo token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);

            PasswordResetToken resetToken = new PasswordResetToken(token, user.getId(), expiresAt);
            passwordResetTokenRepository.save(resetToken);

            // Enviar e-mail
            emailService.sendPasswordResetEmail(email, token, user.getNome());
        }

        return HttpResponse.ok(Map.of(
                "message", "Se o e-mail existir em nossa base, você receberá instruções para redefinir sua senha."));
    }

    /**
     * RESET DE SENHA VIA TOKEN - Valida token e define nova senha
     * Body: { "token": "uuid-token", "novaSenha": "..." }
     */
    @Post("/reset-password")
    @Transactional
    public HttpResponse<?> resetPassword(@Body Map<String, String> body) {
        String token = body.get("token");
        String novaSenha = body.get("novaSenha");

        if (token == null || novaSenha == null) {
            return HttpResponse.badRequest(Map.of("message", "Token e novaSenha são obrigatórios."));
        }

        if (novaSenha.length() < 6) {
            return HttpResponse.badRequest(Map.of("message", "A nova senha deve ter no mínimo 6 caracteres."));
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return HttpResponse.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token inválido ou expirado."));
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (!resetToken.isValid()) {
            return HttpResponse.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "Token inválido ou expirado."));
        }

        // Atualizar senha
        String hashedPassword = BCrypt.hashpw(novaSenha, BCrypt.gensalt());
        usuarioRepository.updatePassword(resetToken.getUserId(), hashedPassword);

        // Marcar token como usado
        passwordResetTokenRepository.markAsUsed(resetToken.getId());

        return HttpResponse.ok(Map.of("message", "Senha redefinida com sucesso."));
    }

    /**
     * RESET POR ADMIN/PERSONAL - Reseta senha de um usuário específico
     * Query: ?requesterId=1&requesterRole=ADMIN
     * Body: { "novaSenha": "..." }
     */
    @Post("/admin/reset-password/{userId}")
    @Transactional
    public HttpResponse<?> adminResetPassword(
            @PathVariable Long userId,
            @Body Map<String, String> body,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        return processAdminReset(userId, body, requesterId, requesterRole);
    }

    /**
     * Alias para a rota administrativa (suporta formato com hífen)
     */
    @Post("/admin-reset-password/{userId}")
    @Transactional
    public HttpResponse<?> adminResetPasswordAlias(
            @PathVariable Long userId,
            @Body Map<String, String> body,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        return processAdminReset(userId, body, requesterId, requesterRole);
    }

    private HttpResponse<?> processAdminReset(
            Long userId,
            Map<String, String> body,
            Long requesterId,
            String requesterRole) {

        String novaSenha = body.get("novaSenha");

        if (novaSenha == null) {
            return HttpResponse.badRequest(Map.of("message", "novaSenha é obrigatória."));
        }

        if (novaSenha.length() < 6) {
            return HttpResponse.badRequest(Map.of("message", "A nova senha deve ter no mínimo 6 caracteres."));
        }

        // Verificar permissão: ADMIN pode tudo, PERSONAL só seus alunos
        if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
            if ("PERSONAL".equalsIgnoreCase(requesterRole)) {
                if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Você não tem permissão para resetar a senha deste usuário."));
                }
            } else {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Apenas administradores e personais podem resetar senhas."));
            }
        }

        return usuarioRepository.findById(userId).map(user -> {
            String hashedPassword = BCrypt.hashpw(novaSenha, BCrypt.gensalt());
            usuarioRepository.updatePassword(userId, hashedPassword);

            return HttpResponse.ok(Map.of(
                    "message", "Senha do usuário " + user.getNome() + " redefinida com sucesso."));
        }).orElse(HttpResponse.notFound());
    }

    // Injecting missing repositories for deletion
    @Inject
    private DietaRepository dietaRepository;
    @Inject
    private StructuredTreinoRepository structuredTreinoRepository;
    @Inject
    private StructuredDietaRepository structuredDietaRepository;
    @Inject
    private CheckinRepository checkinRepository;
    @Inject
    private HistoricoRepository historicoRepository;
    @Inject
    private SubscriptionHistoryRepository subscriptionHistoryRepository;
    @Inject
    private PaymentTransactionRepository paymentTransactionRepository;

    /**
     * EXCLUSÃO DE CONTA (LGPD)
     * Rota: DELETE /api/usuarios/{id}
     * 
     * Permissões de exclusão:
     * - O próprio usuário pode excluir sua conta
     * - O Personal Trainer atrelado ao usuário pode excluí-lo
     * - Administradores podem excluir qualquer usuário
     */
    @Delete("/{id}")
    @Transactional
    public HttpResponse<?> deleteUser(
            @PathVariable Long id,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        // 1. Buscar o usuário alvo
        Optional<Usuario> targetUserOpt = usuarioRepository.findById(id);
        if (targetUserOpt.isEmpty()) {
            return HttpResponse.notFound(Map.of("message", "Usuário não encontrado."));
        }
        Usuario targetUser = targetUserOpt.get();

        // 2. Validação de Segurança
        boolean isAdmin = "ADMIN".equalsIgnoreCase(requesterRole);
        boolean isOwner = id.equals(requesterId);

        // Verifica se o requester é o Personal Trainer atrelado ao usuário alvo
        boolean isPersonalOfUser = targetUser.getPersonalId() != null
                && targetUser.getPersonalId().equals(requesterId);

        // Verifica se o requester é o Manager do Professor alvo
        boolean isManagerOfProfessor = "PROFESSOR".equalsIgnoreCase(targetUser.getRole())
                && targetUser.getManagerId() != null
                && targetUser.getManagerId().equals(requesterId);

        if (!isAdmin && !isOwner && !isPersonalOfUser && !isManagerOfProfessor) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Você não tem permissão para excluir este usuário."));
        }

        // Continua com a exclusão usando o usuário já carregado
        Usuario user = targetUser;
        {
            try {
                String userIdStr = String.valueOf(id);

                // 2. Remover Associações (Ordem importa para evitar FK constraints se houver,
                // mas JPA/JDBC deve lidar com deletes simples)

                // Treinos V1
                treinoRepository.deleteByUserId(userIdStr);

                // Dietas V1
                dietaRepository.deleteByUserId(userIdStr);

                // Treinos V2
                structuredTreinoRepository.deleteByUserId(userIdStr);

                // Dietas V2
                structuredDietaRepository.deleteByUserId(userIdStr);

                // Checkins
                checkinRepository.deleteByUserId(userIdStr);

                // Histórico de Evolução
                historicoRepository.deleteByUserId(userIdStr);

                // Histórico de Consumo de Créditos
                creditHistoryRepository.deleteByUserId(id);

                // Exercícios do Usuário
                usuarioExercicioRepository.deleteByUsuario(user);

                // Transações de Pagamento (Opicional: Manter para auditoria? Spec diz "Remover
                // dados sensíveis")
                // Vamos deletar para cumprir "Exclusão total" pedido pelo usuário
                paymentTransactionRepository.deleteByUserId(id);

                // Tokens de Reset de Senha
                passwordResetTokenRepository.deleteByUserId(id);

                // Histórico de Assinaturas
                subscriptionHistoryRepository.deleteByUserId(id);

                // 3. Deletar Usuário
                usuarioRepository.delete(user);

                return HttpResponse.ok(Map.of("message", "Conta excluída com sucesso."));

            } catch (Exception e) {
                return HttpResponse.serverError(Map.of("message", "Erro ao excluir conta: " + e.getMessage()));
            }
        }
    }

    /**
     * Verifica se a senha informada corresponde à senha armazenada.
     * Suporta tanto senhas em texto puro (legado) quanto senhas com hash BCrypt.
     */
    private boolean checkPassword(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        // Se a senha armazenada começa com $2a$, $2b$ ou $2y$ é BCrypt
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {
            return BCrypt.checkpw(rawPassword, storedPassword);
        }

        // Caso contrário, é texto puro (legado) - compara diretamente
        return rawPassword.equals(storedPassword);
    }

    /**
     * ATUALIZAR META SEMANAL DE TREINOS
     * PUT /api/usuarios/{userId}/weekly-goal
     * Body: { "weeklyGoal": 5 }
     * 
     * Apenas o próprio usuário pode alterar sua meta.
     */
    @Put("/{userId}/weekly-goal")
    @Transactional
    public HttpResponse<?> updateWeeklyGoal(
            @PathVariable Long userId,
            @Body gcfv2.dto.checkin.WeeklyGoalRequest body,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        try {
            // Apenas o próprio usuário pode alterar sua meta
            if (!requesterId.equals(userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Apenas o próprio usuário pode alterar sua meta semanal."));
            }

            Integer weeklyGoal = body.getWeeklyGoal();
            if (weeklyGoal == null || weeklyGoal < 1 || weeklyGoal > 7) {
                return HttpResponse.badRequest(Map.of("message", "A meta semanal deve estar entre 1 e 7 dias."));
            }

            return usuarioRepository.findById(userId).map(user -> {
                usuarioRepository.updateWeeklyGoal(userId, weeklyGoal);
                return HttpResponse.ok(Map.of(
                        "success", true,
                        "weeklyGoal", weeklyGoal));
            }).orElse(HttpResponse.notFound());

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao atualizar meta semanal: " + e.getMessage()));
        }
    }
}