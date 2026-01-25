package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpMethod;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.List;

@Controller("/api/dietas")
@CrossOrigin(allowedOrigins = { "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app" }, allowedMethods = {
                HttpMethod.GET,
                HttpMethod.POST,
                HttpMethod.DELETE,
                HttpMethod.OPTIONS
        })
public class DietaController {

    @Inject
    private DietaRepository dietaRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private EmailService emailService;

    @Inject
    private ActivityLogService activityLogService;

    @Post("/")
    @Transactional
    public HttpResponse<?> salvar(@Body Dieta dieta,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, dieta.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não tem vínculo com este aluno."));
            }

            // GATEKEEPER: Verificar limites de geração do plano
            Long targetUserId = Long.parseLong(dieta.getUserId());
            var userOpt = usuarioRepository.findById(targetUserId);
            String targetUserName = null;
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                targetUserName = user.getNome();

                // Check Access Level (Leitura vs Escrita)
                if ("USER".equalsIgnoreCase(requesterRole) && "READONLY".equalsIgnoreCase(user.getAccessLevel())) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message",
                                    "Seu plano atual não permite gerar dietas. Solicite ao seu Personal."));
                }

                // Determinar plano a verificar:
                // Se requester é PERSONAL/ADMIN, usar plano do requester (não do aluno)
                String planTypeToCheck = user.getPlanType() != null ? user.getPlanType() : "FREE";
                boolean isPrivileged = "PERSONAL".equalsIgnoreCase(requesterRole)
                        || "ADMIN".equalsIgnoreCase(requesterRole)
                        || "PROFESSOR".equalsIgnoreCase(requesterRole);

                if (isPrivileged && !requesterId.equals(targetUserId)) {
                    var requesterOpt = usuarioRepository.findById(requesterId);
                    if (requesterOpt.isPresent()) {
                        planTypeToCheck = requesterOpt.get().getPlanType() != null ? requesterOpt.get().getPlanType()
                                : "FREE";
                    }
                }

                // Planos: Verificação de créditos é feita pelo consume-credit no frontend
                // FREE: 5 créditos por geração
                // STARTER: 4 créditos por geração
                // PRO: 3 créditos por geração
                // STUDIO: 2 créditos por geração
            }

            Dieta salva = dietaRepository.save(dieta);

            // Incrementar contador de gerações
            usuarioRepository.incrementGenerationsUsedCycle(Long.parseLong(dieta.getUserId()));

            // Log de atividade para professor
            activityLogService.logActivity(
                    requesterId,
                    requesterRole,
                    "DIET_GENERATED",
                    targetUserId,
                    targetUserName,
                    "DIET",
                    salva.getId());

            // Enviar e-mail de notificação APENAS se não foi o próprio aluno que criou
            if (!requesterId.equals(Long.parseLong(dieta.getUserId()))) {
                try {
                    var userOptEmail = usuarioRepository.findById(Long.parseLong(dieta.getUserId()));
                    if (userOptEmail.isPresent()) {
                        var user = userOptEmail.get();
                        String goal = (dieta.getGoal() != null && !dieta.getGoal().isEmpty()) ? dieta.getGoal()
                                : "Dieta Personalizada";
                        emailService.sendDietGeneratedEmail(user.getEmail(), user.getNome(), goal);
                    }
                } catch (Exception e) {
                    System.err.println("Erro ao enviar email de dieta: " + e.getMessage());
                }
            }

            return HttpResponse.created(salva);
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao salvar dieta: " + e.getMessage()));
        }
    }

    @Get("/{userId}")
    public HttpResponse<?> listar(@PathVariable String userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }
        return HttpResponse.ok(dietaRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    /**
     * EXCLUIR DIETA
     * Adicionado para resolver a falta do endpoint de exclusão.
     */
    @Delete("/{id}")
    @Transactional
    public HttpResponse<?> excluir(
            @PathVariable Long id,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        return dietaRepository.findById(id).map(dieta -> {
            // Verifica se quem está tentando excluir tem permissão sobre o dono da dieta
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, dieta.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não pode excluir esta dieta."));
            }

            dietaRepository.delete(dieta);
            return HttpResponse.ok(Map.of("message", "Dieta excluída com sucesso."));

        }).orElse(HttpResponse.notFound(Map.of("message", "Dieta não encontrada.")));
    }
}