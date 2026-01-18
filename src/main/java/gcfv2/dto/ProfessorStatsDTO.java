package gcfv2.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * DTO para estatísticas de produtividade de um professor.
 */
@Serdeable
public class ProfessorStatsDTO {

    private int studentsCreated;
    private int workoutsGenerated;
    private int dietsGenerated;
    private int analysisPerformed;
    private int totalActions;

    public ProfessorStatsDTO() {
    }

    public ProfessorStatsDTO(int studentsCreated, int workoutsGenerated, int dietsGenerated,
            int analysisPerformed) {
        this.studentsCreated = studentsCreated;
        this.workoutsGenerated = workoutsGenerated;
        this.dietsGenerated = dietsGenerated;
        this.analysisPerformed = analysisPerformed;
        this.totalActions = studentsCreated + workoutsGenerated + dietsGenerated + analysisPerformed;
    }

    // Getters e Setters
    public int getStudentsCreated() {
        return studentsCreated;
    }

    public void setStudentsCreated(int studentsCreated) {
        this.studentsCreated = studentsCreated;
        updateTotal();
    }

    public int getWorkoutsGenerated() {
        return workoutsGenerated;
    }

    public void setWorkoutsGenerated(int workoutsGenerated) {
        this.workoutsGenerated = workoutsGenerated;
        updateTotal();
    }

    public int getDietsGenerated() {
        return dietsGenerated;
    }

    public void setDietsGenerated(int dietsGenerated) {
        this.dietsGenerated = dietsGenerated;
        updateTotal();
    }

    public int getAnalysisPerformed() {
        return analysisPerformed;
    }

    public void setAnalysisPerformed(int analysisPerformed) {
        this.analysisPerformed = analysisPerformed;
        updateTotal();
    }

    public int getTotalActions() {
        return totalActions;
    }

    public void setTotalActions(int totalActions) {
        this.totalActions = totalActions;
    }

    private void updateTotal() {
        this.totalActions = studentsCreated + workoutsGenerated + dietsGenerated + analysisPerformed;
    }
}
