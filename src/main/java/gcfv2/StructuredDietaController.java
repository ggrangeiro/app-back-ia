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
 * Controller for Structured Diet Plans (V2 API)
 * 
 * NEW endpoints at /api/v2/dietas/ - does NOT affect existing /api/dietas/
 * routes.
 * 
 * This controller handles JSON-structured diet data instead of HTML content.
 */
@Controller("/api/v2/dietas")
@CrossOrigin(allowedOrigins = {
        "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app"
}, allowedMethods = {
        HttpMethod.GET,
        HttpMethod.POST,
        HttpMethod.DELETE,
        HttpMethod.OPTIONS
})
public class StructuredDietaController {

    @Inject
    private StructuredDietaRepository structuredDietaRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private EmailService emailService;

    /**
     * CREATE - Save a new structured diet plan
     * 
     * POST /api/v2/dietas/
     * 
     * Request Body: StructuredDieta JSON
     * Query Params: requesterId, requesterRole
     */
    @Post("/")
    @Transactional
    public HttpResponse<?> salvar(@Body StructuredDieta dieta,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            // Permission check
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, dieta.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não tem vínculo com este aluno."));
            }

            // Plan type check (same logic as V1)
            Long targetUserId = Long.parseLong(dieta.getUserId());
            var userOpt = usuarioRepository.findById(targetUserId);
            if (userOpt.isPresent()) {
                var user = userOpt.get();

                // Check Access Level (Leitura vs Escrita)
                if ("USER".equalsIgnoreCase(requesterRole) && "READONLY".equalsIgnoreCase(user.getAccessLevel())) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message",
                                    "Seu plano atual não permite gerar dietas estruturadas. Solicite ao seu Personal."));
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

                if ("FREE".equalsIgnoreCase(planTypeToCheck) && !isPrivileged) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message",
                                    "Plano gratuito não permite geração de dietas estruturadas. Faça upgrade!"));
                }
            }

            // Save structured diet
            StructuredDieta salva = structuredDietaRepository.save(dieta);

            // Increment generation counter
            usuarioRepository.incrementGenerationsUsedCycle(Long.parseLong(dieta.getUserId()));

            // Enviar e-mail de notificação
            try {
                var userOptEmail = usuarioRepository.findById(Long.parseLong(dieta.getUserId()));
                if (userOptEmail.isPresent()) {
                    var user = userOptEmail.get();
                    String goal = (dieta.getGoal() != null && !dieta.getGoal().isEmpty()) ? dieta.getGoal()
                            : "Dieta Estruturada";
                    emailService.sendDietGeneratedEmail(user.getEmail(), user.getNome(), goal);
                }
            } catch (Exception e) {
                System.err.println("Erro ao enviar email de dieta estruturada: " + e.getMessage());
            }

            return HttpResponse.created(salva);
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao salvar dieta estruturada: " + e.getMessage()));
        }
    }

    /**
     * READ - List all structured diets for a user
     * 
     * GET /api/v2/dietas/{userId}
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

        List<StructuredDieta> dietas = structuredDietaRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return HttpResponse.ok(dietas);
    }

    /**
     * DELETE - Remove a structured diet
     * 
     * DELETE /api/v2/dietas/{id}
     * 
     * Query Params: requesterId, requesterRole
     */
    @Delete("/{id}")
    @Transactional
    public HttpResponse<?> excluir(
            @PathVariable Long id,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        return structuredDietaRepository.findById(id).map(dieta -> {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, dieta.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não pode excluir esta dieta."));
            }

            structuredDietaRepository.delete(dieta);
            return HttpResponse.ok(Map.of("message", "Dieta estruturada excluída com sucesso."));

        }).orElse(HttpResponse.notFound(Map.of("message", "Dieta estruturada não encontrada.")));
    }
}
