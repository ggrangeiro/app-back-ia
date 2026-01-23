package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

/**
 * Repository for Structured Diet Plans (V2 API)
 * Separate from DietaRepository - does NOT affect existing flow.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface StructuredDietaRepository extends CrudRepository<StructuredDieta, Long> {

    List<StructuredDieta> findByUserIdOrderByCreatedAtDesc(String userId);

    List<StructuredDieta> findByUserId(String userId);

    long countByUserId(String userId);

    void deleteByUserId(String userId);
}
