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

/**
 * Controller for Structured Training Plans (V2 API)
 * 
 * NEW endpoints at /api/v2/treinos/ - does NOT affect existing /api/treinos/
 * routes.
 * 
 * This controller handles JSON-structured training data instead of HTML
 * content.
 */
@Controller("/api/v2/treinos")
@CrossOrigin(allowedOrigins = {
        "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app"
}, allowedMethods = {
        HttpMethod.GET,
        HttpMethod.POST,
        HttpMethod.DELETE,
        HttpMethod.OPTIONS
})
public class StructuredTreinoController {

    @Inject
    private StructuredTreinoRepository structuredTreinoRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private EmailService emailService;

    /**
     * CREATE - Save a new structured training plan
     * 
     * POST /api/v2/treinos/
     * 
     * Request Body: StructuredTreino JSON
     * Query Params: requesterId, requesterRole
     */
    @Post("/")
    @Transactional
    public HttpResponse<?> salvar(@Body StructuredTreino treino,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            // Permission check
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, treino.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não tem vínculo com este aluno."));
            }

            // Plan type check (same logic as V1)
            Long targetUserId = Long.parseLong(treino.getUserId());
            var userOpt = usuarioRepository.findById(targetUserId);
            if (userOpt.isPresent()) {
                var user = userOpt.get();

                // Check Access Level (Leitura vs Escrita)
                if ("USER".equalsIgnoreCase(requesterRole) && "READONLY".equalsIgnoreCase(user.getAccessLevel())) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message",
                                    "Seu plano atual não permite gerar treinos. Solicite ao seu Personal."));
                }

                String planTypeToCheck = user.getPlanType() != null ? user.getPlanType() : "FREE";
                boolean isPrivileged = "PERSONAL".equalsIgnoreCase(requesterRole)
                        || "ADMIN".equalsIgnoreCase(requesterRole);

                if (isPrivileged && !requesterId.equals(targetUserId)) {
                    var requesterOpt = usuarioRepository.findById(requesterId);
                    if (requesterOpt.isPresent()) {
                        planTypeToCheck = requesterOpt.get().getPlanType() != null
                                ? requesterOpt.get().getPlanType()
                                : "FREE";
                    }
                }

                Integer userCredits = user.getCredits() != null ? user.getCredits() : 0;
                if ("FREE".equalsIgnoreCase(planTypeToCheck) && !isPrivileged && userCredits <= 0) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message",
                                    "Plano gratuito não permite geração de treinos estruturados sem créditos. Compre créditos ou faça upgrade!"));
                }
            }

            // Save structured training
            StructuredTreino salvo = structuredTreinoRepository.save(treino);

            // Increment generation counter
            usuarioRepository.incrementGenerationsUsedCycle(Long.parseLong(treino.getUserId()));

            // Enviar e-mail de notificação
            try {
                var userOptEmail = usuarioRepository.findById(Long.parseLong(treino.getUserId()));
                if (userOptEmail.isPresent()) {
                    var user = userOptEmail.get();
                    String subject = (treino.getGoal() != null && !treino.getGoal().isEmpty()) ? treino.getGoal()
                            : "Treino Estruturado";
                    emailService.sendWorkoutGeneratedEmail(user.getEmail(), user.getNome(), subject);
                }
            } catch (Exception e) {
                System.err.println("Erro ao enviar email de treino estruturado: " + e.getMessage());
            }

            return HttpResponse.created(salvo);
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao salvar treino estruturado: " + e.getMessage()));
        }
    }

    /**
     * READ - List all structured trainings for a user
     * 
     * GET /api/v2/treinos/{userId}
     * 
     * Query Params: requesterId, requesterRole
     */
    @Get("/{userId}")
    public HttpResponse<?> listar(@PathVariable String userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }

        List<StructuredTreino> treinos = structuredTreinoRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return HttpResponse.ok(treinos);
    }

    /**
     * DELETE - Remove a structured training
     * 
     * DELETE /api/v2/treinos/{id}
     * 
     * Query Params: requesterId, requesterRole
     */
    @Delete("/{id}")
    @Transactional
    public HttpResponse<?> excluir(
            @PathVariable Long id,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        return structuredTreinoRepository.findById(id).map(treino -> {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, treino.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não pode excluir este treino."));
            }

            structuredTreinoRepository.delete(treino);
            return HttpResponse.ok(Map.of("message", "Treino estruturado excluído com sucesso."));

        }).orElse(HttpResponse.notFound(Map.of("message", "Treino estruturado não encontrado.")));
    }
}
