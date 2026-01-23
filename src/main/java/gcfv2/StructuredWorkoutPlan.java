package gcfv2;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.*;
import io.micronaut.data.model.DataType;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
@Introspected
@MappedEntity("structured_workout_plans")
public class StructuredWorkoutPlan {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private Long userId;

    private String title;

    @TypeDef(type = DataType.JSON)
    @MappedProperty("days_data")
    private String daysData; // JSON armazenado como String

    @MappedProperty("legacy_html")
    private String legacyHtml;

    @DateCreated
    @MappedProperty("created_at")
    private Instant createdAt;

    @DateUpdated
    @MappedProperty("updated_at")
    private Instant updatedAt;

    @MappedProperty("deleted_at")
    private Instant deletedAt;

    public StructuredWorkoutPlan() {}

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDaysData() {
        return daysData;
    }

    public void setDaysData(String daysData) {
        this.daysData = daysData;
    }

    public String getLegacyHtml() {
        return legacyHtml;
    }

    public void setLegacyHtml(String legacyHtml) {
        this.legacyHtml = legacyHtml;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
