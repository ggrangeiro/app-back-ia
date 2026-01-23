package gcfv2.dto;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

/**
 * DTO para resumo de produtividade de professores (dashboard).
 */
@Serdeable
public class ProductivitySummaryDTO {

    private String period;
    private String startDate;
    private String endDate;
    private List<ProfessorProductivityDTO> professors;
    private ProfessorStatsDTO totals;

    public ProductivitySummaryDTO() {
    }

    // Getters e Setters
    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
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

    public List<ProfessorProductivityDTO> getProfessors() {
        return professors;
    }

    public void setProfessors(List<ProfessorProductivityDTO> professors) {
        this.professors = professors;
    }

    public ProfessorStatsDTO getTotals() {
        return totals;
    }

    public void setTotals(ProfessorStatsDTO totals) {
        this.totals = totals;
    }

    /**
     * DTO interno para produtividade individual de professor.
     */
    @Serdeable
    public static class ProfessorProductivityDTO {
        private Long id;
        private String name;
        private String avatar;
        private ProfessorStatsDTO stats;
        private String lastActivity;

        public ProfessorProductivityDTO() {
        }

        // Getters e Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public ProfessorStatsDTO getStats() {
            return stats;
        }

        public void setStats(ProfessorStatsDTO stats) {
            this.stats = stats;
        }

        public String getLastActivity() {
            return lastActivity;
        }

        public void setLastActivity(String lastActivity) {
            this.lastActivity = lastActivity;
        }
    }
}
