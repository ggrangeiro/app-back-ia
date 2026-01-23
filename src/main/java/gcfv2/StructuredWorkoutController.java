package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Controller para gerenciar treinos estruturados (V2)
 * Permite criar, listar, buscar e deletar treinos com estrutura JSON
 */
@Controller("/api/v2/treinos")
@CrossOrigin("https://fitai-analyzer-732767853162.us-west1.run.app")
public class StructuredWorkoutController {

    private final StructuredWorkoutPlanRepository workoutPlanRepository;
    private final UsuarioRepository usuarioRepository;
    private final PermissionService permissionService;

    @Inject
    public StructuredWorkoutController(
        StructuredWorkoutPlanRepository workoutPlanRepository,
        UsuarioRepository usuarioRepository,
        PermissionService permissionService
    ) {
        this.workoutPlanRepository = workoutPlanRepository;
        this.usuarioRepository = usuarioRepository;
        this.permissionService = permissionService;
    }

    /**
     * POST /api/v2/treinos
     * Criar novo treino estruturado (V2)
     */
    @Post
    @Transactional
    public HttpResponse<?> createWorkout(
        @Body StructuredWorkoutPlan workout,
        @QueryValue Long requesterId,
        @QueryValue String requesterRole
    ) {
        try {
            // Validações básicas
            if (workout.getUserId() == null || workout.getTitle() == null ||
                workout.getDaysData() == null || workout.getDaysData().isEmpty()) {
                return HttpResponse.badRequest(Map.of(
                    "error", "Campos obrigatórios faltando: userId, title, daysData"
                ));
            }

            // Validação de permissões
            if (!permissionService.canAccessUserData(requesterId, requesterRole, workout.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para criar treino para este usuário"));
            }

            // Validação do usuário
            if (!usuarioRepository.existsById(workout.getUserId())) {
                return HttpResponse.notFound(Map.of("error", "Usuário não encontrado"));
            }

            // Salvar treino
            StructuredWorkoutPlan savedWorkout = workoutPlanRepository.save(workout);

            return HttpResponse.status(HttpStatus.CREATED).body(savedWorkout);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                "error", "Erro ao criar treino: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/v2/treinos/{userId}
     * Listar todos os treinos de um usuário
     */
    @Get("/{userId}")
    public HttpResponse<?> listWorkouts(
        @PathVariable Long userId,
        @QueryValue Long requesterId,
        @QueryValue String requesterRole
    ) {
        try {
            // Validação de permissões
            if (!permissionService.canAccessUserData(requesterId, requesterRole, userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para acessar treinos deste usuário"));
            }

            List<StructuredWorkoutPlan> workouts = workoutPlanRepository
                .findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId);

            return HttpResponse.ok(workouts);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                "error", "Erro ao listar treinos: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/v2/treinos/detail/{workoutId}
     * Buscar detalhes de um treino específico
     */
    @Get("/detail/{workoutId}")
    public HttpResponse<?> getWorkoutDetails(
        @PathVariable Long workoutId,
        @QueryValue Long requesterId,
        @QueryValue String requesterRole
    ) {
        try {
            var workoutOpt = workoutPlanRepository.findByIdAndDeletedAtIsNull(workoutId);

            if (workoutOpt.isEmpty()) {
                return HttpResponse.notFound(Map.of("error", "Treino não encontrado"));
            }

            StructuredWorkoutPlan workout = workoutOpt.get();

            // Validação de permissões
            if (!permissionService.canAccessUserData(requesterId, requesterRole, workout.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para acessar este treino"));
            }

            return HttpResponse.ok(workout);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                "error", "Erro ao buscar treino: " + e.getMessage()
            ));
        }
    }

    /**
     * PUT /api/v2/treinos/{workoutId}
     * Atualizar um treino existente
     */
    @Put("/{workoutId}")
    @Transactional
    public HttpResponse<?> updateWorkout(
        @PathVariable Long workoutId,
        @Body StructuredWorkoutPlan updatedWorkout,
        @QueryValue Long requesterId,
        @QueryValue String requesterRole
    ) {
        try {
            var workoutOpt = workoutPlanRepository.findByIdAndDeletedAtIsNull(workoutId);

            if (workoutOpt.isEmpty()) {
                return HttpResponse.notFound(Map.of("error", "Treino não encontrado"));
            }

            StructuredWorkoutPlan existingWorkout = workoutOpt.get();

            // Validação de permissões
            if (!permissionService.canAccessUserData(requesterId, requesterRole, existingWorkout.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para atualizar este treino"));
            }

            // Atualizar campos
            if (updatedWorkout.getTitle() != null) {
                existingWorkout.setTitle(updatedWorkout.getTitle());
            }
            if (updatedWorkout.getDaysData() != null) {
                existingWorkout.setDaysData(updatedWorkout.getDaysData());
            }
            if (updatedWorkout.getLegacyHtml() != null) {
                existingWorkout.setLegacyHtml(updatedWorkout.getLegacyHtml());
            }

            StructuredWorkoutPlan savedWorkout = workoutPlanRepository.update(existingWorkout);

            return HttpResponse.ok(savedWorkout);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                "error", "Erro ao atualizar treino: " + e.getMessage()
            ));
        }
    }

    /**
     * DELETE /api/v2/treinos/{workoutId}
     * Soft delete de um treino (marca como deletado)
     */
    @Delete("/{workoutId}")
    @Transactional
    public HttpResponse<?> deleteWorkout(
        @PathVariable Long workoutId,
        @QueryValue Long requesterId,
        @QueryValue String requesterRole
    ) {
        try {
            var workoutOpt = workoutPlanRepository.findByIdAndDeletedAtIsNull(workoutId);

            if (workoutOpt.isEmpty()) {
                return HttpResponse.notFound(Map.of("error", "Treino não encontrado"));
            }

            StructuredWorkoutPlan workout = workoutOpt.get();

            // Validação de permissões
            if (!permissionService.canAccessUserData(requesterId, requesterRole, workout.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para deletar este treino"));
            }

            // Soft delete
            workout.setDeletedAt(Instant.now());
            workoutPlanRepository.update(workout);

            return HttpResponse.ok(Map.of("message", "Treino deletado com sucesso"));

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                "error", "Erro ao deletar treino: " + e.getMessage()
            ));
        }
    }
}
