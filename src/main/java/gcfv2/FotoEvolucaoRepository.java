package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;

/**
 * Repository para operações de banco de dados com fotos de evolução.
 */
@JdbcRepository(dialect = Dialect.MYSQL)
public interface FotoEvolucaoRepository extends CrudRepository<FotoEvolucao, Long> {

    /**
     * Busca todas as fotos de um usuário ordenadas por data da foto (mais recente
     * primeiro)
     */
    List<FotoEvolucao> findByUserIdOrderByPhotoDateDesc(Long userId);

    /**
     * Busca fotos de um usuário filtradas por categoria
     */
    List<FotoEvolucao> findByUserIdAndCategoryOrderByPhotoDateDesc(Long userId, String category);

    /**
     * Busca a foto mais recente de cada categoria para um usuário
     */
    List<FotoEvolucao> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Conta quantas fotos um usuário tem
     */
    long countByUserId(Long userId);

    /**
     * Deleta todas as fotos de um usuário (usado ao deletar conta)
     */
    void deleteByUserId(Long userId);
}
