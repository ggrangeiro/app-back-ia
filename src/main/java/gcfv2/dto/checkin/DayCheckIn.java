package gcfv2.dto.checkin;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Representa um dia da semana com informações de check-in.
 */
@Serdeable
public class DayCheckIn {

    private String dayOfWeek; // monday, tuesday, etc.
    private String dayLabel; // Seg, Ter, etc.
    private String date; // YYYY-MM-DD
    private Boolean hasCheckIn;
    private CheckInDetail checkIn;

    public DayCheckIn() {
    }

    public DayCheckIn(String dayOfWeek, String dayLabel, String date, Boolean hasCheckIn, CheckInDetail checkIn) {
        this.dayOfWeek = dayOfWeek;
        this.dayLabel = dayLabel;
        this.date = date;
        this.hasCheckIn = hasCheckIn;
        this.checkIn = checkIn;
    }

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public void setDayLabel(String dayLabel) {
        this.dayLabel = dayLabel;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Boolean getHasCheckIn() {
        return hasCheckIn;
    }

    public void setHasCheckIn(Boolean hasCheckIn) {
        this.hasCheckIn = hasCheckIn;
    }

    public CheckInDetail getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(CheckInDetail checkIn) {
        this.checkIn = checkIn;
    }
}
