package gcfv2.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@Introspected
public class ExerciseExecutionRequest {

    private String exerciseName;
    private Integer order;
    private Integer setsCompleted;
    private String actualLoad;
    private String notes;

    public ExerciseExecutionRequest() {}

    // Getters e Setters
    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getSetsCompleted() {
        return setsCompleted;
    }

    public void setSetsCompleted(Integer setsCompleted) {
        this.setsCompleted = setsCompleted;
    }

    public String getActualLoad() {
        return actualLoad;
    }

    public void setActualLoad(String actualLoad) {
        this.actualLoad = actualLoad;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
