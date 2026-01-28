package gcfv2.model;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import java.time.LocalDateTime;

@Serdeable
@MappedEntity("group_classes")
public class GroupClass {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    private String name;

    private String description; // Default maps to name

    @MappedProperty("professor_id")
    private Long professorId;

    @MappedProperty("start_time")
    private LocalDateTime startTime;

    @MappedProperty("duration_minutes")
    private Integer durationMinutes;

    private Integer capacity;

    private String location;

    @MappedProperty("photo_url")
    private String photoUrl;

    @MappedProperty("is_recurrent")
    private Boolean isRecurrent;

    @MappedProperty("recurrence_days")
    private String recurrenceDays; // "MONDAY,WEDNESDAY"

    @MappedProperty("recurrence_group_id")
    private String recurrenceGroupId;

    @MappedProperty("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Boolean getIsRecurrent() {
        return isRecurrent;
    }

    public void setIsRecurrent(Boolean isRecurrent) {
        this.isRecurrent = isRecurrent;
    }

    public String getRecurrenceDays() {
        return recurrenceDays;
    }

    public void setRecurrenceDays(String recurrenceDays) {
        this.recurrenceDays = recurrenceDays;
    }

    public String getRecurrenceGroupId() {
        return recurrenceGroupId;
    }

    public void setRecurrenceGroupId(String recurrenceGroupId) {
        this.recurrenceGroupId = recurrenceGroupId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
