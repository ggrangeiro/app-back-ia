package gcfv2;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@MappedEntity("notifications")
public class Notification {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("recipient_id")
    private Long recipientId; // ID do Professor ou Personal

    @MappedProperty("student_id")
    private Long studentId; // ID do Aluno que gerou a ação

    @MappedProperty("student_name")
    private String studentName; // Cache do nome

    private String type; // CHECKIN, PHOTO, ALERT, etc.
    private String message;
    private Long timestamp;

    @MappedProperty("is_read")
    private Boolean isRead = false;

    public Notification() {
    }

    public Notification(Long recipientId, Long studentId, String studentName, String type, String message,
            Long timestamp) {
        this.recipientId = recipientId;
        this.studentId = studentId;
        this.studentName = studentName;
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }
}
