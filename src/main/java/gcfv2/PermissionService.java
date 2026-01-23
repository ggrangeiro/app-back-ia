package gcfv2;

import jakarta.inject.Singleton;

/**
 * Service para validação de permissões de acesso
 *
 * Hierarquia de permissões:
 * - user: Pode acessar apenas seus próprios dados
 * - personal: Pode acessar seus dados e dados de seus alunos
 * - professor: Pode acessar seus dados, dados dos personals que gerencia e dos
 * alunos desses personals
 * - admin: Pode acessar todos os dados
 *
 * NOTA: As tabelas de relacionamento personal→aluno e professor→personal JÁ
 * EXISTEM no sistema.
 * Para integrar completamente, injete os repositories dessas tabelas e
 * descomente os TODOs abaixo.
 */
@Singleton
public class PermissionService {

    private final UsuarioRepository usuarioRepository;

    @jakarta.inject.Inject
    public PermissionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Verifica se o requester tem permissão para acessar dados do targetUserId
     */
    public boolean canAccessUserData(Long requesterId, String requesterRole, Long targetUserId) {
        // Delega para a lógica centralizada e robusta do UsuarioRepository
        return usuarioRepository.hasPermission(requesterId, requesterRole, targetUserId.toString());
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
        if (role == null)
            return false;
        return role.equalsIgnoreCase("user") ||
                role.equalsIgnoreCase("personal") ||
                role.equalsIgnoreCase("professor") ||
                role.equalsIgnoreCase("admin");
    }

    /**
     * Valida se o dayOfWeek é válido (aceita nome ou número 1-7)
     */
    public boolean isValidDayOfWeek(String dayOfWeek) {
        if (dayOfWeek == null)
            return false;
        return dayOfWeek.equalsIgnoreCase("monday") ||
                dayOfWeek.equalsIgnoreCase("tuesday") ||
                dayOfWeek.equalsIgnoreCase("wednesday") ||
                dayOfWeek.equalsIgnoreCase("thursday") ||
                dayOfWeek.equalsIgnoreCase("friday") ||
                dayOfWeek.equalsIgnoreCase("saturday") ||
                dayOfWeek.equalsIgnoreCase("sunday") ||
                dayOfWeek.equals("1") ||
                dayOfWeek.equals("2") ||
                dayOfWeek.equals("3") ||
                dayOfWeek.equals("4") ||
                dayOfWeek.equals("5") ||
                dayOfWeek.equals("6") ||
                dayOfWeek.equals("7");
    }

    /**
     * Normaliza o dayOfWeek para o formato texto (monday, tuesday, etc.)
     * Converte números 1-7 para os nomes correspondentes
     * 1=monday, 2=tuesday, 3=wednesday, 4=thursday, 5=friday, 6=saturday, 7=sunday
     */
    public String normalizeDayOfWeek(String dayOfWeek) {
        if (dayOfWeek == null)
            return null;
        switch (dayOfWeek) {
            case "1":
                return "monday";
            case "2":
                return "tuesday";
            case "3":
                return "wednesday";
            case "4":
                return "thursday";
            case "5":
                return "friday";
            case "6":
                return "saturday";
            case "7":
                return "sunday";
            default:
                return dayOfWeek.toLowerCase();
        }
    }
}
