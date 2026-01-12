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