package gcfv2.gamification;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@MappedEntity("achievement")
public class Achievement {
    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    private String name;
    private String description;

    @MappedProperty("icon_key")
    private String iconKey;

    @MappedProperty("criteria_type")
    private String criteriaType; // ENUM: WEEKLY_COUNT, STREAK, TIME_WINDOW, WEATHER, NONE (Manual)

    @MappedProperty("criteria_threshold")
    private int criteriaThreshold;

    private boolean active = true;

    public Achievement() {
    }

    public Achievement(String name, String description, String iconKey, String criteriaType, int criteriaThreshold) {
        this.name = name;
        this.description = description;
        this.iconKey = iconKey;
        this.criteriaType = criteriaType;
        this.criteriaThreshold = criteriaThreshold;
    }

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
}
