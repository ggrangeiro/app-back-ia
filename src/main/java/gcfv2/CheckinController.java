package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpMethod;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.List;

@Controller("/api/checkins")
@CrossOrigin(allowedOrigins = { "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app" }, allowedMethods = {
                HttpMethod.GET,
                HttpMethod.POST,
                HttpMethod.OPTIONS
        })
public class CheckinController {

    @Inject
    private CheckinRepository checkinRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private TreinoRepository treinoRepository;

    @Post("/")
    public HttpResponse<?> salvar(@Body Checkin checkin,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            // Regra 1: Validação de Permissões
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, checkin.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message",
                                "Acesso negado. Sem permissão para registrar check-in para este aluno."));
            }

            // Regra 2: Validar trainingId
            if (checkin.getTrainingId() == null) {
                return HttpResponse.badRequest(Map.of("message", "O trainingId é obrigatório."));
            }

            return treinoRepository.findById(checkin.getTrainingId()).map(treino -> {
                // Valida se o treino pertence ao usuário
                if (!treino.getUserId().equals(checkin.getUserId())) {
                    return HttpResponse.badRequest(Map.of("message", "O treino informado não pertence ao aluno."));
                }

                // Garante status completed se não enviado
                if (checkin.getStatus() == null || checkin.getStatus().isEmpty()) {
                    checkin.setStatus("completed");
                }

                // Garante timestamp se não enviado
                if (checkin.getTimestamp() == null || checkin.getTimestamp() == 0) {
                    checkin.setTimestamp(System.currentTimeMillis());
                }

                Checkin salvo = checkinRepository.save(checkin);
                return HttpResponse.created(salvo);

            }).orElse(HttpResponse.badRequest(Map.of("message", "Treino (trainingId) não encontrado.")));

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao salvar check-in: " + e.getMessage()));
        }
    }

    @Get("/{userId}")
    public HttpResponse<?> listar(@PathVariable String userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
            }

            List<Checkin> checkins = checkinRepository.findByUserId(userId);
            return HttpResponse.ok(checkins);

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao listar check-ins: " + e.getMessage()));
        }
    }
}
