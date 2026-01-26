package gcfv2.gamification;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import java.time.LocalDateTime;

/**
 * Entidade para registrar conquistas desbloqueadas por professores.
 */
@Serdeable
@MappedEntity("professor_user_achievement")
public class ProfessorUserAchievement {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("professor_id")
    private Long professorId;

    @MappedProperty("achievement_id")
    private Long achievementId;

    @MappedProperty("unlocked_at")
    private LocalDateTime unlockedAt = LocalDateTime.now();

    public ProfessorUserAchievement() {
    }

    public ProfessorUserAchievement(Long professorId, Long achievementId) {
        this.professorId = professorId;
        this.achievementId = achievementId;
        this.unlockedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfessorId() {
        return professorId;
    }

    public void setProfessorId(Long professorId) {
        this.professorId = professorId;
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
