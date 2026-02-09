package gcfv2;

import gcfv2.dto.*;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Controller("/api/v2/workout-executions")
@CrossOrigin
public class WorkoutExecutionController {

    private final WorkoutExecutionRepository workoutExecutionRepository;
    private final ExerciseExecutionRepository exerciseExecutionRepository;
    private final StructuredWorkoutPlanRepository workoutPlanRepository;
    private final TreinoRepository treinoRepository; // CORRECTED: Use TreinoRepository
    private final UsuarioRepository usuarioRepository;
    private final PermissionService permissionService;
    private final NotificationService notificationService;
    private final gcfv2.gamification.GamificationService gamificationService;

    @Inject
    public WorkoutExecutionController(
            WorkoutExecutionRepository workoutExecutionRepository,
            ExerciseExecutionRepository exerciseExecutionRepository,
            StructuredWorkoutPlanRepository workoutPlanRepository,
            TreinoRepository treinoRepository, // CORRECTED
            UsuarioRepository usuarioRepository,
            PermissionService permissionService,
            NotificationService notificationService,
            gcfv2.gamification.GamificationService gamificationService) {
        this.workoutExecutionRepository = workoutExecutionRepository;
        this.exerciseExecutionRepository = exerciseExecutionRepository;
        this.workoutPlanRepository = workoutPlanRepository;
        this.treinoRepository = treinoRepository;
        this.usuarioRepository = usuarioRepository;
        this.permissionService = permissionService;
        this.notificationService = notificationService;
        this.gamificationService = gamificationService;
    }

    /**
     * POST /api/v2/workout-executions
     * Salvar a execução de um treino com as cargas utilizadas
     */
    @Post
    @Transactional
    public HttpResponse<?> saveWorkoutExecution(
            @Body WorkoutExecutionRequest request,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            // Validações básicas
            if (request.getUserId() == null || request.getWorkoutId() == null ||
                    request.getDayOfWeek() == null || request.getExecutedAt() == null) {
                return HttpResponse.badRequest(Map.of(
                        "error", "Campos obrigatórios faltando: userId, workoutId, dayOfWeek, executedAt"));
            }

            // Validação de permissões
            if (!permissionService.canSaveWorkoutExecution(requesterId, requesterRole, request.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Você não tem permissão para salvar execuções para este usuário"));
            }

            // Validação do usuário
            if (!usuarioRepository.existsById(request.getUserId())) {
                return HttpResponse.notFound(Map.of("error", "Usuário não encontrado"));
            }

            // Validação do treino (Dual-Check: V2 Plans OR Legacy Treinos)
            boolean existsV2 = workoutPlanRepository.existsById(request.getWorkoutId());
            Long effectiveWorkoutId = request.getWorkoutId();

            if (!existsV2) {
                // Try to find the legacy treino
                var legacyTreino = treinoRepository.findById(request.getWorkoutId());
                if (legacyTreino.isEmpty()) {
                    return HttpResponse.notFound(Map.of("error", "Treino não encontrado (nem V2 nem Legado)"));
                }

                // Auto-migrate: create a structured_workout_plans entry from the legacy treino
                Treino treino = legacyTreino.get();
                StructuredWorkoutPlan migratedPlan = new StructuredWorkoutPlan();
                migratedPlan.setUserId(request.getUserId());
                migratedPlan.setTitle(treino.getGoal() != null ? treino.getGoal() : "Treino Migrado");
                migratedPlan.setDaysData(treino.getDaysData() != null ? treino.getDaysData() : "{}");
                migratedPlan.setLegacyHtml(treino.getContent());
                StructuredWorkoutPlan savedPlan = workoutPlanRepository.save(migratedPlan);
                effectiveWorkoutId = savedPlan.getId();

                System.out.println("[WORKOUT_EXECUTION] Auto-migrated legacy treino " +
                    request.getWorkoutId() + " -> structured_workout_plans " + effectiveWorkoutId);
            }

            // Validação do dayOfWeek
            if (!permissionService.isValidDayOfWeek(request.getDayOfWeek())) {
                return HttpResponse.badRequest(Map.of(
                        "error",
                        "dayOfWeek inválido. Use: monday, tuesday, wednesday, thursday, friday, saturday, sunday"));
            }

            // Validação dos exercícios
            if (request.getExercises() == null || request.getExercises().isEmpty()) {
                return HttpResponse.badRequest(Map.of("error", "Lista de exercícios não pode estar vazia"));
            }

            // Criar WorkoutExecution
            WorkoutExecution execution = new WorkoutExecution();
            execution.setUserId(request.getUserId());
            execution.setWorkoutId(effectiveWorkoutId);
            execution.setDayOfWeek(permissionService.normalizeDayOfWeek(request.getDayOfWeek()));
            execution.setExecutedAt(request.getExecutedAt());
            execution.setComment(request.getComment());
            execution.setLiked(request.getLiked());
            System.out.println("[WORKOUT_EXECUTION] Saving execution. UserId: " + request.getUserId() + ", Liked: "
                    + request.getLiked());

            // Salvar execução
            WorkoutExecution savedExecution = workoutExecutionRepository.save(execution);

            // Criar ExerciseExecutions
            List<ExerciseExecution> exerciseExecutions = new ArrayList<>();
            for (ExerciseExecutionRequest exerciseReq : request.getExercises()) {
                // Validações do exercício
                if (exerciseReq.getExerciseName() == null || exerciseReq.getOrder() == null ||
                        exerciseReq.getSetsCompleted() == null) {
                    return HttpResponse.badRequest(Map.of(
                            "error", "Cada exercício deve ter: exerciseName, order, setsCompleted"));
                }

                ExerciseExecution exerciseExec = new ExerciseExecution();
                exerciseExec.setWorkoutExecution(savedExecution);
                exerciseExec.setExerciseName(exerciseReq.getExerciseName());
                exerciseExec.setExerciseOrder(exerciseReq.getOrder());
                exerciseExec.setSetsCompleted(exerciseReq.getSetsCompleted());
                exerciseExec.setActualLoad(exerciseReq.getActualLoad());
                exerciseExec.setNotes(exerciseReq.getNotes());

                ExerciseExecution savedExercise = exerciseExecutionRepository.save(exerciseExec);
                exerciseExecutions.add(savedExercise);
            }

            // Montar resposta
            savedExecution.setExercises(exerciseExecutions);

            // --- NOTIFICAÇÃO ---
            try {
                notificationService.createNotification(
                        request.getUserId(),
                        "WORKOUT_FINISHED",
                        "Treino finalizado com sucesso!");
            } catch (Exception e) {
                System.out.println("Erro ao criar notificação de treino finalizado: " + e.getMessage());
            }

            // --- GAMIFICATION ---
            try {
                gamificationService.checkAndUnlockAchievements(String.valueOf(request.getUserId()));
            } catch (Exception e) {
                System.out.println("Erro ao processar gamificação: " + e.getMessage());
            }

            return HttpResponse.status(HttpStatus.CREATED).body(savedExecution);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                    "error", "Erro ao salvar execução de treino: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v2/workout-executions/{userId}
     * Listar todas as execuções de treino de um usuário
     */
    @Get("/{userId}")
    public HttpResponse<?> listWorkoutExecutions(
            @PathVariable Long userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole,
            @Nullable @QueryValue Long workoutId,
            @Nullable @QueryValue Long startDate,
            @Nullable @QueryValue Long endDate,
            @Nullable @QueryValue Integer limit,
            @Nullable @QueryValue Integer offset) {
        try {
            // Validação de permissões
            if (!permissionService.canAccessUserData(requesterId, requesterRole, userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Você não tem permissão para acessar dados deste usuário"));
            }

            // Buscar execuções
            List<WorkoutExecution> executions;

            if (workoutId != null) {
                // Filtrar por workoutId
                executions = workoutExecutionRepository.findByUserIdAndWorkoutIdOrderByExecutedAtDesc(userId,
                        workoutId);
            } else if (startDate != null && endDate != null) {
                // Filtrar por período
                executions = workoutExecutionRepository.findByUserIdAndExecutedAtBetweenOrderByExecutedAtDesc(
                        userId, startDate, endDate);
            } else {
                // Todas as execuções
                executions = workoutExecutionRepository.findByUserIdOrderByExecutedAtDesc(userId);
            }

            // Aplicar paginação se necessário
            int totalRecords = executions.size();
            int limitValue = (limit != null && limit > 0) ? limit : 50;
            int offsetValue = (offset != null && offset >= 0) ? offset : 0;

            List<WorkoutExecution> paginatedExecutions = executions.stream()
                    .skip(offsetValue)
                    .limit(limitValue)
                    .toList();

            // Montar resposta com paginação
            Map<String, Object> response = new HashMap<>();
            response.put("executions", paginatedExecutions);
            response.put("pagination", Map.of(
                    "total", totalRecords,
                    "limit", limitValue,
                    "offset", offsetValue));

            return HttpResponse.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                    "error", "Erro ao listar execuções: " + e.getMessage()));
        }
    }

    /**
     * GET /api/v2/workout-executions/detail/{executionId}
     * Obter detalhes de uma execução específica
     */
    @Get("/detail/{executionId}")
    public HttpResponse<?> getWorkoutExecutionDetails(
            @PathVariable Long executionId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            var executionOpt = workoutExecutionRepository.findById(executionId);

            if (executionOpt.isEmpty()) {
                return HttpResponse.notFound(Map.of("error", "Execução não encontrada"));
            }

            WorkoutExecution execution = executionOpt.get();

            // Validação de permissões
            if (!permissionService.canAccessUserData(requesterId, requesterRole, execution.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Você não tem permissão para acessar esta execução"));
            }

            return HttpResponse.ok(execution);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                    "error", "Erro ao buscar execução: " + e.getMessage()));
        }
    }
}
