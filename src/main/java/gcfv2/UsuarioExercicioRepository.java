package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

/**
 * Repositório responsável pelas operações na tabela 'usuario_exercicios'.
 * Esta interface permite salvar a lista de exercícios vinculada a um usuário.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface UsuarioExercicioRepository extends CrudRepository<UsuarioExercicio, Long> {
    // O Micronaut Data gerará automaticamente as implementações de save, findAll, etc.
}