package gcfv2;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface AppConfigRepository extends CrudRepository<AppConfig, Long> {

    /**
     * Busca uma configuração pela chave
     */
    Optional<AppConfig> findByConfigKey(String configKey);

    /**
     * Atualiza o valor de uma configuração
     */
    @Query("UPDATE app_config SET config_value = :value, updated_at = NOW() WHERE config_key = :key")
    void updateConfigValue(String key, String value);
}
