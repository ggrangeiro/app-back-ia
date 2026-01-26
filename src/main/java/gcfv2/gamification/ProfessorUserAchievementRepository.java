package gcfv2.gamification;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface ProfessorUserAchievementRepository extends CrudRepository<ProfessorUserAchievement, Long> {

    /**
     * Buscar todas as conquistas desbloqueadas por um professor
     */
    List<ProfessorUserAchievement> findByProfessorId(Long professorId);

    /**
     * Verificar se um professor já desbloqueou uma conquista específica
     */
    boolean existsByProfessorIdAndAchievementId(Long professorId, Long achievementId);

    /**
     * Contar quantas conquistas um professor desbloqueou
     */
    @Query("SELECT COUNT(*) FROM professor_user_achievement WHERE professor_id = :professorId")
    int countByProfessorId(Long professorId);

    /**
     * Buscar conquistas recentes de um professor (últimas N)
     */
    @Query("SELECT * FROM professor_user_achievement WHERE professor_id = :professorId ORDER BY unlocked_at DESC LIMIT :limit")
    List<ProfessorUserAchievement> findRecentByProfessorId(Long professorId, int limit);

    /**
     * Deletar todas as conquistas de um professor (para testes ou reset)
     */
    void deleteByProfessorId(Long professorId);
}
