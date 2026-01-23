package gcfv2;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

@Serdeable
@Introspected
@MappedEntity("workout_executions")
public class WorkoutExecution {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private Long userId;

    @MappedProperty("workout_id")
    private Long workoutId;

    @MappedProperty("day_of_week")
    private String dayOfWeek; // monday, tuesday, etc.

    @MappedProperty("executed_at")
    private Long executedAt; // Unix timestamp em milissegundos

    private String comment;

    @DateCreated
    @MappedProperty("created_at")
    private Instant createdAt;

    @DateUpdated
    @MappedProperty("updated_at")
    private Instant updatedAt;

    // Relacionamento ONE_TO_MANY com ExerciseExecution
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "workoutExecution")
    private List<ExerciseExecution> exercises = new ArrayList<>();

    public WorkoutExecution() {}

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

    public List<ExerciseExecution> getExercises() {
        return exercises;
    }

    public void setExercises(List<ExerciseExecution> exercises) {
        this.exercises = exercises;
    }
}
