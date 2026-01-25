package gcfv2.gamification;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import jakarta.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@Controller("/api/gamification")
public class GamificationController {

    @Inject
    private AchievementRepository achievementRepository;

    @Inject
    private UserAchievementRepository userAchievementRepository;

    @Get("/achievements")
    public List<Achievement> listAchievements() {
        return (List<Achievement>) achievementRepository.findAll();
    }

    @Get("/users/{userId}/achievements")
    public List<UserAchievement> getUserAchievements(@PathVariable String userId) {
        return userAchievementRepository.findByUserId(userId);
    }

    // Returns a DTO with merged info (simplifies frontend)
    @Get("/users/{userId}/progress")
    public List<AchievementProgressDTO> getUserProgress(@PathVariable String userId) {
        List<Achievement> all = (List<Achievement>) achievementRepository.findAll();
        List<UserAchievement> unlocked = userAchievementRepository.findByUserId(userId);

        return all.stream().map(a -> {
            boolean isUnlocked = unlocked.stream().anyMatch(ua -> ua.getAchievementId().equals(a.getId()));
            return new AchievementProgressDTO(a, isUnlocked,
                    isUnlocked
                            ? unlocked.stream().filter(ua -> ua.getAchievementId().equals(a.getId())).findFirst().get()
                                    .getUnlockedAt()
                            : null);
        }).collect(Collectors.toList());
    }

    @io.micronaut.serde.annotation.Serdeable
    public static class AchievementProgressDTO {
        public Achievement achievement;
        public boolean unlocked;
        public java.time.LocalDateTime unlockedAt;

        public AchievementProgressDTO(Achievement achievement, boolean unlocked, java.time.LocalDateTime unlockedAt) {
            this.achievement = achievement;
            this.unlocked = unlocked;
            this.unlockedAt = unlockedAt;
        }
    }
}
