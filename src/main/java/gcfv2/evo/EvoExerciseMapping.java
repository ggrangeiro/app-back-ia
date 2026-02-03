package gcfv2.evo;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;

/**
 * Entidade para mapear exercícios do FitAI para exercícios do EVO.
 * Permite que cada academia tenha seu próprio mapeamento.
 */
@Serdeable
@MappedEntity("evo_exercise_mapping")
public class EvoExerciseMapping {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private Long userId; // ID do Personal/Academia (owner do mapeamento)

    @MappedProperty("fitai_exercise_name")
    private String fitaiExerciseName; // Nome do exercício no FitAI (normalizado)

    @MappedProperty("evo_exercise_id")
    private Long evoExerciseId; // ID do exercício no EVO

    @MappedProperty("evo_exercise_name")
    private String evoExerciseName; // Nome do exercício no EVO (para referência)

    @MappedProperty("muscle_group")
    private String muscleGroup; // Grupo muscular (para facilitar busca)

    @MappedProperty("is_verified")
    private Boolean isVerified = false; // Mapeamento verificado manualmente

    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    @MappedProperty("updated_at")
    private LocalDateTime updatedAt;

    // Construtor padrão
    public EvoExerciseMapping() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Construtor para criação rápida
    public EvoExerciseMapping(Long userId, String fitaiExerciseName, 
                              Long evoExerciseId, String evoExerciseName) {
        this();
        this.userId = userId;
        this.fitaiExerciseName = normalizeName(fitaiExerciseName);
        this.evoExerciseId = evoExerciseId;
        this.evoExerciseName = evoExerciseName;
    }

    // Normaliza nome de exercício para busca
    public static String normalizeName(String name) {
        if (name == null) return null;
        return name.toLowerCase()
                   .trim()
                   .replaceAll("[áàâã]", "a")
                   .replaceAll("[éèê]", "e")
                   .replaceAll("[íìî]", "i")
                   .replaceAll("[óòôõ]", "o")
                   .replaceAll("[úùû]", "u")
                   .replaceAll("[ç]", "c")
                   .replaceAll("\\s+", " ");
    }

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

    public String getFitaiExerciseName() {
        return fitaiExerciseName;
    }

    public void setFitaiExerciseName(String fitaiExerciseName) {
        this.fitaiExerciseName = normalizeName(fitaiExerciseName);
    }

    public Long getEvoExerciseId() {
        return evoExerciseId;
    }

    public void setEvoExerciseId(Long evoExerciseId) {
        this.evoExerciseId = evoExerciseId;
    }

    public String getEvoExerciseName() {
        return evoExerciseName;
    }

    public void setEvoExerciseName(String evoExerciseName) {
        this.evoExerciseName = evoExerciseName;
    }

    public String getMuscleGroup() {
        return muscleGroup;
    }

    public void setMuscleGroup(String muscleGroup) {
        this.muscleGroup = muscleGroup;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
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
