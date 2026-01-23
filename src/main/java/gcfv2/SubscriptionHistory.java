package gcfv2;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;

@Serdeable
@MappedEntity("subscription_history")
public class SubscriptionHistory {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private Long userId;

    @MappedProperty("old_plan")
    private String oldPlan;

    @MappedProperty("new_plan")
    private String newPlan;

    @MappedProperty("change_reason")
    private String changeReason;

    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    public SubscriptionHistory() {
    }

    public SubscriptionHistory(Long userId, String oldPlan, String newPlan, String changeReason) {
        this.userId = userId;
        this.oldPlan = oldPlan;
        this.newPlan = newPlan;
        this.changeReason = changeReason;
        this.createdAt = LocalDateTime.now();
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOldPlan() {
        return oldPlan;
    }

    public void setOldPlan(String oldPlan) {
        this.oldPlan = oldPlan;
    }

    public String getNewPlan() {
        return newPlan;
    }

    public void setNewPlan(String newPlan) {
        this.newPlan = newPlan;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
