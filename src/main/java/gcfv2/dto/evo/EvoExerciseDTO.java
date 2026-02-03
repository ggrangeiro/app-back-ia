package gcfv2.dto.evo;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

/**
 * DTO para representar um exercício no formato EVO.
 * Usado na exportação de treinos para o EVO.
 */
@Serdeable
public class EvoExerciseDTO {

    private Long idExercise;           // ID do exercício no EVO
    private String name;               // Nome do exercício
    private String muscleGroup;        // Grupo muscular
    private Integer series;            // Número de séries
    private String repetitions;        // Repetições (pode ser "10-12" ou "10")
    private Integer restIntervalSeconds; // Descanso em segundos
    private String observation;        // Observações/técnica
    private Integer order;             // Ordem no treino
    private String load;               // Carga (opcional)
    private String equipment;          // Equipamento
    private String videoUrl;           // URL do vídeo demonstrativo

    // Construtor padrão
    public EvoExerciseDTO() {}

    // Construtor para criação a partir de dados FitAI
    public EvoExerciseDTO(Long idExercise, Integer series, String repetitions, 
                          Integer restIntervalSeconds, String observation) {
        this.idExercise = idExercise;
        this.series = series;
        this.repetitions = repetitions;
        this.restIntervalSeconds = restIntervalSeconds;
        this.observation = observation;
    }

    // Getters e Setters
    public Long getIdExercise() {
        return idExercise;
    }

    public void setIdExercise(Long idExercise) {
        this.idExercise = idExercise;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMuscleGroup() {
        return muscleGroup;
    }

    public void setMuscleGroup(String muscleGroup) {
        this.muscleGroup = muscleGroup;
    }

    public Integer getSeries() {
        return series;
    }

    public void setSeries(Integer series) {
        this.series = series;
    }

    public String getRepetitions() {
        return repetitions;
    }

    public void setRepetitions(String repetitions) {
        this.repetitions = repetitions;
    }

    public Integer getRestIntervalSeconds() {
        return restIntervalSeconds;
    }

    public void setRestIntervalSeconds(Integer restIntervalSeconds) {
        this.restIntervalSeconds = restIntervalSeconds;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public String getLoad() {
        return load;
    }

    public void setLoad(String load) {
        this.load = load;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
