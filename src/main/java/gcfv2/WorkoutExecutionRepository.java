package gcfv2;

import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface WorkoutExecutionRepository extends CrudRepository<WorkoutExecution, Long> {

    /**
     * Lista todas as execuções de um usuário ordenadas por data de execução (mais recente primeiro)
     * Inclui os exercícios relacionados
     */
    @Join(value = "exercises", type = Join.Type.LEFT_FETCH)
    List<WorkoutExecution> findByUserIdOrderByExecutedAtDesc(Long userId);

    /**
     * Lista execuções de um usuário para um treino específico
     */
    @Join(value = "exercises", type = Join.Type.LEFT_FETCH)
    List<WorkoutExecution> findByUserIdAndWorkoutIdOrderByExecutedAtDesc(Long userId, Long workoutId);

    /**
     * Lista execuções de um usuário em um período específico
     */
    @Join(value = "exercises", type = Join.Type.LEFT_FETCH)
    List<WorkoutExecution> findByUserIdAndExecutedAtBetweenOrderByExecutedAtDesc(
        Long userId,
        Long startDate,
        Long endDate
    );

    /**
     * Busca uma execução específica com seus exercícios
     */
    @Join(value = "exercises", type = Join.Type.LEFT_FETCH)
    Optional<WorkoutExecution> findById(Long id);

    /**
     * Conta quantas execuções um usuário tem
     */
    Long countByUserId(Long userId);
}
