package gcfv2.dto.checkin;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Request body para atualização de meta semanal de treinos.
 */
@Serdeable
public class WeeklyGoalRequest {

    private Integer weeklyGoal;

    public WeeklyGoalRequest() {
    }

    public WeeklyGoalRequest(Integer weeklyGoal) {
        this.weeklyGoal = weeklyGoal;
    }

    public Integer getWeeklyGoal() {
        return weeklyGoal;
    }

    public void setWeeklyGoal(Integer weeklyGoal) {
        this.weeklyGoal = weeklyGoal;
    }
}
