package gcfv2;

import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface UsuarioRepository extends CrudRepository<Usuario, Long> {

    @Join(value = "assignedExercisesList", type = Join.Type.LEFT_FETCH)
    List<Usuario> listAll();

    @Join(value = "assignedExercisesList", type = Join.Type.LEFT_FETCH)
    Optional<Usuario> findByEmailAndSenha(String email, String senha);

    // ADICIONE ESTE MÉTODO ABAIXO:
    @Join(value = "assignedExercisesList", type = Join.Type.LEFT_FETCH)
    Optional<Usuario> findById(Long id);
}