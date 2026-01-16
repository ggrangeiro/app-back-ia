package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface TreinoRepository extends CrudRepository<Treino, Long> {
    List<Treino> findByUserIdOrderByCreatedAtDesc(String userId);

    void deleteByUserId(String userId);
}