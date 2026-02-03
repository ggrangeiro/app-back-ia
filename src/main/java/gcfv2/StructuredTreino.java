package gcfv2;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.serde.annotation.Serdeable;
import java.time.LocalDateTime;

/**
 * Entity for Structured Training Plans (V2 API)
 * Stores training data as structured JSON instead of HTML
 * 
 * This entity is SEPARATE from Treino.java and does NOT affect the existing
 * flow.
 */
@Serdeable
@MappedEntity("structured_trainings")
public class StructuredTreino {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private String userId;

    private String goal;
    private String level; // beginner, intermediate, advanced
    private Integer frequency;

    @DateCreated
    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    // Summary fields (denormalized)
    @MappedProperty("training_style")
    private String trainingStyle;

    @MappedProperty("estimated_duration")
    private String estimatedDuration;

    private String focus; // Comma-separated focus areas

    // Full structured data as JSON string
    @MappedProperty("days_data")
    private String daysData;

    private String observations;

    // Backward compatibility
    @MappedProperty("legacy_html")
    private String legacyHtml;

    // ===== Campos de Integração EVO =====
    
    @MappedProperty("evo_workout_id")
    private String evoWorkoutId; // ID do treino no EVO após exportação
    
    @MappedProperty("evo_synced_at")
    private LocalDateTime evoSyncedAt; // Data da última sincronização com EVO

    // ===== Getters and Setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public void setFrequency(Integer frequency) {
        this.frequency = frequency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTrainingStyle() {
        return trainingStyle;
    }

    public void setTrainingStyle(String trainingStyle) {
        this.trainingStyle = trainingStyle;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(String estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public String getDaysData() {
        return daysData;
    }

    public void setDaysData(String daysData) {
        this.daysData = daysData;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getLegacyHtml() {
        return legacyHtml;
    }

    public void setLegacyHtml(String legacyHtml) {
        this.legacyHtml = legacyHtml;
    }

    // ===== Getters/Setters EVO =====
    
    public String getEvoWorkoutId() {
        return evoWorkoutId;
    }

    public void setEvoWorkoutId(String evoWorkoutId) {
        this.evoWorkoutId = evoWorkoutId;
    }

    public LocalDateTime getEvoSyncedAt() {
        return evoSyncedAt;
    }

    public void setEvoSyncedAt(LocalDateTime evoSyncedAt) {
        this.evoSyncedAt = evoSyncedAt;
    }
}
