package gcfv2.dto.checkin;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;

/**
 * Resposta do endpoint GET /api/checkins/{userId}/week
 * Contém os check-ins formatados por semana para exibição visual.
 */
@Serdeable
public class WeeklyCheckInsResponse {

    private String weekStart;
    private String weekEnd;
    private String weekLabel;
    private Integer weeklyGoal;
    private Integer totalCheckIns;
    private List<DayCheckIn> days;

    public WeeklyCheckInsResponse() {
    }

    public String getWeekStart() {
        return weekStart;
    }

    public void setWeekStart(String weekStart) {
        this.weekStart = weekStart;
    }

    public String getWeekEnd() {
        return weekEnd;
    }

    public void setWeekEnd(String weekEnd) {
        this.weekEnd = weekEnd;
    }

    public String getWeekLabel() {
        return weekLabel;
    }

    public void setWeekLabel(String weekLabel) {
        this.weekLabel = weekLabel;
    }

    public Integer getWeeklyGoal() {
        return weeklyGoal;
    }

    public void setWeeklyGoal(Integer weeklyGoal) {
        this.weeklyGoal = weeklyGoal;
    }

    public Integer getTotalCheckIns() {
        return totalCheckIns;
    }

    public void setTotalCheckIns(Integer totalCheckIns) {
        this.totalCheckIns = totalCheckIns;
    }

    public List<DayCheckIn> getDays() {
        return days;
    }

    public void setDays(List<DayCheckIn> days) {
        this.days = days;
    }
}
