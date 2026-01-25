package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import java.util.Map;

@Controller("/api/treinos")
@CrossOrigin
public class TreinoController {

    @Inject
    private TreinoRepository treinoRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private EmailService emailService;

    @Inject
    private ActivityLogService activityLogService;

    @Post("/")
    @Transactional
    public HttpResponse<?> salvar(@Body Treino treino,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, treino.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Sem permissão para este aluno."));
            }

            // GATEKEEPER: Verificar limites de geração do plano
            Long targetUserId = Long.parseLong(treino.getUserId());
            var userOpt = usuarioRepository.findById(targetUserId);
            String targetUserName = null;
            if (userOpt.isPresent()) {
                var user = userOpt.get();
                targetUserName = user.getNome();

                // Check Access Level (Leitura vs Escrita)
                if ("USER".equalsIgnoreCase(requesterRole) && "READONLY".equalsIgnoreCase(user.getAccessLevel())) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message",
                                    "Seu plano atual não permite gerar treinos. Solicite ao seu Personal."));
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

            Treino salvo = treinoRepository.save(treino);

            // Incrementar contador de gerações
            usuarioRepository.incrementGenerationsUsedCycle(Long.parseLong(treino.getUserId()));

            // Log de atividade para professor
            activityLogService.logActivity(
                    requesterId,
                    requesterRole,
                    "WORKOUT_GENERATED",
                    targetUserId,
                    targetUserName,
                    "TRAINING",
                    salvo.getId());

            // Enviar e-mail de notificação APENAS se não foi o próprio aluno que criou
            if (!requesterId.equals(Long.parseLong(treino.getUserId()))) {
                try {
                    var userOptEmail = usuarioRepository.findById(Long.parseLong(treino.getUserId()));
                    if (userOptEmail.isPresent()) {
                        var user = userOptEmail.get();
                        String subject = (treino.getGoal() != null && !treino.getGoal().isEmpty()) ? treino.getGoal()
                                : "Treino Personalizado";
                        emailService.sendWorkoutGeneratedEmail(user.getEmail(), user.getNome(), subject);
                    }
                } catch (Exception e) {
                    // Log and continue, don't fail the request because email failed
                    System.err.println("Erro ao enviar email de treino: " + e.getMessage());
                }
            }

            return HttpResponse.created(salvo);
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao salvar treino: " + e.getMessage()));
        }
    }

    @Get("/{userId}")
    public HttpResponse<?> listar(@PathVariable String userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }
        return HttpResponse.ok(treinoRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Delete("/{id}")
    @Transactional
    public HttpResponse<?> excluir(
            @PathVariable Long id,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        return treinoRepository.findById(id).map(treino -> {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, treino.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não pode excluir este treino."));
            }

            treinoRepository.delete(treino);
            return HttpResponse.ok(Map.of("message", "Treino excluído com sucesso."));

        }).orElse(HttpResponse.notFound(Map.of("message", "Treino não encontrado.")));
    }

    @Put("/{id}")
    @Transactional
    public HttpResponse<?> atualizar(
            @PathVariable Long id,
            @Body Treino atualizacao,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        return treinoRepository.findById(id).map(treino -> {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, treino.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado."));
            }

            if (atualizacao.getDaysData() != null) {
                treino.setDaysData(atualizacao.getDaysData());
            }
            if (atualizacao.getContent() != null) {
                treino.setContent(atualizacao.getContent());
            }
            if (atualizacao.getFormData() != null) {
                treino.setFormData(atualizacao.getFormData());
            }

            treinoRepository.update(treino);
            return HttpResponse.ok(treino);

        }).orElse(HttpResponse.notFound(Map.of("message", "Treino não encontrado.")));
    }

}
