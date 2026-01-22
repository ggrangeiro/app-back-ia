package gcfv2;

import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import java.time.LocalDateTime;
import java.util.List;

@JdbcRepository(dialect = Dialect.MYSQL)
public interface AtividadeProfessorRepository extends CrudRepository<AtividadeProfessor, Long> {

        /**
         * Buscar atividades por manager_id ordenadas por data decrescente
         */
        List<AtividadeProfessor> findByManagerIdOrderByCreatedAtDesc(Long managerId);

        /**
         * Buscar atividades por professor_id ordenadas por data decrescente
         */
        List<AtividadeProfessor> findByProfessorIdOrderByCreatedAtDesc(Long professorId);

        /**
         * Buscar atividades filtradas por manager, professor e tipo de ação
         */
        @Query("SELECT * FROM atividades_professor WHERE manager_id = :managerId " +
                        "AND (:professorId IS NULL OR professor_id = :professorId) " +
                        "AND (:actionType IS NULL OR action_type = :actionType) " +
                        "AND created_at >= :startDate AND created_at <= :endDate " +
                        "ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
        List<AtividadeProfessor> findActivitiesFiltered(
                        Long managerId, Long professorId, String actionType,
                        LocalDateTime startDate, LocalDateTime endDate,
                        int limit, int offset);

        /**
         * Contar atividades filtradas (para paginação)
         */
        @Query("SELECT COUNT(*) FROM atividades_professor WHERE manager_id = :managerId " +
                        "AND (:professorId IS NULL OR professor_id = :professorId) " +
                        "AND (:actionType IS NULL OR action_type = :actionType) " +
                        "AND created_at >= :startDate AND created_at <= :endDate")
        long countActivitiesFiltered(
                        Long managerId, Long professorId, String actionType,
                        LocalDateTime startDate, LocalDateTime endDate);

        /**
         * Contar ações por professor e tipo em um período
         */
        @Query("SELECT COUNT(*) FROM atividades_professor " +
                        "WHERE professor_id = :professorId AND action_type = :actionType " +
                        "AND created_at >= :startDate AND created_at <= :endDate")
        int countByProfessorIdAndActionTypeAndPeriod(
                        Long professorId, String actionType,
                        LocalDateTime startDate, LocalDateTime endDate);

        /**
         * Buscar última atividade de um professor
         */
        @Query("SELECT * FROM atividades_professor WHERE professor_id = :professorId " +
                        "ORDER BY created_at DESC LIMIT 1")
        java.util.Optional<AtividadeProfessor> findLastActivityByProfessorId(Long professorId);

        /**
         * Contar total de ações por professor em um período
         */
        @Query("SELECT COUNT(*) FROM atividades_professor " +
                        "WHERE professor_id = :professorId " +
                        "AND created_at >= :startDate AND created_at <= :endDate")
        int countTotalByProfessorIdAndPeriod(Long professorId, LocalDateTime startDate, LocalDateTime endDate);

        /**
         * Deletar atividades por professor
         */
        void deleteByProfessorId(Long professorId);

        /**
         * Deletar atividades por manager
         */
        void deleteByManagerId(Long managerId);

        /**
         * Agregação para Gráfico de Linha: Contar atividades por dia
         * Retorna lista de mapas com keys: "date", "count"
         */
        @Query("SELECT DATE(created_at) as date, COUNT(*) as count FROM atividades_professor " +
                        "WHERE manager_id = :managerId " +
                        "AND (:professorId IS NULL OR professor_id = :professorId) " +
                        "AND created_at >= :startDate AND created_at <= :endDate " +
                        "GROUP BY DATE(created_at) ORDER BY DATE(created_at)")
        List<gcfv2.dto.DailyActivityDTO> countDailyActivities(
                        Long managerId, Long professorId,
                        LocalDateTime startDate, LocalDateTime endDate);
}
