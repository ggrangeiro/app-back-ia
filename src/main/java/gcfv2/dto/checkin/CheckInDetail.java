package gcfv2.dto.checkin;

import io.micronaut.serde.annotation.Serdeable;

/**
 * Detalhes de um check-in individual.
 */
@Serdeable
public class CheckInDetail {

    private String id;
    private Long timestamp;
    private String comment;

    public CheckInDetail() {
    }

    public CheckInDetail(String id, Long timestamp, String comment) {
        this.id = id;
        this.timestamp = timestamp;
        this.comment = comment;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
