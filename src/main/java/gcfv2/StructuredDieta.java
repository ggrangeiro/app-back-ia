package gcfv2;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.serde.annotation.Serdeable;
import java.time.LocalDateTime;

/**
 * Entity for Structured Diet Plans (V2 API)
 * Stores diet data as structured JSON instead of HTML
 * 
 * This entity is SEPARATE from Dieta.java and does NOT affect the existing
 * flow.
 */
@Serdeable
@MappedEntity("structured_diets")
public class StructuredDieta {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private String userId;

    private String goal;

    @DateCreated
    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    // Summary fields (denormalized)
    @MappedProperty("total_calories")
    private Integer totalCalories;

    private Integer protein;
    private Integer carbohydrates;
    private Integer fats;
    private Integer fiber;
    private String water;

    // Full structured data as JSON string
    @MappedProperty("days_data")
    private String daysData;

    private String observations;

    // Backward compatibility
    @MappedProperty("legacy_html")
    private String legacyHtml;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(Integer totalCalories) {
        this.totalCalories = totalCalories;
    }

    public Integer getProtein() {
        return protein;
    }

    public void setProtein(Integer protein) {
        this.protein = protein;
    }

    public Integer getCarbohydrates() {
        return carbohydrates;
    }

    public void setCarbohydrates(Integer carbohydrates) {
        this.carbohydrates = carbohydrates;
    }

    public Integer getFats() {
        return fats;
    }

    public void setFats(Integer fats) {
        this.fats = fats;
    }

    public Integer getFiber() {
        return fiber;
    }

    public void setFiber(Integer fiber) {
        this.fiber = fiber;
    }

    public String getWater() {
        return water;
    }

    public void setWater(String water) {
        this.water = water;
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
}
