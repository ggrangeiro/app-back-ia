package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;

@Controller("/api/exercises")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app", "http://localhost:3000",
        "http://localhost:5173" })
public class ExerciseController {

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private UsuarioExercicioRepository usuarioExercicioRepository;

    /**
     * ATRIBUIR EXERCÍCIO A UM USUÁRIO
     * Endpoint: POST /api/exercises/assign
     * Query Params: requesterId, requesterRole
     * Body: { "userId": 123, "exerciseName": "Supino Reto" }
     */
    @Post("/assign")
    @Transactional
    public HttpResponse<?> assignExercise(
            @Body Map<String, Object> body,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        // 1. Validar Permissões (Apenas ADMIN ou PERSONAL)
        boolean isPrivileged = "ADMIN".equalsIgnoreCase(requesterRole) || "PERSONAL".equalsIgnoreCase(requesterRole);
        if (!isPrivileged) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Apenas Administradores e Personais podem atribuir exercícios."));
        }

        // 2. Extrair dados do corpo
        Number userIdNum = (Number) body.get("userId");
        String exerciseName = (String) body.get("exerciseName");

        if (userIdNum == null || exerciseName == null || exerciseName.isBlank()) {
            return HttpResponse.badRequest(Map.of("message", "userId e exerciseName são obrigatórios."));
        }
        Long userId = userIdNum.longValue();

        // 3. Se for PERSONAL, verificar se o aluno pertence a ele
        if ("PERSONAL".equalsIgnoreCase(requesterRole)) {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Você não tem permissão para gerenciar este aluno."));
            }
        }

        // 4. Buscar Usuário
        Optional<Usuario> userOpt = usuarioRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return HttpResponse.notFound(Map.of("message", "Usuário não encontrado."));
        }
        Usuario usuario = userOpt.get();

        // 5. Verificar se já existe
        boolean exists = usuarioExercicioRepository.findByUsuario(usuario).stream()
                .anyMatch(ue -> ue.getExercicio().equalsIgnoreCase(exerciseName));

        if (exists) {
            return HttpResponse.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Este exercício já está atribuído ao usuário."));
        }

        // 6. Atribuir Exercício
        try {
            UsuarioExercicio novoExercicio = new UsuarioExercicio();
            novoExercicio.setUsuario(usuario);
            novoExercicio.setExercicio(exerciseName);

            usuarioExercicioRepository.save(novoExercicio);

            return HttpResponse.created(Map.of(
                    "message", "Exercício atribuído com sucesso.",
                    "exercise", exerciseName,
                    "userId", userId));

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao atribuir exercício: " + e.getMessage()));
        }
    }
}
