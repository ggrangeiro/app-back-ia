package gcfv2;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface NotificationRepository extends CrudRepository<Notification, Long> {

    // Buscar notificações de um destinatário, ordenadas por mais recente
    List<Notification> findByRecipientIdOrderByTimestampDesc(Long recipientId);

    // Contar não lidas (opcional caso queiramos usar 'read' em vez de deletar)
    long countByRecipientId(Long recipientId);

    // Deletar todas de um destinatário
    void deleteByRecipientId(Long recipientId);
}
