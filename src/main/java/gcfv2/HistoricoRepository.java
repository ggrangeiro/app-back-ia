package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface HistoricoRepository extends CrudRepository<Historico, Long> {

    // Busca filtrada por exercício (usada no gráfico)
    List<Historico> findByUserIdAndExerciseOrderByTimestampDesc(String userId, String exercise);

    // Busca geral por usuário (o método que faltava!)
    List<Historico> findByUserIdOrderByTimestampDesc(String userId);

    // Métodos para o Delete
    void deleteByIdAndUserId(Long id, String userId);
    boolean existsByIdAndUserId(Long id, String userId);
}