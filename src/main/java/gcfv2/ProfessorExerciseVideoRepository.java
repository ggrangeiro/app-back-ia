package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface ProfessorExerciseVideoRepository extends CrudRepository<ProfessorExerciseVideo, Long> {

    Optional<ProfessorExerciseVideo> findByProfessorIdAndExerciseId(Long professorId, String exerciseId);

    List<ProfessorExerciseVideo> findByProfessorId(Long professorId);
}
