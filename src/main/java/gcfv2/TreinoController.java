package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpMethod;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import java.util.Map;

@Controller("/api/treinos")
@CrossOrigin(allowedOrigins = { "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app" }, allowedMethods = {
                HttpMethod.GET,
                HttpMethod.POST,
                HttpMethod.DELETE,
                HttpMethod.OPTIONS
        })
public class TreinoController {

    @Inject
    private TreinoRepository treinoRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

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
            if (userOpt.isPresent()) {
                var user = userOpt.get();

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
                        || "ADMIN".equalsIgnoreCase(requesterRole);

                if (isPrivileged && !requesterId.equals(targetUserId)) {
                    var requesterOpt = usuarioRepository.findById(requesterId);
                    if (requesterOpt.isPresent()) {
                        planTypeToCheck = requesterOpt.get().getPlanType() != null ? requesterOpt.get().getPlanType()
                                : "FREE";
                    }
                }

                // FREE: bloqueado (apenas se não for privilegiado E não tiver créditos)
                Integer userCredits = user.getCredits() != null ? user.getCredits() : 0;
                if ("FREE".equalsIgnoreCase(planTypeToCheck) && !isPrivileged && userCredits <= 0) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message",
                                    "Plano gratuito não permite geração de treinos sem créditos. Compre créditos ou faça upgrade!"));
                }

                // PRO e STUDIO: sem limite de gerações
                // STARTER: limite de 10 (controlado pelo consume-credit)
                // A verificação do limite agora é feita pelo consume-credit no frontend
            }

            Treino salvo = treinoRepository.save(treino);

            // Incrementar contador de gerações
            usuarioRepository.incrementGenerationsUsedCycle(Long.parseLong(treino.getUserId()));

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
}