package gcfv2.gamification;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface AchievementRepository extends CrudRepository<Achievement, Long> {
    List<Achievement> findByActive(boolean active);
}
