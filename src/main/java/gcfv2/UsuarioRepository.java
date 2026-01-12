package gcfv2;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface UsuarioRepository extends CrudRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Usuario> findByPersonalId(Long personalId);

    List<Usuario> findByRole(String role);

    @Query("UPDATE usuario SET credits = credits - 1 WHERE id = :id AND credits > 0")
    void executeConsumeCredit(Long id);

    @Query("UPDATE usuario SET credits = credits + :amount WHERE id = :id")
    void executeAddCredits(Long id, Integer amount);

    @Query("UPDATE usuario SET senha = :senha WHERE id = :id")
    void updatePassword(Long id, String senha);

    @Query("UPDATE usuario SET plan_type = :planType, subscription_status = :status, subscription_end_date = :endDate, credits_reset_date = :resetDate, credits = :credits + purchased_credits, generations_used_cycle = 0 WHERE id = :id")
    void updateSubscription(Long id, String planType, String status, java.time.LocalDateTime endDate,
            java.time.LocalDateTime resetDate, Integer credits);

    @Query("UPDATE usuario SET generations_used_cycle = generations_used_cycle + 1 WHERE id = :id")
    void incrementGenerationsUsedCycle(Long id);

    @Query("UPDATE usuario SET subscription_status = :status WHERE id = :id")
    void updateSubscriptionStatus(Long id, String status);

    // Queries para créditos separados - Mantendo coluna 'credits' sempre
    // sincronizada (Soma)

    @Query("UPDATE usuario SET subscription_credits = subscription_credits - 1, credits = credits - 1 WHERE id = :id AND subscription_credits > 0")
    void consumeSubscriptionCredit(Long id);

    @Query("UPDATE usuario SET purchased_credits = purchased_credits - 1, credits = credits - 1 WHERE id = :id AND purchased_credits > 0")
    void consumePurchasedCredit(Long id);

    @Query("UPDATE usuario SET purchased_credits = purchased_credits + :amount, credits = credits + :amount WHERE id = :id")
    void addPurchasedCredits(Long id, Integer amount);

    @Query("UPDATE usuario SET subscription_credits = :credits, credits = :credits + purchased_credits WHERE id = :id")
    void resetSubscriptionCredits(Long id, Integer credits);

    // Verifica se o requester tem autorização sobre o targetUserId
    default boolean hasPermission(Long requesterId, String requesterRole, String targetUserId) {
        // CORREÇÃO: Usando equalsIgnoreCase para aceitar "admin", "personal", etc.
        if ("ADMIN".equalsIgnoreCase(requesterRole))
            return true;

        if (requesterId.toString().equals(targetUserId))
            return true;

        if ("PERSONAL".equalsIgnoreCase(requesterRole)) {
            try {
                return findById(Long.parseLong(targetUserId))
                        .map(aluno -> requesterId.equals(aluno.getPersonalId()))
                        .orElse(false);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}