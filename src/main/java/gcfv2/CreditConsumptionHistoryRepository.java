package gcfv2;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface CreditConsumptionHistoryRepository extends CrudRepository<CreditConsumptionHistory, Long> {

    /**
     * Lista todo o histórico de consumo de um usuário, ordenado pelo mais recente
     */
    List<CreditConsumptionHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Conta quantas gerações gratuitas (dieta/treino) o usuário usou no mês atual
     * Usado para verificar o limite de 10 gerações gratuitas do plano STARTER
     */
    @Query("SELECT COUNT(*) FROM credit_consumption_history " +
            "WHERE user_id = :userId " +
            "AND reason IN ('DIETA', 'TREINO') " +
            "AND was_free = true " +
            "AND MONTH(created_at) = MONTH(CURRENT_DATE) " +
            "AND YEAR(created_at) = YEAR(CURRENT_DATE)")
    long countFreeGenerationsThisMonth(Long userId);

    /**
     * Conta total de créditos consumidos por um usuário
     */
    @Query("SELECT COALESCE(SUM(credits_consumed), 0) FROM credit_consumption_history WHERE user_id = :userId")
    long sumCreditsConsumedByUserId(Long userId);

    /**
     * Conta total de créditos consumidos no mês atual
     */
    @Query("SELECT COALESCE(SUM(credits_consumed), 0) FROM credit_consumption_history " +
            "WHERE user_id = :userId " +
            "AND MONTH(created_at) = MONTH(CURRENT_DATE) " +
            "AND YEAR(created_at) = YEAR(CURRENT_DATE)")
    long sumCreditsConsumedThisMonth(Long userId);
}
