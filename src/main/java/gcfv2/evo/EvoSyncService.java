package gcfv2.evo;

import gcfv2.Usuario;
import gcfv2.UsuarioRepository;
import gcfv2.dto.evo.EvoMemberDTO;
import gcfv2.dto.evo.EvoEmployeeDTO;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviço para sincronização de dados entre EVO e FitAI.
 */
@Singleton
public class EvoSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(EvoSyncService.class);

    private final EvoApiClient evoApiClient;
    private final EvoIntegrationRepository integrationRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvoMemberMapper memberMapper;
    private final EvoEmployeeMapper employeeMapper;
    private final EvoWorkoutMapper workoutMapper;

    public EvoSyncService(
            EvoApiClient evoApiClient,
            EvoIntegrationRepository integrationRepository,
            UsuarioRepository usuarioRepository,
            EvoMemberMapper memberMapper,
            EvoEmployeeMapper employeeMapper,
            EvoWorkoutMapper workoutMapper
    ) {
        this.evoApiClient = evoApiClient;
        this.integrationRepository = integrationRepository;
        this.usuarioRepository = usuarioRepository;
        this.memberMapper = memberMapper;
        this.employeeMapper = employeeMapper;
        this.workoutMapper = workoutMapper;
    }

    /**
     * Sincroniza membros do EVO para o FitAI.
     * Cria novos usuários ou atualiza existentes.
     */
    public SyncResult syncMembers(Long personalId) {
        LOG.info("Iniciando sincronização de membros para personal ID: {}", personalId);
        
        Optional<EvoIntegration> integrationOpt = integrationRepository.findByUserId(personalId);
        if (integrationOpt.isEmpty()) {
            return new SyncResult(false, 0, 0, 0, "Integração EVO não configurada");
        }
        
        EvoIntegration integration = integrationOpt.get();
        if (!integration.isActive()) {
            return new SyncResult(false, 0, 0, 0, "Integração EVO não está ativa");
        }
        
        // Buscar membros ativos do EVO
        var response = evoApiClient.getActiveMembers(integration);
        if (!response.isSuccess()) {
            LOG.error("Falha ao buscar membros do EVO: {}", response.getError());
            integration.setStatus("ERROR");
            integration.setErrorMessage("Falha ao buscar membros: " + response.getError());
            integrationRepository.update(integration);
            return new SyncResult(false, 0, 0, 0, response.getError());
        }
        
        List<EvoMemberDTO> evoMembers = response.getData();
        LOG.info("Encontrados {} membros no EVO", evoMembers.size());
        
        int created = 0;
        int updated = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        
        for (EvoMemberDTO evoMember : evoMembers) {
            try {
                if (!memberMapper.isValidForImport(evoMember)) {
                    skipped++;
                    continue;
                }
                
                // Verificar se já existe por email
                Optional<Usuario> existingByEmail = usuarioRepository.findByEmail(evoMember.getEmail());
                
                // Verificar se já existe por evoMemberId
                String evoMemberId = String.valueOf(evoMember.getIdMember());
                Optional<Usuario> existingByEvoId = usuarioRepository.findByEvoMemberId(evoMemberId);
                
                if (existingByEvoId.isPresent()) {
                    // Atualizar usuário existente
                    Usuario usuario = memberMapper.updateUsuario(existingByEvoId.get(), evoMember);
                    usuarioRepository.update(usuario);
                    updated++;
                } else if (existingByEmail.isPresent()) {
                    // Email existe mas não tem evoMemberId - vincular
                    Usuario usuario = existingByEmail.get();
                    // Verificar se pertence ao mesmo personal
                    if (personalId.equals(usuario.getPersonalId())) {
                        memberMapper.updateUsuario(usuario, evoMember);
                        usuarioRepository.update(usuario);
                        updated++;
                    } else {
                        // Email já existe para outro personal - pular
                        skipped++;
                        LOG.warn("Email {} já existe para outro personal", evoMember.getEmail());
                    }
                } else {
                    // Criar novo usuário
                    Usuario newUser = memberMapper.toUsuario(evoMember, personalId);
                    usuarioRepository.save(newUser);
                    created++;
                }
            } catch (Exception e) {
                LOG.error("Erro ao processar membro EVO {}: {}", evoMember.getIdMember(), e.getMessage());
                errors.add("Membro " + evoMember.getIdMember() + ": " + e.getMessage());
            }
        }
        
        // Atualizar último sync
        integration.setLastMembersSync(LocalDateTime.now());
        integration.setStatus("ACTIVE");
        integration.setErrorMessage(null);
        integrationRepository.update(integration);
        
        LOG.info("Sincronização de membros concluída: {} criados, {} atualizados, {} pulados", 
                 created, updated, skipped);
        
        String message = errors.isEmpty() ? "Sincronização concluída com sucesso" : 
                         "Sincronização com " + errors.size() + " erros";
        
        return new SyncResult(true, created, updated, skipped, message);
    }

    /**
     * Sincroniza funcionários do EVO como professores no FitAI.
     */
    public SyncResult syncEmployees(Long personalId) {
        LOG.info("Iniciando sincronização de funcionários para personal ID: {}", personalId);
        
        Optional<EvoIntegration> integrationOpt = integrationRepository.findByUserId(personalId);
        if (integrationOpt.isEmpty()) {
            return new SyncResult(false, 0, 0, 0, "Integração EVO não configurada");
        }
        
        EvoIntegration integration = integrationOpt.get();
        if (!integration.isActive()) {
            return new SyncResult(false, 0, 0, 0, "Integração EVO não está ativa");
        }
        
        // Buscar funcionários do EVO
        var response = evoApiClient.getEmployees(integration);
        if (!response.isSuccess()) {
            LOG.error("Falha ao buscar funcionários do EVO: {}", response.getError());
            return new SyncResult(false, 0, 0, 0, response.getError());
        }
        
        List<EvoEmployeeDTO> evoEmployees = response.getData();
        LOG.info("Encontrados {} funcionários no EVO", evoEmployees.size());
        
        int created = 0;
        int updated = 0;
        int skipped = 0;
        
        for (EvoEmployeeDTO evoEmployee : evoEmployees) {
            try {
                if (!employeeMapper.isValidForImport(evoEmployee)) {
                    skipped++;
                    continue;
                }
                
                // Verificar se já existe por email
                Optional<Usuario> existingByEmail = usuarioRepository.findByEmail(evoEmployee.getEmail());
                
                // Verificar se já existe por evoMemberId (usamos mesmo campo para funcionário)
                String evoEmployeeId = String.valueOf(evoEmployee.getIdEmployee());
                Optional<Usuario> existingByEvoId = usuarioRepository.findByEvoMemberId(evoEmployeeId);
                
                if (existingByEvoId.isPresent()) {
                    // Atualizar professor existente
                    Usuario usuario = employeeMapper.updateUsuario(existingByEvoId.get(), evoEmployee);
                    usuarioRepository.update(usuario);
                    updated++;
                } else if (existingByEmail.isPresent()) {
                    // Email existe - verificar se é do mesmo manager
                    Usuario usuario = existingByEmail.get();
                    if (personalId.equals(usuario.getManagerId())) {
                        employeeMapper.updateUsuario(usuario, evoEmployee);
                        usuarioRepository.update(usuario);
                        updated++;
                    } else {
                        skipped++;
                    }
                } else {
                    // Criar novo professor
                    Usuario newProfessor = employeeMapper.toUsuario(evoEmployee, personalId);
                    usuarioRepository.save(newProfessor);
                    created++;
                }
            } catch (Exception e) {
                LOG.error("Erro ao processar funcionário EVO {}: {}", evoEmployee.getIdEmployee(), e.getMessage());
            }
        }
        
        // Atualizar último sync
        integration.setLastEmployeesSync(LocalDateTime.now());
        integrationRepository.update(integration);
        
        LOG.info("Sincronização de funcionários concluída: {} criados, {} atualizados, {} pulados", 
                 created, updated, skipped);
        
        return new SyncResult(true, created, updated, skipped, "Sincronização concluída");
    }

    /**
     * Busca membros do EVO para preview (sem salvar).
     */
    public EvoApiClient.EvoApiResponse<List<EvoMemberDTO>> previewMembers(Long personalId) {
        Optional<EvoIntegration> integrationOpt = integrationRepository.findByUserId(personalId);
        if (integrationOpt.isEmpty()) {
            return new EvoApiClient.EvoApiResponse<>(false, null, "Integração não configurada");
        }
        return evoApiClient.getActiveMembers(integrationOpt.get());
    }

    /**
     * Busca funcionários do EVO para preview (sem salvar).
     */
    public EvoApiClient.EvoApiResponse<List<EvoEmployeeDTO>> previewEmployees(Long personalId) {
        Optional<EvoIntegration> integrationOpt = integrationRepository.findByUserId(personalId);
        if (integrationOpt.isEmpty()) {
            return new EvoApiClient.EvoApiResponse<>(false, null, "Integração não configurada");
        }
        return evoApiClient.getEmployees(integrationOpt.get());
    }

    // ========== Métodos de Sincronização de Treinos ==========

    /**
     * Exporta um treino estruturado para o EVO.
     * @param treino Treino do FitAI a ser exportado
     * @param usuario Usuário alvo (deve ter evoMemberId)
     * @param ownerId ID do Personal/Academia dono da integração
     * @return Resultado da exportação
     */
    public WorkoutSyncResult syncWorkoutToEvo(gcfv2.StructuredTreino treino, 
                                               gcfv2.Usuario usuario, Long ownerId) {
        LOG.info("Exportando treino {} para EVO (usuário: {}, owner: {})", 
                 treino.getId(), usuario.getId(), ownerId);
        
        Optional<EvoIntegration> integrationOpt = integrationRepository.findByUserId(ownerId);
        if (integrationOpt.isEmpty()) {
            return new WorkoutSyncResult(false, null, "Integração EVO não configurada");
        }
        
        EvoIntegration integration = integrationOpt.get();
        if (!integration.isActive()) {
            return new WorkoutSyncResult(false, null, "Integração EVO não está ativa");
        }
        
        if (!integration.getSyncWorkouts()) {
            return new WorkoutSyncResult(false, null, "Sincronização de treinos desativada");
        }
        
        if (usuario.getEvoMemberId() == null) {
            return new WorkoutSyncResult(false, null, "Usuário não tem membro EVO vinculado");
        }
        
        // Converter treino para formato EVO
        List<gcfv2.dto.evo.EvoWorkoutDTO> evoWorkouts = workoutMapper.toEvoWorkouts(treino, usuario, ownerId);
        
        if (evoWorkouts.isEmpty()) {
            return new WorkoutSyncResult(false, null, "Treino não tem dias para exportar");
        }
        
        List<Long> createdWorkoutIds = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (gcfv2.dto.evo.EvoWorkoutDTO evoWorkout : evoWorkouts) {
            try {
                var response = evoApiClient.createWorkout(integration, evoWorkout);
                if (response.isSuccess() && response.getData() != null) {
                    Object idObj = response.getData().get("idWorkout");
                    if (idObj != null) {
                        Long idWorkout = Long.parseLong(idObj.toString());
                        createdWorkoutIds.add(idWorkout);
                    }
                } else {
                    errors.add(evoWorkout.getWorkoutName() + ": " + response.getError());
                }
            } catch (Exception e) {
                LOG.error("Erro ao exportar treino para EVO: {}", e.getMessage());
                errors.add(evoWorkout.getWorkoutName() + ": " + e.getMessage());
            }
        }
        
        // Atualizar treino com IDs do EVO
        if (!createdWorkoutIds.isEmpty()) {
            treino.setEvoWorkoutId(createdWorkoutIds.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse(null));
            treino.setEvoSyncedAt(LocalDateTime.now());
        }
        
        if (errors.isEmpty()) {
            return new WorkoutSyncResult(true, createdWorkoutIds, "Treino exportado com sucesso");
        } else if (!createdWorkoutIds.isEmpty()) {
            return new WorkoutSyncResult(true, createdWorkoutIds, 
                "Parcialmente exportado: " + errors.size() + " erros");
        } else {
            return new WorkoutSyncResult(false, null, String.join("; ", errors));
        }
    }

    /**
     * Busca exercícios disponíveis no EVO.
     */
    public EvoApiClient.EvoApiResponse<List<java.util.Map<String, Object>>> getEvoExercises(Long personalId) {
        Optional<EvoIntegration> integrationOpt = integrationRepository.findByUserId(personalId);
        if (integrationOpt.isEmpty()) {
            return new EvoApiClient.EvoApiResponse<>(false, null, "Integração não configurada");
        }
        return evoApiClient.getExercises(integrationOpt.get());
    }

    /**
     * Analisa status de mapeamento de exercícios de um treino.
     */
    public EvoWorkoutMapper.WorkoutMappingResult analyzeMappingStatus(
            gcfv2.StructuredTreino treino, Long ownerId) {
        return workoutMapper.analyzeMappingStatus(treino, ownerId);
    }

    // ========== Classes de Resultado ==========

    @Serdeable
    public static class SyncResult {
        private final boolean success;
        private final int created;
        private final int updated;
        private final int skipped;
        private final String message;

        public SyncResult(boolean success, int created, int updated, int skipped, String message) {
            this.success = success;
            this.created = created;
            this.updated = updated;
            this.skipped = skipped;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public int getCreated() { return created; }
        public int getUpdated() { return updated; }
        public int getSkipped() { return skipped; }
        public String getMessage() { return message; }
        public int getTotal() { return created + updated; }
    }

    @Serdeable
    public static class WorkoutSyncResult {
        private final boolean success;
        private final List<Long> evoWorkoutIds;
        private final String message;

        public WorkoutSyncResult(boolean success, List<Long> evoWorkoutIds, String message) {
            this.success = success;
            this.evoWorkoutIds = evoWorkoutIds;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public List<Long> getEvoWorkoutIds() { return evoWorkoutIds; }
        public String getMessage() { return message; }
    }
}

