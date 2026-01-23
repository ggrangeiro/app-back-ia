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

    /**
     * Busca a última carga utilizada para cada exercício de um usuário
     * Retorna apenas a execução mais recente de cada exercício
     */
    @Query("""
        SELECT ee.exercise_name, ee.actual_load, we.executed_at
        FROM exercise_executions ee
        INNER JOIN workout_executions we ON ee.workout_execution_id = we.id
        INNER JOIN (
            SELECT ee2.exercise_name, MAX(we2.executed_at) as max_executed_at
            FROM exercise_executions ee2
            INNER JOIN workout_executions we2 ON ee2.workout_execution_id = we2.id
            WHERE we2.user_id = :userId
            GROUP BY ee2.exercise_name
        ) latest ON ee.exercise_name = latest.exercise_name AND we.executed_at = latest.max_executed_at
        WHERE we.user_id = :userId
    """)
    List<Object[]> findLastUsedLoadsByUserId(Long userId);
}
