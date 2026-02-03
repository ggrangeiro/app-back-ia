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

    // === QUERIES PARA INTEGRAÇÃO EVO ===
    
    /**
     * Buscar usuário pelo ID do membro no EVO
     */
    Optional<Usuario> findByEvoMemberId(String evoMemberId);

    /**
     * Buscar usuários de um personal que vieram do EVO
     */
    @Query("SELECT * FROM usuario WHERE personal_id = :personalId AND evo_member_id IS NOT NULL")
    List<Usuario> findEvoMembersByPersonalId(Long personalId);

    /**
     * Buscar professores de um personal que vieram do EVO  
     */
    @Query("SELECT * FROM usuario WHERE manager_id = :managerId AND evo_member_id IS NOT NULL AND role = 'professor'")
    List<Usuario> findEvoProfessorsByManagerId(Long managerId);

    // === QUERIES PARA PROFESSORES ===

    /**
     * Buscar professores de um personal (manager)
     */
    List<Usuario> findByManagerId(Long managerId);

    /**
     * Buscar professor por ID e verificar se pertence ao manager
     */
    @Query("SELECT * FROM usuario WHERE id = :professorId AND manager_id = :managerId AND role = 'professor'")
    Optional<Usuario> findProfessorByIdAndManagerId(Long professorId, Long managerId);

    /**
     * Contar alunos de um professor
     */
    @Query("SELECT COUNT(*) FROM usuario WHERE personal_id = :professorId AND role = 'user'")
    long countStudentsByProfessorId(Long professorId);

    /**
     * Buscar todos os professores com role = 'professor' de um manager
     */
    @Query("SELECT * FROM usuario WHERE manager_id = :managerId AND role = 'professor'")
    List<Usuario> findProfessorsByManagerId(Long managerId);

    // === QUERIES EXISTENTES ===

    @Query("UPDATE usuario SET credits = credits - 1 WHERE id = :id AND credits > 0")
    void executeConsumeCredit(Long id);

    @Query("UPDATE usuario SET credits = credits + :amount WHERE id = :id")
    void executeAddCredits(Long id, Integer amount);

    @Query("UPDATE usuario SET senha = :senha WHERE id = :id")
    void updatePassword(Long id, String senha);

    @Query("UPDATE usuario SET plan_type = :planType, subscription_status = :status, subscription_end_date = :endDate, credits_reset_date = :resetDate, credits = :credits + IFNULL(purchased_credits, 0), generations_used_cycle = 0 WHERE id = :id")
    void updateSubscription(Long id, String planType, String status, java.time.LocalDateTime endDate,
            java.time.LocalDateTime resetDate, Integer credits);

    @Query("UPDATE usuario SET generations_used_cycle = generations_used_cycle + 1 WHERE id = :id")
    void incrementGenerationsUsedCycle(Long id);

    @Query("UPDATE usuario SET subscription_status = :status WHERE id = :id")
    void updateSubscriptionStatus(Long id, String status);

    // Queries para créditos separados - Mantendo coluna 'credits' sempre
    // sincronizada (Soma)

    @Query("UPDATE usuario SET subscription_credits = COALESCE(subscription_credits, 0) - 1, credits = COALESCE(credits, 0) - 1 WHERE id = :id AND COALESCE(subscription_credits, 0) > 0")
    void consumeSubscriptionCredit(Long id);

    @Query("UPDATE usuario SET purchased_credits = COALESCE(purchased_credits, 0) - 1, credits = COALESCE(credits, 0) - 1 WHERE id = :id AND COALESCE(purchased_credits, 0) > 0")
    void consumePurchasedCredit(Long id);

    @Query("UPDATE usuario SET purchased_credits = COALESCE(purchased_credits, 0) + :amount, credits = COALESCE(credits, 0) + :amount WHERE id = :id")
    void addPurchasedCredits(Long id, Integer amount);

    @Query("UPDATE usuario SET subscription_credits = :credits, credits = :credits + COALESCE(purchased_credits, 0) WHERE id = :id")
    void resetSubscriptionCredits(Long id, Integer credits);

    // Queries para Cron Job de Expiração
    @Query("SELECT * FROM usuario WHERE subscription_end_date < :now AND plan_type != 'FREE'")
    List<Usuario> findExpiredSubscriptions(java.time.LocalDateTime now);

    @Query("UPDATE usuario SET plan_type = 'FREE', subscription_status = 'INACTIVE', generations_used_cycle = 0, subscription_credits = 0 WHERE id = :id")
    void executeDowngradeToFree(Long id);

    @Query("UPDATE usuario SET avatar = :avatar WHERE id = :id")
    void updateAvatar(Long id, String avatar);

    @Query("UPDATE usuario SET brand_logo = :logo WHERE id = :id")
    void updateBrandLogo(Long id, String logo);

    @Query("UPDATE usuario SET weekly_goal = :weeklyGoal WHERE id = :id")
    void updateWeeklyGoal(Long id, Integer weeklyGoal);

    /**
     * Verifica permissão de acesso a um usuário alvo.
     * 
     * Regras:
     * - ADMIN: acesso total
     * - PERSONAL: acesso a si mesmo, seus alunos (personalId), seus professores e
     * alunos de seus professores
     * - PROFESSOR: acesso a si mesmo e a TODOS os alunos do ecossistema do seu
     * Personal
     * (alunos do personal, alunos próprios, alunos de outros professores do mesmo
     * personal)
     * - USER: apenas a si mesmo
     */
    default boolean hasPermission(Long requesterId, String requesterRole, String targetUserId) {
        System.out.println(
                "HasPermission CHECK: requester=" + requesterId + " (" + requesterRole + ") target=" + targetUserId);

        // ADMIN tem acesso total
        if ("ADMIN".equalsIgnoreCase(requesterRole)) {
            System.out.println("  -> ACCESS GRANTED (ADMIN)");
            return true;
        }

        // Acesso a si mesmo
        if (requesterId.toString().equals(targetUserId)) {
            System.out.println("  -> ACCESS GRANTED (SELF)");
            return true;
        }

        try {
            Long targetId = Long.parseLong(targetUserId);
            Optional<Usuario> targetOpt = findById(targetId);

            if (targetOpt.isEmpty()) {
                System.out.println("  -> ACCESS DENIED (Target user not found: " + targetId + ")");
                return false;
            }

            Usuario target = targetOpt.get();
            System.out.println("  -> Target User: id=" + target.getId() + " role=" + target.getRole() + " personalId="
                    + target.getPersonalId() + " managerId=" + target.getManagerId());

            // PERSONAL pode acessar:
            // 1. Seus alunos diretos (target.personalId == requesterId)
            // 2. Seus professores (target.managerId == requesterId && target.role ==
            // 'professor')
            // 3. Alunos de seus professores
            if ("PERSONAL".equalsIgnoreCase(requesterRole)) {
                // Alunos diretos do personal
                if (requesterId.equals(target.getPersonalId())) {
                    System.out.println("  -> ACCESS GRANTED (Direct Student of Personal)");
                    return true;
                }

                // Professor é subordinado do personal
                if ("PROFESSOR".equalsIgnoreCase(target.getRole()) && requesterId.equals(target.getManagerId())) {
                    System.out.println("  -> ACCESS GRANTED (Professor subordinate to Personal)");
                    return true;
                }

                // Aluno de um professor subordinado
                if (target.getPersonalId() != null) {
                    Optional<Usuario> professorOpt = findById(target.getPersonalId());
                    if (professorOpt.isPresent()) {
                        Usuario professor = professorOpt.get();
                        System.out.println("    -> Checking Professor Intermediary: id=" + professor.getId() + " role="
                                + professor.getRole() + " managerId=" + professor.getManagerId());
                        if ("PROFESSOR".equalsIgnoreCase(professor.getRole()) &&
                                requesterId.equals(professor.getManagerId())) {
                            System.out.println("  -> ACCESS GRANTED (Student of subordinate Professor)");
                            return true;
                        }
                    } else {
                        System.out.println("    -> Professor linked not found ID=" + target.getPersonalId());
                    }
                }

                System.out.println("  -> ACCESS DENIED (PERSONAL logic exhausted)");
                return false;
            }

            // PROFESSOR pode acessar:
            // 1. Seus próprios alunos (target.personalId == requesterId)
            // 2. Alunos diretos do seu Manager (Personal)
            // 3. Alunos de outros professores do mesmo Personal
            // Ou seja: TODOS os alunos do ecossistema do seu Personal
            if ("PROFESSOR".equalsIgnoreCase(requesterRole)) {
                // Alunos próprios do professor
                if (requesterId.equals(target.getPersonalId())) {
                    System.out.println("  -> ACCESS GRANTED (Direct Student of Professor)");
                    return true;
                }

                // Buscar o managerId do professor requisitante
                Optional<Usuario> requesterOpt = findById(requesterId);
                if (requesterOpt.isEmpty()) {
                    System.out.println("  -> ACCESS DENIED (Requester not found)");
                    return false;
                }
                Usuario requester = requesterOpt.get();
                Long managerId = requester.getManagerId();
                System.out.println("    -> Requester (Professor) managerId=" + managerId);

                if (managerId == null) {
                    System.out.println("  -> ACCESS DENIED (Professor has no managerId)");
                    return false;
                }

                // Aluno é direto do Personal (manager do professor)
                if (managerId.equals(target.getPersonalId())) {
                    System.out.println("  -> ACCESS GRANTED (Student of Manager/Personal)");
                    return true;
                }

                // Aluno é de outro professor subordinado ao mesmo Personal
                if (target.getPersonalId() != null) {
                    Optional<Usuario> outroProfOpt = findById(target.getPersonalId());
                    if (outroProfOpt.isPresent()) {
                        Usuario outroProf = outroProfOpt.get();
                        System.out.println("    -> Checking Other Professor: id=" + outroProf.getId() + " role="
                                + outroProf.getRole() + " managerId=" + outroProf.getManagerId());
                        if ("PROFESSOR".equalsIgnoreCase(outroProf.getRole()) &&
                                managerId.equals(outroProf.getManagerId())) {
                            System.out.println("  -> ACCESS GRANTED (Student of Colleague Professor)");
                            return true;
                        }
                    }
                }

                System.out.println("  -> ACCESS DENIED (PROFESSOR logic exhausted)");
                return false;
            }

            System.out.println("  -> ACCESS DENIED (Role " + requesterRole + " not privileged)");
            return false;
        } catch (NumberFormatException e) {
            System.out.println("  -> ACCESS DENIED (Invalid format)");
            return false;
        }
    }
}