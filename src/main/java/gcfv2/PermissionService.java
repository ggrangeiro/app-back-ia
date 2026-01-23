package gcfv2;

import jakarta.inject.Singleton;

/**
 * Service para validação de permissões de acesso
 *
 * Hierarquia de permissões:
 * - user: Pode acessar apenas seus próprios dados
 * - personal: Pode acessar seus dados e dados de seus alunos
 * - professor: Pode acessar seus dados, dados dos personals que gerencia e dos alunos desses personals
 * - admin: Pode acessar todos os dados
 *
 * NOTA: As tabelas de relacionamento personal→aluno e professor→personal JÁ EXISTEM no sistema.
 * Para integrar completamente, injete os repositories dessas tabelas e descomente os TODOs abaixo.
 */
@Singleton
public class PermissionService {

    /**
     * Verifica se o requester tem permissão para acessar dados do targetUserId
     */
    public boolean canAccessUserData(Long requesterId, String requesterRole, Long targetUserId) {
        // Admin pode acessar tudo
        if ("admin".equalsIgnoreCase(requesterRole)) {
            return true;
        }

        // Usuário pode acessar apenas seus próprios dados
        if ("user".equalsIgnoreCase(requesterRole)) {
            return requesterId.equals(targetUserId);
        }

        // Personal pode acessar seus dados (implementação básica)
        // TODO: Adicionar verificação na tabela personal_students quando implementada
        if ("personal".equalsIgnoreCase(requesterRole)) {
            return requesterId.equals(targetUserId);
            // Futuramente: || personalStudentsRepository.existsByPersonalIdAndStudentId(requesterId, targetUserId)
        }

        // Professor pode acessar seus dados (implementação básica)
        // TODO: Adicionar verificação nas tabelas professor_personals e personal_students quando implementadas
        if ("professor".equalsIgnoreCase(requesterRole)) {
            return requesterId.equals(targetUserId);
            // Futuramente: verificar se targetUserId é aluno de algum personal gerenciado por este professor
        }

        return false;
    }

    /**
     * Verifica se o requester pode salvar execução de treino para o targetUserId
     */
    public boolean canSaveWorkoutExecution(Long requesterId, String requesterRole, Long targetUserId) {
        // Mesmas regras de acesso
        return canAccessUserData(requesterId, requesterRole, targetUserId);
    }

    /**
     * Valida se a role é válida
     */
    public boolean isValidRole(String role) {
        if (role == null) return false;
        return role.equalsIgnoreCase("user") ||
               role.equalsIgnoreCase("personal") ||
               role.equalsIgnoreCase("professor") ||
               role.equalsIgnoreCase("admin");
    }

    /**
     * Valida se o dayOfWeek é válido
     */
    public boolean isValidDayOfWeek(String dayOfWeek) {
        if (dayOfWeek == null) return false;
        return dayOfWeek.equalsIgnoreCase("monday") ||
               dayOfWeek.equalsIgnoreCase("tuesday") ||
               dayOfWeek.equalsIgnoreCase("wednesday") ||
               dayOfWeek.equalsIgnoreCase("thursday") ||
               dayOfWeek.equalsIgnoreCase("friday") ||
               dayOfWeek.equalsIgnoreCase("saturday") ||
               dayOfWeek.equalsIgnoreCase("sunday");
    }
}
