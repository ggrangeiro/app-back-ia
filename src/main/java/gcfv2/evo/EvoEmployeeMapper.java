package gcfv2.evo;

import gcfv2.Usuario;
import gcfv2.dto.evo.EvoEmployeeDTO;
import jakarta.inject.Singleton;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper para converter funcionários EVO em professores FitAI.
 */
@Singleton
public class EvoEmployeeMapper {

    /**
     * Converte um funcionário EVO em um professor FitAI.
     * 
     * @param evoEmployee Funcionário vindo da API EVO
     * @param managerId ID do Personal/Academia (manager)
     * @return Usuario com role='professor' pronto para ser salvo
     */
    public Usuario toUsuario(EvoEmployeeDTO evoEmployee, Long managerId) {
        Usuario usuario = new Usuario();
        
        // Dados básicos
        usuario.setNome(evoEmployee.getFullName());
        usuario.setEmail(evoEmployee.getEmail());
        usuario.setTelefone(evoEmployee.getPreferredPhone());
        usuario.setAvatar(evoEmployee.getPhoto());
        
        // Configuração FitAI - professor
        usuario.setRole("professor");
        usuario.setManagerId(managerId); // Vincula ao Personal/Academia
        usuario.setAccessLevel("FULL");
        
        // Gerar senha temporária
        usuario.setSenha(generateTemporaryPassword());
        
        // Créditos - professor não precisa de créditos próprios
        usuario.setCredits(0);
        usuario.setSubscriptionCredits(0);
        usuario.setPurchasedCredits(0);
        
        // Mapeamento EVO
        usuario.setEvoMemberId(String.valueOf(evoEmployee.getIdEmployee())); // Usamos mesmo campo
        if (evoEmployee.getIdBranch() != null) {
            usuario.setEvoBranchId(String.valueOf(evoEmployee.getIdBranch()));
        }
        usuario.setEvoLastSync(LocalDateTime.now());
        
        return usuario;
    }

    /**
     * Atualiza um professor existente com dados do EVO.
     *
     * @param usuario Professor existente no FitAI
     * @param evoEmployee Dados atualizados do EVO
     * @return Usuario atualizado
     */
    public Usuario updateUsuario(Usuario usuario, EvoEmployeeDTO evoEmployee) {
        // Atualiza foto se não tiver no FitAI
        if (evoEmployee.getPhoto() != null && usuario.getAvatar() == null) {
            usuario.setAvatar(evoEmployee.getPhoto());
        }
        
        // Atualiza telefone se não tiver no FitAI
        if (evoEmployee.getPreferredPhone() != null && 
            (usuario.getTelefone() == null || usuario.getTelefone().isEmpty())) {
            usuario.setTelefone(evoEmployee.getPreferredPhone());
        }

        // Nome pode ser atualizado
        if (evoEmployee.getFullName() != null && !evoEmployee.getFullName().isEmpty()) {
            usuario.setNome(evoEmployee.getFullName());
        }
        
        // Atualiza mapeamento EVO
        usuario.setEvoMemberId(String.valueOf(evoEmployee.getIdEmployee()));
        if (evoEmployee.getIdBranch() != null) {
            usuario.setEvoBranchId(String.valueOf(evoEmployee.getIdBranch()));
        }
        usuario.setEvoLastSync(LocalDateTime.now());
        
        return usuario;
    }

    /**
     * Verifica se um funcionário EVO pode ser importado como professor.
     * Requer email, nome e idealmente ser um instrutor.
     */
    public boolean isValidForImport(EvoEmployeeDTO evoEmployee) {
        // Email é obrigatório
        if (evoEmployee.getEmail() == null || evoEmployee.getEmail().isEmpty()) {
            return false;
        }
        // Nome também é necessário
        String name = evoEmployee.getFullName();
        if (name == null || name.isEmpty()) {
            return false;
        }
        // Ativo no EVO
        if (evoEmployee.getIsActive() != null && !evoEmployee.getIsActive()) {
            return false;
        }
        return true;
    }

    /**
     * Verifica se o funcionário é um instrutor/professor.
     */
    public boolean isInstructor(EvoEmployeeDTO evoEmployee) {
        return evoEmployee.isInstructor();
    }

    /**
     * Gera uma senha temporária segura.
     */
    private String generateTemporaryPassword() {
        return "EVO_PROF_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
