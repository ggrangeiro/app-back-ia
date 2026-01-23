package gcfv2;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

@Serdeable
@Introspected
@MappedEntity("exercise_executions")
public class ExerciseExecution {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("workout_execution_id")
    private Long workoutExecutionId;

    @MappedProperty("exercise_name")
    private String exerciseName;

    @MappedProperty("exercise_order")
    private Integer exerciseOrder;

    @MappedProperty("sets_completed")
    private Integer setsCompleted;

    @MappedProperty("actual_load")
    private String actualLoad;

    private String notes;

    @DateCreated
    @MappedProperty("created_at")
    private Instant createdAt;

    // Relacionamento MANY_TO_ONE com WorkoutExecution
    @Relation(Relation.Kind.MANY_TO_ONE)
    @JsonIgnore
    private WorkoutExecution workoutExecution;

    public ExerciseExecution() {}

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWorkoutExecutionId() {
        return workoutExecutionId;
    }

    public void setWorkoutExecutionId(Long workoutExecutionId) {
        this.workoutExecutionId = workoutExecutionId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public Integer getExerciseOrder() {
        return exerciseOrder;
    }

    public void setExerciseOrder(Integer exerciseOrder) {
        this.exerciseOrder = exerciseOrder;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public WorkoutExecution getWorkoutExecution() {
        return workoutExecution;
    }

    public void setWorkoutExecution(WorkoutExecution workoutExecution) {
        this.workoutExecution = workoutExecution;
    }
}
