package gcfv2.evo;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import io.micronaut.serde.annotation.Serdeable;
import java.time.LocalDateTime;

/**
 * Entidade para armazenar configurações de integração EVO por academia/personal.
 * Cada academia pode ter suas próprias credenciais e configurações de sync.
 */
@Serdeable
@MappedEntity("evo_integration")
public class EvoIntegration {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private Long userId; // ID do Personal/Academia owner no FitAI

    @MappedProperty("evo_username")
    private String evoUsername; // Usuário da API EVO

    @MappedProperty("evo_password")
    private String evoPassword; // Senha da API EVO (criptografada)

    @MappedProperty("evo_branch_id")
    private String evoBranchId; // ID da filial no EVO (opcional)

    @MappedProperty("evo_base_url")
    private String evoBaseUrl; // URL base da API EVO (pode variar por cliente)

    @MappedProperty("sync_members")
    private Boolean syncMembers = true;

    @MappedProperty("sync_employees")
    private Boolean syncEmployees = true;

    @MappedProperty("sync_workouts")
    private Boolean syncWorkouts = true;

    @MappedProperty("auto_sync")
    private Boolean autoSync = false; // Se true, sincroniza automaticamente ao gerar treino

    @MappedProperty("last_members_sync")
    private LocalDateTime lastMembersSync;

    @MappedProperty("last_employees_sync")
    private LocalDateTime lastEmployeesSync;

    @MappedProperty("last_workouts_sync")
    private LocalDateTime lastWorkoutsSync;

    @MappedProperty("status")
    private String status = "PENDING"; // PENDING, ACTIVE, INACTIVE, ERROR

    @MappedProperty("error_message")
    private String errorMessage;

    @DateCreated
    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    @DateUpdated
    @MappedProperty("updated_at")
    private LocalDateTime updatedAt;

    // Construtor padrão
    public EvoIntegration() {}

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEvoUsername() {
        return evoUsername;
    }

    public void setEvoUsername(String evoUsername) {
        this.evoUsername = evoUsername;
    }

    public String getEvoPassword() {
        return evoPassword;
    }

    public void setEvoPassword(String evoPassword) {
        this.evoPassword = evoPassword;
    }

    public String getEvoBranchId() {
        return evoBranchId;
    }

    public void setEvoBranchId(String evoBranchId) {
        this.evoBranchId = evoBranchId;
    }

    public String getEvoBaseUrl() {
        return evoBaseUrl;
    }

    public void setEvoBaseUrl(String evoBaseUrl) {
        this.evoBaseUrl = evoBaseUrl;
    }

    public Boolean getSyncMembers() {
        return syncMembers != null ? syncMembers : true;
    }

    public void setSyncMembers(Boolean syncMembers) {
        this.syncMembers = syncMembers;
    }

    public Boolean getSyncEmployees() {
        return syncEmployees != null ? syncEmployees : true;
    }

    public void setSyncEmployees(Boolean syncEmployees) {
        this.syncEmployees = syncEmployees;
    }

    public Boolean getSyncWorkouts() {
        return syncWorkouts != null ? syncWorkouts : true;
    }

    public void setSyncWorkouts(Boolean syncWorkouts) {
        this.syncWorkouts = syncWorkouts;
    }

    public Boolean getAutoSync() {
        return autoSync != null ? autoSync : false;
    }

    public void setAutoSync(Boolean autoSync) {
        this.autoSync = autoSync;
    }

    public LocalDateTime getLastMembersSync() {
        return lastMembersSync;
    }

    public void setLastMembersSync(LocalDateTime lastMembersSync) {
        this.lastMembersSync = lastMembersSync;
    }

    public LocalDateTime getLastEmployeesSync() {
        return lastEmployeesSync;
    }

    public void setLastEmployeesSync(LocalDateTime lastEmployeesSync) {
        this.lastEmployeesSync = lastEmployeesSync;
    }

    public LocalDateTime getLastWorkoutsSync() {
        return lastWorkoutsSync;
    }

    public void setLastWorkoutsSync(LocalDateTime lastWorkoutsSync) {
        this.lastWorkoutsSync = lastWorkoutsSync;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Verifica se a integração está ativa e configurada corretamente
     */
    public boolean isActive() {
        return "ACTIVE".equals(status) && evoUsername != null && evoPassword != null;
    }
}
