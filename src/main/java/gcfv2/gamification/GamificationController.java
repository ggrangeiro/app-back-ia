package gcfv2.gamification;

import gcfv2.Usuario;
import gcfv2.UsuarioRepository;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpMethod;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller("/api/gamification")
@CrossOrigin(allowedOrigins = { "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app" }, allowedMethods = {
                HttpMethod.GET,
                HttpMethod.POST,
                HttpMethod.OPTIONS
        })
public class GamificationController {

    @Inject
    private AchievementRepository achievementRepository;

    @Inject
    private UserAchievementRepository userAchievementRepository;

    @Inject
    private GamificationService gamificationService;

    @Inject
    private UsuarioRepository usuarioRepository;

    // === PROFESSOR ACHIEVEMENTS ===

    @Inject
    private ProfessorAchievementRepository professorAchievementRepository;

    @Inject
    private ProfessorUserAchievementRepository professorUserAchievementRepository;

    @Inject
    private ProfessorGamificationService professorGamificationService;

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
     * Backfill: Verifica e desbloqueia achievements para TODOS os usuários
     * existentes.
     * Útil para rodar uma vez após criar novos achievements ou para usuários
     * antigos.
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
                userResult.put("achievements",
                        unlocked.stream().map(Achievement::getName).collect(Collectors.toList()));
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

    // ==========================================================================
    // PROFESSOR ACHIEVEMENTS ENDPOINTS
    // ==========================================================================

    /**
     * Lista todas as conquistas disponíveis para professores.
     */
    @Get("/professor/achievements")
    public List<ProfessorAchievement> listProfessorAchievements() {
        return (List<ProfessorAchievement>) professorAchievementRepository.findAll();
    }

    /**
     * Lista conquistas desbloqueadas de um professor.
     */
    @Get("/professor/{professorId}/achievements")
    public List<ProfessorUserAchievement> getProfessorAchievements(@PathVariable Long professorId) {
        return professorUserAchievementRepository.findByProfessorId(professorId);
    }

    /**
     * Retorna o progresso completo de conquistas de um professor.
     * Inclui conquistas bloqueadas e desbloqueadas com progresso atual.
     */
    @Get("/professor/{professorId}/progress")
    public List<ProfessorGamificationService.ProfessorAchievementProgressDTO> getProfessorProgress(
            @PathVariable Long professorId) {
        return professorGamificationService.getProfessorProgress(professorId);
    }

    /**
     * Retorna estatísticas do professor (contagem de treinos, dietas, alunos, etc.)
     */
    @Get("/professor/{professorId}/stats")
    public ProfessorGamificationService.ProfessorStats getProfessorStats(@PathVariable Long professorId) {
        return professorGamificationService.getProfessorStats(professorId);
    }

    /**
     * Verifica e desbloqueia conquistas para um professor específico.
     * Retorna as conquistas recém-desbloqueadas.
     */
    @Post("/professor/{professorId}/check")
    public Map<String, Object> checkProfessorAchievements(@PathVariable Long professorId) {
        List<ProfessorAchievement> newBadges = professorGamificationService.checkAndUnlockAchievements(professorId);

        Map<String, Object> result = new HashMap<>();
        result.put("professorId", professorId);
        result.put("newBadges", newBadges);
        result.put("newBadgesCount", newBadges.size());

        return result;
    }

    /**
     * Backfill: Verifica e desbloqueia conquistas para TODOS os professores.
     * Útil para rodar após criar novas conquistas.
     */
    @Post("/professor/backfill")
    public Map<String, Object> backfillProfessorAchievements() {
        return professorGamificationService.backfillAllProfessors();
    }

    // ==========================================================================
    // PERSONAL ACHIEVEMENTS ENDPOINTS (Delegates to Professor Logic with
    // Aggregation)
    // ==========================================================================

    /**
     * Retorna o progresso de conquistas de um Personal.
     * A lógica interna detecta que é Personal e agrega atividades dos subordinados.
     */
    @Get("/personal/{personalId}/progress")
    public List<ProfessorGamificationService.ProfessorAchievementProgressDTO> getPersonalProgress(
            @PathVariable Long personalId) {
        return professorGamificationService.getProfessorProgress(personalId);
    }

    /**
     * Retorna estatísticas de um Personal.
     */
    @Get("/personal/{personalId}/stats")
    public ProfessorGamificationService.ProfessorStats getPersonalStats(@PathVariable Long personalId) {
        return professorGamificationService.getProfessorStats(personalId);
    }

    /**
     * Verifica e desbloqueia conquistas para um Personal.
     */
    @Post("/personal/{personalId}/check")
    public Map<String, Object> checkPersonalAchievements(@PathVariable Long personalId) {
        List<ProfessorAchievement> newBadges = professorGamificationService.checkAndUnlockAchievements(personalId);

        Map<String, Object> result = new HashMap<>();
        result.put("personalId", personalId);
        result.put("newBadges", newBadges);
        result.put("newBadgesCount", newBadges.size());

        return result;
    }
}
