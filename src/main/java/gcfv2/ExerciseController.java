package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.Optional;

@Controller("/api/exercises")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173" })
public class ExerciseController {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ExerciseController.class);

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private UsuarioExercicioRepository usuarioExercicioRepository;

    @Inject
    private ExerciseRepository exerciseRepository;

    /**
     * ATRIBUIR EXERCÍCIO A UM USUÁRIO
     * Endpoint: POST /api/exercises/assign
     * Query Params: requesterId, requesterRole
     * Body: { "userId": 123, "exerciseName": "Supino Reto" } OR { "userId": 123,
     * "exerciseId": "1" }
     */
    @Post("/assign")
    @Transactional
    public HttpResponse<?> assignExercise(
            @Body Map<String, Object> body,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        LOG.info("Recebendo requisição de atribuição. Body: {}", body);

        // 1. Validar Permissões (Apenas ADMIN ou PERSONAL)
        boolean isPrivileged = "ADMIN".equalsIgnoreCase(requesterRole) || "PERSONAL".equalsIgnoreCase(requesterRole);
        if (!isPrivileged) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Apenas Administradores e Personais podem atribuir exercícios."));
        }

        // 2. Extrair dados do corpo
        Object userIdObj = body.get("userId");
        Object exerciseIdObj = body.get("exerciseId");
        String exerciseNameRaw = (String) body.get("exerciseName");

        LOG.info("Parsed: userIdObj={}, exerciseIdObj={}, exerciseNameRaw={}", userIdObj, exerciseIdObj,
                exerciseNameRaw);

        if (userIdObj == null) {
            return HttpResponse.badRequest(Map.of("message", "userId é obrigatório."));
        }

        String resolvedExerciseName = exerciseNameRaw;

        // Resolução do Nome do Exercício
        if (exerciseIdObj != null) {
            String exId = exerciseIdObj.toString();
            Optional<Exercise> exOpt = exerciseRepository.findById(exId);
            if (exOpt.isPresent()) {
                resolvedExerciseName = exOpt.get().getName();
            } else {
                return HttpResponse.notFound(Map.of("message", "Exercício não encontrado com o ID fornecido: " + exId));
            }
        }

        if (resolvedExerciseName == null || resolvedExerciseName.isBlank()) {
            return HttpResponse
                    .badRequest(Map.of("message", "É necessário fornecer exerciseId ou exerciseName válido."));
        }

        Long userId;
        try {
            if (userIdObj instanceof Number) {
                userId = ((Number) userIdObj).longValue();
            } else {
                userId = Long.parseLong(userIdObj.toString());
            }
        } catch (NumberFormatException e) {
            return HttpResponse.badRequest(Map.of("message", "userId inválido."));
        }

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

        final String finalExerciseName = resolvedExerciseName;

        // 5. Verificar se já existe
        boolean exists = usuarioExercicioRepository.findByUsuario(usuario).stream()
                .anyMatch(ue -> ue.getExercicio().equalsIgnoreCase(finalExerciseName));

        if (exists) {
            return HttpResponse.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Este exercício já está atribuído ao usuário."));
        }

        // 6. Atribuir Exercício
        try {
            UsuarioExercicio novoExercicio = new UsuarioExercicio();
            novoExercicio.setUsuario(usuario);
            novoExercicio.setExercicio(finalExerciseName);

            usuarioExercicioRepository.save(novoExercicio);

            return HttpResponse.created(Map.of(
                    "message", "Exercício atribuído com sucesso.",
                    "exercise", finalExerciseName,
                    "userId", userId));

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao atribuir exercício: " + e.getMessage()));
        }
    }
}
