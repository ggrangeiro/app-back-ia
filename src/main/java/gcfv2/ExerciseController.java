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
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app" })
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

    /**
     * CRIAR NOVO EXERCÍCIO (CUSTOMIZADO)
     * Endpoint: POST /api/exercises/create
     * Permite que Professores/Admin criem novos exercícios.
     * Se videoUrl for fornecido, já vincula como custom video do professor.
     */
    @Inject
    private ProfessorVideoService professorVideoService;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private UsuarioExercicioRepository usuarioExercicioRepository;

    @Post("/create")
    @Transactional
    public HttpResponse<?> createExercise(@Body Map<String, Object> body,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        // 1. Validar Permissões (Professor, Personal ou Admin)
        boolean canCreate = "ADMIN".equalsIgnoreCase(requesterRole) ||
                "PERSONAL".equalsIgnoreCase(requesterRole) ||
                "PROFESSOR".equalsIgnoreCase(requesterRole);

        if (!canCreate) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Apenas Professores e Administradores podem criar exercícios."));
        }

        String name = (String) body.get("name");
        String videoUrl = (String) body.get("videoUrl");
        String description = (String) body.get("description");

        if (name == null || name.isBlank()) {
            return HttpResponse.badRequest(Map.of("message", "Nome do exercício é obrigatório."));
        }

        // 2. Normalizar ID (UpperCase + Underscore)
        String exerciseId = name.trim().toUpperCase().replaceAll("\\s+", "_");

        // 3. Verificar se já existe (Evitar duplicatas globais com mesmo ID)
        if (exerciseRepository.existsById(exerciseId)) {
            // Se já existe, mas o usuário quer "criar", podemos apenas vincular o vídeo se
            // for professor
            // Ou retornar erro. Vamos assumir que se existe, retornamos o existente e
            // tentamos vincular o vídeo.
            LOG.info("Exercício já existe: {}", exerciseId);
        } else {
            // Criar Novo
            Exercise newExercise = new Exercise();
            newExercise.setId(exerciseId);
            newExercise.setName(name);
            newExercise.setCategory("CUSTOM"); // Categoria padrão
            newExercise.setActive(true);
            newExercise.setDescription("Criado por " + requesterRole + " " + requesterId);

            exerciseRepository.save(newExercise);
        }

        // 4. Se houver Video URL e for Professor/Personal, salvar o vídeo customizado
        if (videoUrl != null && !videoUrl.isBlank()) {
            try {
                // Se o serviço espera ID numérico para professor, usamos requesterId
                professorVideoService.saveOrUpdateVideo(requesterId, exerciseId, videoUrl, description);
            } catch (Exception e) {
                LOG.error("Erro ao salvar vídeo para o novo exercício: ", e);
                // Não falha o request todo, apenas loga
            }
        }

        // [FIX] Vincular o exercício ao usuário criador (Personal ou Professor)
        // para que apareça na lista dele (UsuarioExercicio)
        Optional<Usuario> userOpt = usuarioRepository.findById(requesterId);
        if (userOpt.isPresent()) {
            // Verificar se já não existe o vinculo (embora seja novo, vai que...)
            boolean jaVinculado = usuarioExercicioRepository.findByUsuario(userOpt.get())
                    .stream()
                    .anyMatch(ue -> ue.getExercicio().equalsIgnoreCase(exerciseId));

            if (!jaVinculado) {
                UsuarioExercicio ue = new UsuarioExercicio();
                ue.setExercicio(exerciseId); // USAR O ID (NORMALIZED) "SUPINO"
                ue.setUsuario(userOpt.get());
                usuarioExercicioRepository.save(ue);
            }
        }

        return HttpResponse.created(Map.of(
                "message", "Exercício criado/atualizado com sucesso.",
                "id", exerciseId,
                "name", name));
    }
}
