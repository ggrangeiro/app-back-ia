package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface UsuarioExercicioRepository extends CrudRepository<UsuarioExercicio, Long> {
    // O Micronaut usará o ID do objeto usuário automaticamente
    List<UsuarioExercicio> findByUsuario(Usuario usuario);

    void deleteByUsuario(Usuario usuario);
}