package gcfv2.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
@Introspected
public class WorkoutExecutionRequest {

    private Long userId;
    private Long workoutId;
    private String dayOfWeek;
    private Long executedAt;
    private String comment;
    private List<ExerciseExecutionRequest> exercises;

    public WorkoutExecutionRequest() {}

    // Getters e Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(Long workoutId) {
        this.workoutId = workoutId;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Long getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Long executedAt) {
        this.executedAt = executedAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<ExerciseExecutionRequest> getExercises() {
        return exercises;
    }

    public void setExercises(List<ExerciseExecutionRequest> exercises) {
        this.exercises = exercises;
    }
}
