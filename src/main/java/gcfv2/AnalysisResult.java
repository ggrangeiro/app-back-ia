package gcfv2;

import io.micronaut.serde.annotation.Serdeable;
import java.io.Serializable; // <--- ESTE É O IMPORT QUE ESTAVA FALTANDO
import java.util.List;

@Serdeable
public record AnalysisResult(
    boolean isValidContent,
    Integer score,
    Integer repetitions,
    String gender,
    String formCorrection,
    List<Feedback> feedback,
    List<String> strengths,
    List<Improvement> improvements,
    List<String> muscleGroups
) implements Serializable {}

@Serdeable
record Feedback(String message, Integer score) {}

@Serdeable
record Improvement(String instruction, String detail) {}