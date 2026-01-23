package gcfv2.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

@Serdeable
@Introspected
public class LoadHistoryResponse {

    private String exerciseName;
    private List<LoadHistoryEntry> history;
    private ProgressionSuggestion progressionSuggestion;

    public LoadHistoryResponse() {}

    // Getters e Setters
    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public List<LoadHistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(List<LoadHistoryEntry> history) {
        this.history = history;
    }

    public ProgressionSuggestion getProgressionSuggestion() {
        return progressionSuggestion;
    }

    public void setProgressionSuggestion(ProgressionSuggestion progressionSuggestion) {
        this.progressionSuggestion = progressionSuggestion;
    }

    @Serdeable
    @Introspected
    public static class LoadHistoryEntry {
        private Long executionId;
        private Long executedAt;
        private String actualLoad;
        private Integer setsCompleted;

        public LoadHistoryEntry() {}

        public Long getExecutionId() {
            return executionId;
        }

        public void setExecutionId(Long executionId) {
            this.executionId = executionId;
        }

        public Long getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(Long executedAt) {
            this.executedAt = executedAt;
        }

        public String getActualLoad() {
            return actualLoad;
        }

        public void setActualLoad(String actualLoad) {
            this.actualLoad = actualLoad;
        }

        public Integer getSetsCompleted() {
            return setsCompleted;
        }

        public void setSetsCompleted(Integer setsCompleted) {
            this.setsCompleted = setsCompleted;
        }
    }

    @Serdeable
    @Introspected
    public static class ProgressionSuggestion {
        private String currentLoad;
        private String nextSuggestedLoad;
        private String reason;

        public ProgressionSuggestion() {}

        public String getCurrentLoad() {
            return currentLoad;
        }

        public void setCurrentLoad(String currentLoad) {
            this.currentLoad = currentLoad;
        }

        public String getNextSuggestedLoad() {
            return nextSuggestedLoad;
        }

        public void setNextSuggestedLoad(String nextSuggestedLoad) {
            this.nextSuggestedLoad = nextSuggestedLoad;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
