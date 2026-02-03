package gcfv2.evo;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository para gerenciar mapeamentos de exercícios EVO.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface EvoExerciseMappingRepository extends CrudRepository<EvoExerciseMapping, Long> {

    /**
     * Busca mapeamento pelo nome do exercício FitAI (normalizado)
     */
    Optional<EvoExerciseMapping> findByUserIdAndFitaiExerciseName(Long userId, String fitaiExerciseName);

    /**
     * Lista todos os mapeamentos de um usuário
     */
    List<EvoExerciseMapping> findByUserId(Long userId);

    /**
     * Lista mapeamentos por grupo muscular
     */
    List<EvoExerciseMapping> findByUserIdAndMuscleGroup(Long userId, String muscleGroup);

    /**
     * Conta mapeamentos de um usuário
     */
    long countByUserId(Long userId);

    /**
     * Busca mapeamentos não verificados
     */
    List<EvoExerciseMapping> findByUserIdAndIsVerified(Long userId, Boolean isVerified);

    /**
     * Busca por nome de exercício similar (para sugestões)
     */
    @Query("SELECT * FROM evo_exercise_mapping WHERE user_id = :userId AND fitai_exercise_name LIKE :pattern LIMIT 10")
    List<EvoExerciseMapping> findSimilarMappings(Long userId, String pattern);

    /**
     * Deleta todos os mapeamentos de um usuário
     */
    void deleteByUserId(Long userId);
}
