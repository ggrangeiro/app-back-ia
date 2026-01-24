package gcfv2.dto.checkin;

import gcfv2.Checkin;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class CheckinResponse extends Checkin {
    private String workoutName;

    public CheckinResponse() {
        super();
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = workoutName;
    }
}
