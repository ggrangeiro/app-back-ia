package gcfv2.gamification;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import java.time.LocalDateTime;

/**
 * Entidade para conquistas disponíveis para professores/personal trainers.
 * Critérios: WORKOUT_CREATED, DIET_CREATED, STUDENT_REGISTERED, ANALYSIS_PERFORMED, ASSESSMENT_CREATED
 */
@Serdeable
@MappedEntity("professor_achievement")
public class ProfessorAchievement {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    private String name;
    private String description;

    @MappedProperty("icon_key")
    private String iconKey;

    @MappedProperty("criteria_type")
    private String criteriaType;

    @MappedProperty("criteria_threshold")
    private int criteriaThreshold;

    private boolean active = true;

    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    public ProfessorAchievement() {
    }

    public ProfessorAchievement(String name, String description, String iconKey, String criteriaType, int criteriaThreshold) {
        this.name = name;
        this.description = description;
        this.iconKey = iconKey;
        this.criteriaType = criteriaType;
        this.criteriaThreshold = criteriaThreshold;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public String getCriteriaType() {
        return criteriaType;
    }

    public void setCriteriaType(String criteriaType) {
        this.criteriaType = criteriaType;
    }

    public int getCriteriaThreshold() {
        return criteriaThreshold;
    }

    public void setCriteriaThreshold(int criteriaThreshold) {
        this.criteriaThreshold = criteriaThreshold;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
