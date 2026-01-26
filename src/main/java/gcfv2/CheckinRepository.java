package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface CheckinRepository extends CrudRepository<Checkin, Long> {
    List<Checkin> findByUserId(String userId);

    List<Checkin> findByUserIdOrderByTimestampDesc(String userId);

    List<Checkin> findByUserIdAndTimestampBetween(String userId, Long start, Long end);

    // [NEW] Methods for Insights
    List<Checkin> findByUserIdIn(List<String> userIds);

    List<Checkin> findByUserIdInAndTimestampBetween(List<String> userIds, Long start, Long end);

    void deleteByUserId(String userId);

    // [NEW] Find checkins with valid location
    List<Checkin> findByUserIdAndLatitudeIsNotNullAndLongitudeIsNotNull(String userId);
}
