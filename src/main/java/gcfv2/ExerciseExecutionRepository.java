package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.annotation.Query;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface ExerciseExecutionRepository extends CrudRepository<ExerciseExecution, Long> {

    /**
     * Busca todas as execuções de um exercício específico para um usuário
     * Ordenado por data de execução (mais recente primeiro)
     */
    @Query("""
        SELECT ee.*
        FROM exercise_executions ee
        INNER JOIN workout_executions we ON ee.workout_execution_id = we.id
        WHERE we.user_id = :userId
        AND ee.exercise_name = :exerciseName
        ORDER BY we.executed_at DESC
        LIMIT :limit
    """)
    List<ExerciseExecution> findByUserIdAndExerciseName(Long userId, String exerciseName, int limit);

    /**
     * Busca o histórico completo de um exercício com informações da execução do treino
     */
    @Query("""
        SELECT ee.id, ee.exercise_name, ee.exercise_order, ee.sets_completed, ee.actual_load, ee.notes,
               we.id as workout_execution_id, we.executed_at, we.day_of_week
        FROM exercise_executions ee
        INNER JOIN workout_executions we ON ee.workout_execution_id = we.id
        WHERE we.user_id = :userId
        AND ee.exercise_name = :exerciseName
        ORDER BY we.executed_at DESC
        LIMIT :limit
    """)
    List<Object[]> findLoadHistoryByUserIdAndExerciseName(Long userId, String exerciseName, int limit);
}
