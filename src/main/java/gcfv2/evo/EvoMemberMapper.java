package gcfv2.evo;

import gcfv2.Usuario;
import gcfv2.dto.evo.EvoMemberDTO;
import jakarta.inject.Singleton;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Mapper para converter membros EVO em usuários FitAI.
 */
@Singleton
public class EvoMemberMapper {

    /**
     * Converte um membro EVO em um novo usuário FitAI.
     * 
     * @param evoMember Membro vindo da API EVO
     * @param personalId ID do Personal/Academia responsável
     * @return Usuario pronto para ser salvo
     */
    public Usuario toUsuario(EvoMemberDTO evoMember, Long personalId) {
        Usuario usuario = new Usuario();
        
        // Dados básicos
        usuario.setNome(evoMember.getFullName());
        usuario.setEmail(evoMember.getEmail());
        usuario.setTelefone(evoMember.getPreferredPhone());
        usuario.setAvatar(evoMember.getPhoto());
        
        // Configuração FitAI
        usuario.setRole("user"); // Membro EVO = Aluno no FitAI
        usuario.setPersonalId(personalId);
        usuario.setAccessLevel("FULL");
        
        // Gerar senha temporária (usuário pode resetar depois)
        usuario.setSenha(generateTemporaryPassword());
        
        // Créditos iniciais
        usuario.setCredits(0);
        usuario.setSubscriptionCredits(0);
        usuario.setPurchasedCredits(0);
        
        // Mapeamento EVO
        usuario.setEvoMemberId(String.valueOf(evoMember.getIdMember()));
        if (evoMember.getIdBranch() != null) {
            usuario.setEvoBranchId(String.valueOf(evoMember.getIdBranch()));
        }
        usuario.setEvoLastSync(LocalDateTime.now());
        
        return usuario;
    }

    /**
     * Atualiza um usuário existente com dados do EVO.
     * Não sobrescreve campos que o usuário pode ter modificado no FitAI.
     *
     * @param usuario Usuário existente no FitAI
     * @param evoMember Dados atualizados do EVO
     * @return Usuario atualizado
     */
    public Usuario updateUsuario(Usuario usuario, EvoMemberDTO evoMember) {
        // Atualiza apenas campos que vêm do EVO e não são editáveis no FitAI
        if (evoMember.getPhoto() != null && usuario.getAvatar() == null) {
            usuario.setAvatar(evoMember.getPhoto());
        }
        
        // Atualiza telefone se não tiver no FitAI
        if (evoMember.getPreferredPhone() != null && 
            (usuario.getTelefone() == null || usuario.getTelefone().isEmpty())) {
            usuario.setTelefone(evoMember.getPreferredPhone());
        }

        // Nome pode ser atualizado se mudou no EVO
        if (evoMember.getFullName() != null && !evoMember.getFullName().isEmpty()) {
            usuario.setNome(evoMember.getFullName());
        }
        
        // Atualiza mapeamento EVO
        usuario.setEvoMemberId(String.valueOf(evoMember.getIdMember()));
        if (evoMember.getIdBranch() != null) {
            usuario.setEvoBranchId(String.valueOf(evoMember.getIdBranch()));
        }
        usuario.setEvoLastSync(LocalDateTime.now());
        
        return usuario;
    }

    /**
     * Verifica se um membro EVO pode ser importado.
     * Requer pelo menos email ou nome.
     */
    public boolean isValidForImport(EvoMemberDTO evoMember) {
        // Email é obrigatório para criar conta
        if (evoMember.getEmail() == null || evoMember.getEmail().isEmpty()) {
            return false;
        }
        // Nome também é necessário
        String name = evoMember.getFullName();
        if (name == null || name.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Gera uma senha temporária segura.
     * O usuário deverá usar "Esqueci minha senha" para definir uma nova.
     */
    private String generateTemporaryPassword() {
        // Gera senha aleatória que o usuário precisará resetar
        return "EVO_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
