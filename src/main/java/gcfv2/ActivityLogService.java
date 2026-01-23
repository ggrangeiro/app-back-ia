package gcfv2;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

/**
 * Serviço centralizado para registro de atividades de professores.
 * Registra ações automaticamente quando professores executam operações.
 */
@Singleton
public class ActivityLogService {

    @Inject
    private AtividadeProfessorRepository activityRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    /**
     * Registra uma atividade de professor.
     * Só registra se o requester for um professor.
     * 
     * @param requesterId    ID do usuário que executou a ação
     * @param requesterRole  Role do usuário
     * @param actionType     Tipo da ação (STUDENT_CREATED, WORKOUT_GENERATED, etc.)
     * @param targetUserId   ID do aluno alvo (pode ser null)
     * @param targetUserName Nome do aluno alvo (pode ser null)
     * @param resourceType   Tipo do recurso (TRAINING, DIET, ANALYSIS, USER)
     * @param resourceId     ID do recurso criado/modificado (pode ser null)
     * @param metadata       Metadados adicionais em formato Map
     */
    public void logActivity(
            Long requesterId,
            String requesterRole,
            String actionType,
            Long targetUserId,
            String targetUserName,
            String resourceType,
            Long resourceId,
            Map<String, Object> metadata) {

        // Só registra se for professor
        if (!"PROFESSOR".equalsIgnoreCase(requesterRole)) {
            return;
        }

        try {
            // Buscar o manager_id do professor
            var professorOpt = usuarioRepository.findById(requesterId);
            if (professorOpt.isEmpty()) {
                return;
            }

            Usuario professor = professorOpt.get();
            Long managerId = professor.getManagerId();

            if (managerId == null) {
                // Professor sem manager, não registra
                return;
            }

            // Converter metadata para JSON string simples
            String metadataJson = null;
            if (metadata != null && !metadata.isEmpty()) {
                metadataJson = mapToJsonString(metadata);
            }

            // Criar e salvar atividade
            AtividadeProfessor activity = new AtividadeProfessor(
                    requesterId,
                    managerId,
                    actionType,
                    targetUserId,
                    targetUserName,
                    resourceType,
                    resourceId,
                    metadataJson);

            activityRepository.save(activity);

        } catch (Exception e) {
            // Log silencioso - não deve impactar a operação principal
            System.err.println("Erro ao registrar atividade de professor: " + e.getMessage());
        }
    }

    /**
     * Overload simplificado para ações sem metadados
     */
    public void logActivity(
            Long requesterId,
            String requesterRole,
            String actionType,
            Long targetUserId,
            String targetUserName,
            String resourceType,
            Long resourceId) {
        logActivity(requesterId, requesterRole, actionType, targetUserId, targetUserName,
                resourceType, resourceId, null);
    }

    /**
     * Converte um Map simples para JSON string sem dependência externa
     */
    private String mapToJsonString(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first)
                sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value == null) {
                sb.append("null");
            } else {
                sb.append("\"").append(value.toString()).append("\"");
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }
}
