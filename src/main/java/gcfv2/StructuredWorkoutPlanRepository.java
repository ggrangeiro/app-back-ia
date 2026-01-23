package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface StructuredWorkoutPlanRepository extends CrudRepository<StructuredWorkoutPlan, Long> {

    /**
     * Busca todos os treinos de um usuário que não foram deletados
     */
    List<StructuredWorkoutPlan> findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    /**
     * Busca um treino específico por ID que não foi deletado
     */
    Optional<StructuredWorkoutPlan> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Busca um treino de um usuário específico
     */
    Optional<StructuredWorkoutPlan> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);
}
