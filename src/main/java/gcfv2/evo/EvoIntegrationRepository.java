package gcfv2.evo;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.Optional;

/**
 * Repository para gerenciar integrações EVO.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface EvoIntegrationRepository extends CrudRepository<EvoIntegration, Long> {

    /**
     * Busca integração EVO pelo ID do usuário (Personal/Academia)
     */
    Optional<EvoIntegration> findByUserId(Long userId);

    /**
     * Verifica se um usuário já possui integração EVO configurada
     */
    boolean existsByUserId(Long userId);

    /**
     * Busca integrações ativas
     */
    Iterable<EvoIntegration> findByStatus(String status);

    /**
     * Deleta integração pelo ID do usuário
     */
    void deleteByUserId(Long userId);
}
