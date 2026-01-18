package gcfv2;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;

/**
 * Entidade para registro de atividades de professores.
 * Armazena todas as ações realizadas por professores para auditoria e
 * produtividade.
 */
@Serdeable
@MappedEntity("atividades_professor")
public class AtividadeProfessor {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("professor_id")
    private Long professorId;

    @MappedProperty("manager_id")
    private Long managerId;

    @MappedProperty("action_type")
    private String actionType;

    @MappedProperty("target_user_id")
    private Long targetUserId;

    @MappedProperty("target_user_name")
    private String targetUserName;

    @MappedProperty("resource_type")
    private String resourceType;

    @MappedProperty("resource_id")
    private Long resourceId;

    private String metadata;

    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    // Construtor padrão
    public AtividadeProfessor() {
    }

    // Construtor completo
    public AtividadeProfessor(Long professorId, Long managerId, String actionType,
            Long targetUserId, String targetUserName, String resourceType,
            Long resourceId, String metadata) {
        this.professorId = professorId;
        this.managerId = managerId;
        this.actionType = actionType;
        this.targetUserId = targetUserId;
        this.targetUserName = targetUserName;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetUserName() {
        return targetUserName;
    }

    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
