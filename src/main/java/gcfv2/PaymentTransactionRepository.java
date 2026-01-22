package gcfv2;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface PaymentTransactionRepository extends CrudRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByMpPaymentId(String mpPaymentId);

    Optional<PaymentTransaction> findByMpPreferenceId(String mpPreferenceId);

    Optional<PaymentTransaction> findByExternalReference(String externalReference);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<PaymentTransaction> findByUserIdAndStatus(Long userId, String status);

    @Query("UPDATE payment_transactions SET status = :status, mp_payment_id = :mpPaymentId, payment_method = :paymentMethod, updated_at = NOW() WHERE id = :id")
    void updatePaymentStatus(Long id, String status, String mpPaymentId, String paymentMethod);

    void deleteByUserId(Long userId);

    List<PaymentTransaction> findAllOrderByCreatedAtDesc();
}
