package gcfv2.evo;

import gcfv2.Usuario;
import gcfv2.UsuarioRepository;
import gcfv2.dto.evo.EvoMemberDTO;
import gcfv2.dto.evo.EvoEmployeeDTO;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.serde.annotation.Serdeable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller para gerenciar integração com EVO Academia.
 * Permite configurar credenciais, testar conexão e sincronizar dados.
 */
@Controller("/api/evo")
@CrossOrigin(allowedOrigins = "*")
public class EvoIntegrationController {

    private static final Logger LOG = LoggerFactory.getLogger(EvoIntegrationController.class);

    private final EvoIntegrationRepository integrationRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvoApiClient evoApiClient;
    private final EvoSyncService syncService;
    private final EvoExerciseMappingRepository exerciseMappingRepository;
    private final gcfv2.StructuredTreinoRepository structuredTreinoRepository;

    public EvoIntegrationController(
            EvoIntegrationRepository integrationRepository,
            UsuarioRepository usuarioRepository,
            EvoApiClient evoApiClient,
            EvoSyncService syncService,
            EvoExerciseMappingRepository exerciseMappingRepository,
            gcfv2.StructuredTreinoRepository structuredTreinoRepository
    ) {
        this.integrationRepository = integrationRepository;
        this.usuarioRepository = usuarioRepository;
        this.evoApiClient = evoApiClient;
        this.syncService = syncService;
        this.exerciseMappingRepository = exerciseMappingRepository;
        this.structuredTreinoRepository = structuredTreinoRepository;
    }

    // ========== CONFIGURAÇÃO DA INTEGRAÇÃO ==========

    /**
     * GET /api/evo/integration
     * Busca configuração de integração EVO do usuário logado
     */
    @Get("/integration")
    public HttpResponse<?> getIntegration(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("GET /api/evo/integration - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Apenas Personal/Academia pode configurar EVO"));
        }
        
        Optional<EvoIntegration> integration = integrationRepository.findByUserId(requesterId);
        
        if (integration.isEmpty()) {
            return HttpResponse.ok(Map.of(
                "configured", false,
                "message", "Integração EVO não configurada"
            ));
        }
        
        EvoIntegration evo = integration.get();
        return HttpResponse.ok(Map.of(
            "configured", true,
            "status", evo.getStatus(),
            "syncMembers", evo.getSyncMembers(),
            "syncEmployees", evo.getSyncEmployees(),
            "syncWorkouts", evo.getSyncWorkouts(),
            "autoSync", evo.getAutoSync(),
            "lastMembersSync", evo.getLastMembersSync() != null ? evo.getLastMembersSync().toString() : null,
            "lastEmployeesSync", evo.getLastEmployeesSync() != null ? evo.getLastEmployeesSync().toString() : null,
            "evoBranchId", evo.getEvoBranchId(),
            "evoBaseUrl", evo.getEvoBaseUrl(),
            "errorMessage", evo.getErrorMessage()
        ));
    }

    /**
     * POST /api/evo/connect
     * Configura e testa conexão com EVO
     */
    @Post("/connect")
    public HttpResponse<?> connectToEvo(
            @Body EvoConnectRequest request,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("POST /api/evo/connect - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Apenas Personal/Academia pode configurar EVO"));
        }
        
        // Validar campos obrigatórios
        if (request.username == null || request.password == null) {
            return HttpResponse.badRequest(Map.of("message", "Usuário e senha são obrigatórios"));
        }
        
        // Testar conexão
        var testResult = evoApiClient.testConnection(
            request.username, 
            request.password, 
            request.baseUrl
        );
        
        if (!testResult.isSuccess()) {
            return HttpResponse.badRequest(Map.of(
                "success", false,
                "message", testResult.getMessage(),
                "errorCode", testResult.getErrorCode()
            ));
        }
        
        // Salvar ou atualizar integração
        EvoIntegration integration = integrationRepository.findByUserId(requesterId)
                .orElse(new EvoIntegration());
        
        integration.setUserId(requesterId);
        integration.setEvoUsername(request.username);
        integration.setEvoPassword(request.password); // TODO: Criptografar
        integration.setEvoBaseUrl(request.baseUrl);
        integration.setEvoBranchId(request.branchId);
        integration.setStatus("ACTIVE");
        integration.setErrorMessage(null);
        
        if (integration.getId() == null) {
            integrationRepository.save(integration);
        } else {
            integrationRepository.update(integration);
        }
        
        LOG.info("Integração EVO configurada com sucesso para userId={}", requesterId);
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "message", "Integração EVO configurada com sucesso!",
            "status", "ACTIVE"
        ));
    }

    /**
     * PUT /api/evo/settings
     * Atualiza configurações de sincronização
     */
    @Put("/settings")
    public HttpResponse<?> updateSettings(
            @Body EvoSettingsRequest request,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("PUT /api/evo/settings - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Apenas Personal/Academia pode configurar EVO"));
        }
        
        Optional<EvoIntegration> integrationOpt = integrationRepository.findByUserId(requesterId);
        if (integrationOpt.isEmpty()) {
            return HttpResponse.notFound(Map.of("message", "Integração EVO não encontrada"));
        }
        
        EvoIntegration integration = integrationOpt.get();
        
        if (request.syncMembers != null) integration.setSyncMembers(request.syncMembers);
        if (request.syncEmployees != null) integration.setSyncEmployees(request.syncEmployees);
        if (request.syncWorkouts != null) integration.setSyncWorkouts(request.syncWorkouts);
        if (request.autoSync != null) integration.setAutoSync(request.autoSync);
        if (request.branchId != null) integration.setEvoBranchId(request.branchId);
        
        integrationRepository.update(integration);
        
        return HttpResponse.ok(Map.of("success", true, "message", "Configurações atualizadas"));
    }

    /**
     * DELETE /api/evo/disconnect
     * Remove integração EVO
     */
    @Delete("/disconnect")
    public HttpResponse<?> disconnect(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("DELETE /api/evo/disconnect - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Apenas Personal/Academia pode configurar EVO"));
        }
        
        integrationRepository.deleteByUserId(requesterId);
        
        return HttpResponse.ok(Map.of("success", true, "message", "Integração EVO removida"));
    }

    // ========== PREVIEW DE DADOS (SEM SALVAR) ==========

    /**
     * GET /api/evo/members/preview
     * Lista membros do EVO para preview antes de importar
     */
    @Get("/members/preview")
    public HttpResponse<?> previewMembers(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("GET /api/evo/members/preview - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Acesso negado"));
        }
        
        var response = syncService.previewMembers(requesterId);
        
        if (!response.isSuccess()) {
            return HttpResponse.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("success", false, "error", response.getError()));
        }
        
        List<EvoMemberDTO> members = response.getData();
        
        // Verificar quais já existem no FitAI
        List<Map<String, Object>> enrichedMembers = new ArrayList<>();
        for (EvoMemberDTO member : members) {
            Map<String, Object> enriched = new HashMap<>();
            enriched.put("idMember", member.getIdMember());
            enriched.put("name", member.getFullName());
            enriched.put("email", member.getEmail());
            enriched.put("phone", member.getPreferredPhone());
            enriched.put("photo", member.getPhoto());
            enriched.put("membershipStatus", member.getMembershipStatus());
            enriched.put("branchName", member.getBranchName());
            
            // Verificar se já existe
            String evoId = String.valueOf(member.getIdMember());
            boolean existsByEvo = usuarioRepository.findByEvoMemberId(evoId).isPresent();
            boolean existsByEmail = member.getEmail() != null && 
                                    usuarioRepository.findByEmail(member.getEmail()).isPresent();
            
            enriched.put("alreadyImported", existsByEvo);
            enriched.put("emailExists", existsByEmail && !existsByEvo);
            enriched.put("canImport", member.getEmail() != null && !existsByEvo);
            
            enrichedMembers.add(enriched);
        }
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "total", members.size(),
            "members", enrichedMembers
        ));
    }

    /**
     * GET /api/evo/employees/preview
     * Lista funcionários do EVO para preview antes de importar
     */
    @Get("/employees/preview")
    public HttpResponse<?> previewEmployees(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("GET /api/evo/employees/preview - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Acesso negado"));
        }
        
        var response = syncService.previewEmployees(requesterId);
        
        if (!response.isSuccess()) {
            return HttpResponse.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("success", false, "error", response.getError()));
        }
        
        List<EvoEmployeeDTO> employees = response.getData();
        
        List<Map<String, Object>> enrichedEmployees = new ArrayList<>();
        for (EvoEmployeeDTO employee : employees) {
            Map<String, Object> enriched = new HashMap<>();
            enriched.put("idEmployee", employee.getIdEmployee());
            enriched.put("name", employee.getFullName());
            enriched.put("email", employee.getEmail());
            enriched.put("phone", employee.getPreferredPhone());
            enriched.put("photo", employee.getPhoto());
            enriched.put("role", employee.getRole());
            enriched.put("department", employee.getDepartment());
            enriched.put("isInstructor", employee.isInstructor());
            enriched.put("isActive", employee.getIsActive());
            
            String evoId = String.valueOf(employee.getIdEmployee());
            boolean existsByEvo = usuarioRepository.findByEvoMemberId(evoId).isPresent();
            boolean existsByEmail = employee.getEmail() != null && 
                                    usuarioRepository.findByEmail(employee.getEmail()).isPresent();
            
            enriched.put("alreadyImported", existsByEvo);
            enriched.put("emailExists", existsByEmail && !existsByEvo);
            enriched.put("canImport", employee.getEmail() != null && !existsByEvo);
            
            enrichedEmployees.add(enriched);
        }
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "total", employees.size(),
            "employees", enrichedEmployees
        ));
    }

    // ========== SINCRONIZAÇÃO ==========

    /**
     * POST /api/evo/sync/members
     * Sincroniza membros do EVO para o FitAI
     */
    @Post("/sync/members")
    public HttpResponse<?> syncMembers(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("POST /api/evo/sync/members - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Acesso negado"));
        }
        
        EvoSyncService.SyncResult result = syncService.syncMembers(requesterId);
        
        if (!result.isSuccess()) {
            return HttpResponse.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                        "success", false,
                        "message", result.getMessage()
                    ));
        }
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "message", result.getMessage(),
            "created", result.getCreated(),
            "updated", result.getUpdated(),
            "skipped", result.getSkipped(),
            "total", result.getTotal()
        ));
    }

    /**
     * POST /api/evo/sync/employees
     * Sincroniza funcionários do EVO como professores no FitAI
     */
    @Post("/sync/employees")
    public HttpResponse<?> syncEmployees(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("POST /api/evo/sync/employees - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Acesso negado"));
        }
        
        EvoSyncService.SyncResult result = syncService.syncEmployees(requesterId);
        
        if (!result.isSuccess()) {
            return HttpResponse.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                        "success", false,
                        "message", result.getMessage()
                    ));
        }
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "message", result.getMessage(),
            "created", result.getCreated(),
            "updated", result.getUpdated(),
            "skipped", result.getSkipped(),
            "total", result.getTotal()
        ));
    }

    /**
     * GET /api/evo/sync/status
     * Retorna status da última sincronização
     */
    @Get("/sync/status")
    public HttpResponse<?> getSyncStatus(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado"));
        }
        
        Optional<EvoIntegration> integrationOpt = integrationRepository.findByUserId(requesterId);
        if (integrationOpt.isEmpty()) {
            return HttpResponse.notFound(Map.of("configured", false));
        }
        
        EvoIntegration integration = integrationOpt.get();
        
        // Contar membros e professores importados do EVO
        List<Usuario> evoMembers = usuarioRepository.findEvoMembersByPersonalId(requesterId);
        List<Usuario> evoProfessors = usuarioRepository.findEvoProfessorsByManagerId(requesterId);
        
        return HttpResponse.ok(Map.of(
            "configured", true,
            "status", integration.getStatus(),
            "lastMembersSync", integration.getLastMembersSync() != null ? 
                               integration.getLastMembersSync().toString() : null,
            "lastEmployeesSync", integration.getLastEmployeesSync() != null ? 
                                 integration.getLastEmployeesSync().toString() : null,
            "importedMembersCount", evoMembers.size(),
            "importedProfessorsCount", evoProfessors.size(),
            "errorMessage", integration.getErrorMessage()
        ));
    }

    // ========== EXERCÍCIOS E MAPEAMENTO (FASE 5) ==========

    /**
     * GET /api/evo/exercises
     * Lista exercícios disponíveis no EVO
     */
    @Get("/exercises")
    public HttpResponse<?> getEvoExercises(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("GET /api/evo/exercises - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado"));
        }
        
        var response = syncService.getEvoExercises(requesterId);
        
        if (!response.isSuccess()) {
            return HttpResponse.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("success", false, "error", response.getError()));
        }
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "total", response.getData().size(),
            "exercises", response.getData()
        ));
    }

    /**
     * GET /api/evo/exercises/mappings
     * Lista mapeamentos de exercícios configurados
     */
    @Get("/exercises/mappings")
    public HttpResponse<?> getExerciseMappings(
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("GET /api/evo/exercises/mappings - requesterId={}", requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado"));
        }
        
        List<EvoExerciseMapping> mappings = exerciseMappingRepository.findByUserId(requesterId);
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "total", mappings.size(),
            "mappings", mappings
        ));
    }

    /**
     * POST /api/evo/exercises/mapping
     * Cria ou atualiza mapeamento de exercício
     */
    @Post("/exercises/mapping")
    public HttpResponse<?> createExerciseMapping(
            @Body ExerciseMappingRequest request,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("POST /api/evo/exercises/mapping - requesterId={}, exercício={}", 
                 requesterId, request.fitaiExerciseName);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado"));
        }
        
        if (request.fitaiExerciseName == null || request.evoExerciseId == null) {
            return HttpResponse.badRequest(Map.of("message", "Nome do exercício FitAI e ID EVO são obrigatórios"));
        }
        
        String normalizedName = EvoExerciseMapping.normalizeName(request.fitaiExerciseName);
        
        // Verificar se já existe mapeamento
        Optional<EvoExerciseMapping> existingOpt = exerciseMappingRepository
                .findByUserIdAndFitaiExerciseName(requesterId, normalizedName);
        
        EvoExerciseMapping mapping;
        if (existingOpt.isPresent()) {
            mapping = existingOpt.get();
            mapping.setEvoExerciseId(request.evoExerciseId);
            mapping.setEvoExerciseName(request.evoExerciseName);
            mapping.setMuscleGroup(request.muscleGroup);
            mapping.setIsVerified(true);
            mapping.setUpdatedAt(LocalDateTime.now());
            exerciseMappingRepository.update(mapping);
        } else {
            mapping = new EvoExerciseMapping(requesterId, request.fitaiExerciseName, 
                                             request.evoExerciseId, request.evoExerciseName);
            mapping.setMuscleGroup(request.muscleGroup);
            mapping.setIsVerified(true);
            exerciseMappingRepository.save(mapping);
        }
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "message", "Mapeamento salvo com sucesso",
            "mapping", mapping
        ));
    }

    /**
     * DELETE /api/evo/exercises/mapping/{id}
     * Remove mapeamento de exercício
     */
    @Delete("/exercises/mapping/{id}")
    public HttpResponse<?> deleteExerciseMapping(
            @PathVariable Long id,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("DELETE /api/evo/exercises/mapping/{} - requesterId={}", id, requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado"));
        }
        
        exerciseMappingRepository.deleteById(id);
        
        return HttpResponse.ok(Map.of("success", true, "message", "Mapeamento removido"));
    }

    // ========== EXPORTAÇÃO DE TREINOS (FASE 4) ==========

    /**
     * POST /api/evo/sync/workout/{treinoId}
     * Exporta um treino específico para o EVO
     */
    @Post("/sync/workout/{treinoId}")
    public HttpResponse<?> syncWorkout(
            @PathVariable Long treinoId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole,
            @QueryValue Long userId  // ID do usuário alvo do treino
    ) {
        LOG.info("POST /api/evo/sync/workout/{} - requesterId={}, userId={}", treinoId, requesterId, userId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado"));
        }
        
        // Buscar treino
        Optional<gcfv2.StructuredTreino> treinoOpt = structuredTreinoRepository.findById(treinoId);
        if (treinoOpt.isEmpty()) {
            return HttpResponse.notFound(Map.of("message", "Treino não encontrado"));
        }
        
        // Buscar usuário alvo
        Optional<Usuario> usuarioOpt = usuarioRepository.findById(userId);
        if (usuarioOpt.isEmpty()) {
            return HttpResponse.notFound(Map.of("message", "Usuário não encontrado"));
        }
        
        Usuario usuario = usuarioOpt.get();
        if (usuario.getEvoMemberId() == null) {
            return HttpResponse.badRequest(Map.of("message", "Usuário não tem membro EVO vinculado"));
        }
        
        EvoSyncService.WorkoutSyncResult result = syncService.syncWorkoutToEvo(
            treinoOpt.get(), usuario, requesterId);
        
        // Atualizar treino se exportado com sucesso
        if (result.isSuccess()) {
            structuredTreinoRepository.update(treinoOpt.get());
        }
        
        if (!result.isSuccess()) {
            return HttpResponse.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of(
                        "success", false,
                        "message", result.getMessage()
                    ));
        }
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "message", result.getMessage(),
            "evoWorkoutIds", result.getEvoWorkoutIds()
        ));
    }

    /**
     * GET /api/evo/workout/{treinoId}/mapping-status
     * Verifica status de mapeamento de exercícios de um treino
     */
    @Get("/workout/{treinoId}/mapping-status")
    public HttpResponse<?> getWorkoutMappingStatus(
            @PathVariable Long treinoId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole
    ) {
        LOG.info("GET /api/evo/workout/{}/mapping-status - requesterId={}", treinoId, requesterId);
        
        if (!isPersonalOrAdmin(requesterRole)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado"));
        }
        
        Optional<gcfv2.StructuredTreino> treinoOpt = structuredTreinoRepository.findById(treinoId);
        if (treinoOpt.isEmpty()) {
            return HttpResponse.notFound(Map.of("message", "Treino não encontrado"));
        }
        
        EvoWorkoutMapper.WorkoutMappingResult status = syncService.analyzeMappingStatus(
            treinoOpt.get(), requesterId);
        
        return HttpResponse.ok(Map.of(
            "success", true,
            "totalExercises", status.getTotalExercises(),
            "mappedExercises", status.getMappedExercises(),
            "unmappedExercises", status.getUnmappedExercises(),
            "unmappedNames", status.getUnmappedNames(),
            "readyToExport", !status.hasUnmapped()
        ));
    }

    // ========== HELPERS ==========

    private boolean isPersonalOrAdmin(String role) {
        return "PERSONAL".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }

    // ========== DTOs DE REQUEST ==========

    @Serdeable
    public static class EvoConnectRequest {
        public String username;
        public String password;
        public String baseUrl;  // URL customizada (opcional)
        public String branchId; // ID da filial (opcional)
    }

    @Serdeable
    public static class EvoSettingsRequest {
        public Boolean syncMembers;
        public Boolean syncEmployees;
        public Boolean syncWorkouts;
        public Boolean autoSync;
        public String branchId;
    }

    @Serdeable
    public static class ExerciseMappingRequest {
        public String fitaiExerciseName;
        public Long evoExerciseId;
        public String evoExerciseName;
        public String muscleGroup;
    }
}

