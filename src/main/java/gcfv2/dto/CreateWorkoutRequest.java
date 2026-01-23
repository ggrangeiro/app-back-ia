package gcfv2.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public class CreateWorkoutRequest {

    private Long userId;
    private String goal;
    private String level;
    private Integer frequency;
    private String trainingStyle;
    private String estimatedDuration;
    private String focus;
    private String daysData;
    private String legacyHtml;

    public CreateWorkoutRequest() {}

    // Getters e Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
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

    public String getLegacyHtml() {
        return legacyHtml;
    }

    public void setLegacyHtml(String legacyHtml) {
        this.legacyHtml = legacyHtml;
    }
}
