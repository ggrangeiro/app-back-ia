package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

/**
 * Repository for Structured Training Plans (V2 API)
 * Separate from TreinoRepository - does NOT affect existing flow.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface StructuredTreinoRepository extends CrudRepository<StructuredTreino, Long> {

    List<StructuredTreino> findByUserIdOrderByCreatedAtDesc(String userId);

    List<StructuredTreino> findByUserId(String userId);

    long countByUserId(String userId);

    List<StructuredTreino> findByUserIdAndLevel(String userId, String level);

    void deleteByUserId(String userId);
}
