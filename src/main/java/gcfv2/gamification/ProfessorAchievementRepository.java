package gcfv2.gamification;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface ProfessorAchievementRepository extends CrudRepository<ProfessorAchievement, Long> {

    /**
     * Buscar todas as conquistas ativas
     */
    List<ProfessorAchievement> findByActive(boolean active);

    /**
     * Buscar conquistas por tipo de critério
     */
    List<ProfessorAchievement> findByCriteriaType(String criteriaType);

    /**
     * Buscar conquistas ativas por tipo de critério
     */
    List<ProfessorAchievement> findByActiveAndCriteriaType(boolean active, String criteriaType);
}
