package gcfv2.dto.checkin;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Resposta do endpoint GET /api/checkins/{userId}/streak
 * Contém informações sobre a sequência de dias consecutivos de treino.
 */
@Serdeable
public class UserStreakResponse {

    private Integer currentStreak;
    private Integer longestStreak;
    private String lastCheckInDate;
    private Boolean isActiveToday;

    public UserStreakResponse() {
    }

    public UserStreakResponse(Integer currentStreak, Integer longestStreak, String lastCheckInDate,
            Boolean isActiveToday) {
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.lastCheckInDate = lastCheckInDate;
        this.isActiveToday = isActiveToday;
    }

    public Integer getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }

    public Integer getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(Integer longestStreak) {
        this.longestStreak = longestStreak;
    }

    public String getLastCheckInDate() {
        return lastCheckInDate;
    }

    public void setLastCheckInDate(String lastCheckInDate) {
        this.lastCheckInDate = lastCheckInDate;
    }

    public Boolean getIsActiveToday() {
        return isActiveToday;
    }

    public void setIsActiveToday(Boolean isActiveToday) {
        this.isActiveToday = isActiveToday;
    }
}
