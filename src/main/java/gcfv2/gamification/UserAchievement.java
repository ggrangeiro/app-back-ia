package gcfv2.gamification;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import java.time.LocalDateTime;

@Serdeable
@MappedEntity("user_achievement")
public class UserAchievement {
    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private String userId; // Changed to String to match Checkin/User userId type

    @MappedProperty("achievement_id")
    private Long achievementId;

    @MappedProperty("unlocked_at")
    private LocalDateTime unlockedAt = LocalDateTime.now();

    public UserAchievement() {
    }

    public UserAchievement(String userId, Long achievementId) {
        this.userId = userId;
        this.achievementId = achievementId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(Long achievementId) {
        this.achievementId = achievementId;
    }

    public LocalDateTime getUnlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(LocalDateTime unlockedAt) {
        this.unlockedAt = unlockedAt;
    }
}
