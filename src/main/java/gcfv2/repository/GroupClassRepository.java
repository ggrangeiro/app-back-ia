package gcfv2.repository;

import gcfv2.model.GroupClass;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface GroupClassRepository extends CrudRepository<GroupClass, Long> {

    List<GroupClass> findByProfessorId(Long professorId);

    // Find available classes in the future
    List<GroupClass> findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime time);
}
