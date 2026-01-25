package gcfv2.gamification;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface UserAchievementRepository extends CrudRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserId(String userId);

    boolean existsByUserIdAndAchievementId(String userId, Long achievementId);
}
