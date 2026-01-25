package gcfv2.gamification;

import gcfv2.Usuario;
import gcfv2.UsuarioRepository;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller("/api/gamification")
public class GamificationController {

    @Inject
    private AchievementRepository achievementRepository;

    @Inject
    private UserAchievementRepository userAchievementRepository;

    @Inject
    private GamificationService gamificationService;

    @Inject
    private UsuarioRepository usuarioRepository;

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

    /**
     * Backfill: Verifica e desbloqueia achievements para TODOS os usuários existentes.
     * Útil para rodar uma vez após criar novos achievements ou para usuários antigos.
     * Apenas ADMIN pode executar.
     */
    @Post("/backfill")
    public Map<String, Object> backfillAchievements() {
        List<Usuario> allUsers = (List<Usuario>) usuarioRepository.findAll();
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> userResults = new ArrayList<>();
        int totalUnlocked = 0;

        for (Usuario user : allUsers) {
            String oduserId = String.valueOf(user.getId());
            List<Achievement> unlocked = gamificationService.checkAndUnlockAchievements(oduserId);

            if (!unlocked.isEmpty()) {
                Map<String, Object> userResult = new HashMap<>();
                userResult.put("userId", user.getId());
                userResult.put("userName", user.getNome());
                userResult.put("unlockedCount", unlocked.size());
                userResult.put("achievements", unlocked.stream().map(Achievement::getName).collect(Collectors.toList()));
                userResults.add(userResult);
                totalUnlocked += unlocked.size();
            }
        }

        result.put("totalUsersProcessed", allUsers.size());
        result.put("totalAchievementsUnlocked", totalUnlocked);
        result.put("details", userResults);
        return result;
    }

    @io.micronaut.serde.annotation.Serdeable
    public static class AchievementProgressDTO {
        private final Achievement achievement;
        private final boolean unlocked;
        private final java.time.LocalDateTime unlockedAt;

        public AchievementProgressDTO(Achievement achievement, boolean unlocked, java.time.LocalDateTime unlockedAt) {
            this.achievement = achievement;
            this.unlocked = unlocked;
            this.unlockedAt = unlockedAt;
        }

        public Achievement getAchievement() {
            return achievement;
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        public java.time.LocalDateTime getUnlockedAt() {
            return unlockedAt;
        }
    }
}
