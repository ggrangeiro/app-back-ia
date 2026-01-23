package gcfv2.dto;

import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;

/**
 * Response DTO para retornar as últimas cargas utilizadas em cada exercício
 */
@Serdeable
public class LastUsedLoadsResponse {

    private Map<String, LoadEntry> loads;

    public LastUsedLoadsResponse() {}

    public LastUsedLoadsResponse(Map<String, LoadEntry> loads) {
        this.loads = loads;
    }

    public Map<String, LoadEntry> getLoads() {
        return loads;
    }

    public void setLoads(Map<String, LoadEntry> loads) {
        this.loads = loads;
    }

    /**
     * Representa a última carga utilizada em um exercício
     */
    @Serdeable
    public static class LoadEntry {
        private String actualLoad;
        private Long executedAt;

        public LoadEntry() {}

        public LoadEntry(String actualLoad, Long executedAt) {
            this.actualLoad = actualLoad;
            this.executedAt = executedAt;
        }

        public String getActualLoad() {
            return actualLoad;
        }

        public void setActualLoad(String actualLoad) {
            this.actualLoad = actualLoad;
        }

        public Long getExecutedAt() {
            return executedAt;
        }

        public void setExecutedAt(Long executedAt) {
            this.executedAt = executedAt;
        }
    }
}
