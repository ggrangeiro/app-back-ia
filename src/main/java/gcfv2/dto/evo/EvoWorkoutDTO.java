package gcfv2.dto.evo;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.ArrayList;

/**
 * DTO para representar um treino no formato EVO.
 * Usado na exportação de treinos FitAI → EVO.
 */
@Serdeable
public class EvoWorkoutDTO {

    private Long idWorkout;            // ID do treino no EVO (retornado após criação)
    private Long idMember;             // ID do membro no EVO
    private String workoutName;        // Nome do treino
    private String description;        // Descrição/objetivo
    private String startDate;          // Data início (formato: yyyy-MM-dd)
    private String endDate;            // Data fim (opcional)
    private List<EvoExerciseDTO> exercises = new ArrayList<>(); // Lista de exercícios
    private String dayOfWeek;          // Dia da semana (MONDAY, TUESDAY, etc.)
    private String trainingType;       // Tipo de treino (A, B, C ou nome livre)
    private Boolean isActive = true;   // Treino ativo?

    // Construtor padrão
    public EvoWorkoutDTO() {}

    // Construtor para criação
    public EvoWorkoutDTO(Long idMember, String workoutName) {
        this.idMember = idMember;
        this.workoutName = workoutName;
    }

    // Getters e Setters
    public Long getIdWorkout() {
        return idWorkout;
    }

    public void setIdWorkout(Long idWorkout) {
        this.idWorkout = idWorkout;
    }

    public Long getIdMember() {
        return idMember;
    }

    public void setIdMember(Long idMember) {
        this.idMember = idMember;
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = workoutName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public List<EvoExerciseDTO> getExercises() {
        return exercises;
    }

    public void setExercises(List<EvoExerciseDTO> exercises) {
        this.exercises = exercises;
    }

    public void addExercise(EvoExerciseDTO exercise) {
        if (this.exercises == null) {
            this.exercises = new ArrayList<>();
        }
        this.exercises.add(exercise);
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getTrainingType() {
        return trainingType;
    }

    public void setTrainingType(String trainingType) {
        this.trainingType = trainingType;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
