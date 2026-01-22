package gcfv2;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;

@Serdeable
@MappedEntity("professor_goals")
public class ProfessorGoal {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    private Long managerId;
    private Long professorId;

    // TYPE: STUDENT_CREATED, WORKOUT_GENERATED, DIET_GENERATED, ANALYSIS_PERFORMED,
    // TOTAL_ACTIONS
    private String type;

    private Integer targetValue;
    private Integer month; // 1-12
    private Integer year;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProfessorGoal() {
    }

    public ProfessorGoal(Long managerId, Long professorId, String type, Integer targetValue, Integer month,
            Integer year) {
        this.managerId = managerId;
        this.professorId = professorId;
        this.type = type;
        this.targetValue = targetValue;
        this.month = month;
        this.year = year;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTargetValue() {
        return targetValue;
    }

    public void setTargetValue(Integer targetValue) {
        this.targetValue = targetValue;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
