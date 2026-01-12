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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

@Controller("/api/usuarios")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app" })
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

    /**
     * CADASTRO DE USUÁRIO
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

            if ("PERSONAL".equalsIgnoreCase(requesterRole)) {
                usuario.setRole("USER");
                usuario.setPersonalId(requesterId);
            } else if ("ADMIN".equalsIgnoreCase(requesterRole)) {
                if (usuario.getRole() == null)
                    usuario.setRole("USER");
            } else {
                usuario.setRole("USER");
            }

            // Hash da senha com BCrypt antes de salvar
            if (usuario.getSenha() != null && !usuario.getSenha().isEmpty()) {
                usuario.setSenha(BCrypt.hashpw(usuario.getSenha(), BCrypt.gensalt()));
            }

            usuario.setCredits(10);
            Usuario novoUsuario = usuarioRepository.save(usuario);

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

            return HttpResponse.created(novoUsuario);

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao cadastrar: " + e.getMessage()));
        }
    }

    /**
     * CONSUMIR CRÉDITO (-1)
     * Prioridade: subscription_credits primeiro, depois purchased_credits
     */
    @Post("/consume-credit/{userId}")
    @Transactional
    public HttpResponse<?> consumirCredito(
            @PathVariable Long userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }

        return usuarioRepository.findById(userId).map(user -> {
            int subCredits = user.getSubscriptionCredits() != null ? user.getSubscriptionCredits() : 0;
            int purCredits = user.getPurchasedCredits() != null ? user.getPurchasedCredits() : 0;
            int totalCredits = subCredits + purCredits;

            if (totalCredits <= 0) {
                return HttpResponse.status(HttpStatus.PAYMENT_REQUIRED)
                        .body(Map.of("message", "Saldo insuficiente."));
            }

            // Prioridade: debitar primeiro de subscription_credits
            if (subCredits > 0) {
                usuarioRepository.consumeSubscriptionCredit(userId);
            } else {
                usuarioRepository.consumePurchasedCredit(userId);
            }

            return HttpResponse.ok(Map.of(
                    "message", "Crédito debitado com sucesso",
                    "novoSaldo", totalCredits - 1,
                    "subscriptionCredits", subCredits > 0 ? subCredits - 1 : 0,
                    "purchasedCredits", subCredits > 0 ? purCredits : purCredits - 1));
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
            // CORREÇÃO: Usando a query direta para não disparar validação de e-mail
            // duplicado
            usuarioRepository.executeAddCredits(userId, amount);

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

                List<Treino> treinos = treinoRepository.findByUserIdOrderByCreatedAtDesc(usuario.getId().toString());
                if (!treinos.isEmpty()) {
                    usuario.setLatestWorkout(treinos.get(0));
                }
                return HttpResponse.ok(usuario);
            }
        }
        return HttpResponse.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Credenciais inválidas."));
    }

    /**
     * LISTAGEM DE USUÁRIOS
     */
    @Get("/")
    public HttpResponse<?> listar(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        if ("ADMIN".equalsIgnoreCase(requesterRole))
            return HttpResponse.ok(usuarioRepository.findAll());
        if ("PERSONAL".equalsIgnoreCase(requesterRole))
            return HttpResponse.ok(usuarioRepository.findByPersonalId(requesterId));
        return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Sem permissão."));
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
}