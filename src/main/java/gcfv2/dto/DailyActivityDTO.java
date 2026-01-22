package gcfv2.dto;

import io.micronaut.core.annotation.Introspected;
import java.time.LocalDate;

@Introspected
public class DailyActivityDTO {
    private LocalDate date;
    private Long count;

    public DailyActivityDTO() {
    }

    public DailyActivityDTO(LocalDate date, Long count) {
        this.date = date;
        this.count = count;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
