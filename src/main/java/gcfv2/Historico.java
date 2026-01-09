package gcfv2;

import io.micronaut.data.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import io.micronaut.data.model.DataType; // Importante
import java.time.Instant;

@Serdeable
@MappedEntity("historico")
public class Historico {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private String userId;

    @MappedProperty("exercise_type")
    private String exercise;

    private Integer score;
    @TypeDef(type = DataType.JSON) // <--- ADICIONE ESTA LINHA AQUI
    @MappedProperty("result_data") // O Micronaut vai serializar o objeto 'AnalysisResult' aqui
    private AnalysisResult result;

    @MappedProperty("created_at")
    private Long timestamp;

    public Historico() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getExercise() { return exercise; }
    public void setExercise(String exercise) { this.exercise = exercise; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public AnalysisResult getResult() { return result; }
    public void setResult(AnalysisResult result) { this.result = result; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}