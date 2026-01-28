package gcfv2.repository;

import gcfv2.model.ClassBooking;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface ClassBookingRepository extends CrudRepository<ClassBooking, Long> {

    List<ClassBooking> findByClassId(Long classId);

    List<ClassBooking> findByStudentId(Long studentId);

    Optional<ClassBooking> findByClassIdAndStudentId(Long classId, Long studentId);

    long countByClassIdAndStatus(Long classId, String status);

    void deleteByClassId(Long classId);
}
