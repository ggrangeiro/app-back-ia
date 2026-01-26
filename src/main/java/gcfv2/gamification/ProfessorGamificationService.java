package gcfv2.gamification;

import gcfv2.AtividadeProfessor;
import gcfv2.AtividadeProfessorRepository;
import gcfv2.UsuarioRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço de gamificação para professores/personal trainers.
 * Verifica e desbloqueia conquistas baseadas nas atividades do professor.
 *
 * Usa a tabela atividades_professor para contagens, que registra:
 * - WORKOUT_GENERATED: treino criado
 * - DIET_GENERATED: dieta criada
 * - STUDENT_CREATED: aluno cadastrado
 * - ANALYSIS_PERFORMED: análise de exercício realizada
 * - ASSESSMENT_CREATED: avaliação/anamnese criada
 */
@Singleton
public class ProfessorGamificationService {

    @Inject
    private ProfessorAchievementRepository achievementRepository;

    @Inject
    private ProfessorUserAchievementRepository userAchievementRepository;

    @Inject
    private AtividadeProfessorRepository atividadeRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    /**
     * Verifica e desbloqueia conquistas para um professor.
     * Deve ser chamado após ações importantes (criar treino, dieta, aluno, etc.)
     *
     * @param professorId ID do professor
     * @return Lista de conquistas recém-desbloqueadas
     */
    public List<ProfessorAchievement> checkAndUnlockAchievements(Long professorId) {
        List<ProfessorAchievement> newUnlocks = new ArrayList<>();
        List<ProfessorAchievement> activeAchievements = achievementRepository.findByActive(true);

        // Buscar contagens das atividades do professor
        ProfessorStats stats = getProfessorStats(professorId);

        for (ProfessorAchievement achievement : activeAchievements) {
            // Pular se já desbloqueou
            if (userAchievementRepository.existsByProfessorIdAndAchievementId(professorId, achievement.getId())) {
                continue;
            }

            boolean unlocked = false;
            int threshold = achievement.getCriteriaThreshold();

            switch (achievement.getCriteriaType()) {
                case "WORKOUT_CREATED":
                    unlocked = stats.workoutsCreated >= threshold;
                    break;
                case "DIET_CREATED":
                    unlocked = stats.dietsCreated >= threshold;
                    break;
                case "STUDENT_REGISTERED":
                    unlocked = stats.studentsRegistered >= threshold;
                    break;
                case "ANALYSIS_PERFORMED":
                    unlocked = stats.analysesPerformed >= threshold;
                    break;
                case "ASSESSMENT_CREATED":
                    unlocked = stats.assessmentsCreated >= threshold;
                    break;
            }

            if (unlocked) {
                ProfessorUserAchievement ua = new ProfessorUserAchievement(professorId, achievement.getId());
                userAchievementRepository.save(ua);
                newUnlocks.add(achievement);
                System.out.println("[PROFESSOR_GAMIFICATION] Professor " + professorId +
                    " unlocked: " + achievement.getName());
            }
        }

        return newUnlocks;
    }

    /**
     * Obtém estatísticas do professor para verificação de conquistas.
     * Todas as contagens são baseadas na tabela atividades_professor.
     */
    public ProfessorStats getProfessorStats(Long professorId) {
        ProfessorStats stats = new ProfessorStats();

        try {
            // Buscar todas as atividades do professor uma única vez
            List<AtividadeProfessor> activities = atividadeRepository.findByProfessorIdOrderByCreatedAtDesc(professorId);

            // Contar por tipo de ação
            for (AtividadeProfessor activity : activities) {
                String actionType = activity.getActionType();
                if (actionType == null) continue;

                switch (actionType) {
                    case "WORKOUT_GENERATED":
                        stats.workoutsCreated++;
                        break;
                    case "DIET_GENERATED":
                        stats.dietsCreated++;
                        break;
                    case "STUDENT_CREATED":
                        stats.studentsRegistered++;
                        break;
                    case "ANALYSIS_PERFORMED":
                        stats.analysesPerformed++;
                        break;
                    case "ASSESSMENT_CREATED":
                        stats.assessmentsCreated++;
                        break;
                }
            }

            System.out.println("[PROFESSOR_GAMIFICATION] Stats for professor " + professorId + ": " +
                "workouts=" + stats.workoutsCreated +
                ", diets=" + stats.dietsCreated +
                ", students=" + stats.studentsRegistered +
                ", analyses=" + stats.analysesPerformed +
                ", assessments=" + stats.assessmentsCreated);

        } catch (Exception e) {
            System.err.println("[PROFESSOR_GAMIFICATION] Error getting stats for professor " + professorId + ": " + e.getMessage());
            e.printStackTrace();
        }

        return stats;
    }

    /**
     * Retorna o progresso de todas as conquistas para um professor.
     * IMPORTANTE: Também verifica e desbloqueia conquistas pendentes automaticamente.
     */
    public List<ProfessorAchievementProgressDTO> getProfessorProgress(Long professorId) {
        // Primeiro, verificar e desbloquear conquistas pendentes
        List<ProfessorAchievement> newlyUnlocked = checkAndUnlockAchievements(professorId);
        if (!newlyUnlocked.isEmpty()) {
            System.out.println("[PROFESSOR_GAMIFICATION] Auto-unlocked " + newlyUnlocked.size() +
                " achievements for professor " + professorId + " on progress check");
        }

        // Agora buscar o estado atualizado
        List<ProfessorAchievement> all = (List<ProfessorAchievement>) achievementRepository.findAll();
        List<ProfessorUserAchievement> unlocked = userAchievementRepository.findByProfessorId(professorId);
        ProfessorStats stats = getProfessorStats(professorId);

        List<ProfessorAchievementProgressDTO> result = new ArrayList<>();

        for (ProfessorAchievement a : all) {
            boolean isUnlocked = unlocked.stream()
                .anyMatch(ua -> ua.getAchievementId().equals(a.getId()));

            java.time.LocalDateTime unlockedAt = null;
            if (isUnlocked) {
                unlockedAt = unlocked.stream()
                    .filter(ua -> ua.getAchievementId().equals(a.getId()))
                    .findFirst()
                    .map(ProfessorUserAchievement::getUnlockedAt)
                    .orElse(null);
            }

            // Calcular progresso atual
            int currentProgress = getCurrentProgress(stats, a.getCriteriaType());

            result.add(new ProfessorAchievementProgressDTO(
                a,
                isUnlocked,
                unlockedAt,
                currentProgress
            ));
        }

        return result;
    }

    /**
     * Obtém o progresso atual baseado no tipo de critério.
     */
    private int getCurrentProgress(ProfessorStats stats, String criteriaType) {
        switch (criteriaType) {
            case "WORKOUT_CREATED":
                return stats.workoutsCreated;
            case "DIET_CREATED":
                return stats.dietsCreated;
            case "STUDENT_REGISTERED":
                return stats.studentsRegistered;
            case "ANALYSIS_PERFORMED":
                return stats.analysesPerformed;
            case "ASSESSMENT_CREATED":
                return stats.assessmentsCreated;
            default:
                return 0;
        }
    }

    /**
     * Backfill: verifica e desbloqueia conquistas para todos os professores.
     */
    public java.util.Map<String, Object> backfillAllProfessors() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        List<java.util.Map<String, Object>> professorResults = new ArrayList<>();
        int totalUnlocked = 0;

        // Buscar todos os professores (role = 'professor' ou 'PROFESSOR')
        List<gcfv2.Usuario> professors = usuarioRepository.findByRole("PROFESSOR");
        professors.addAll(usuarioRepository.findByRole("professor"));

        // Também incluir PERSONALs que podem ter conquistas
        professors.addAll(usuarioRepository.findByRole("PERSONAL"));
        professors.addAll(usuarioRepository.findByRole("personal"));

        for (gcfv2.Usuario professor : professors) {
            List<ProfessorAchievement> unlocked = checkAndUnlockAchievements(professor.getId());

            if (!unlocked.isEmpty()) {
                java.util.Map<String, Object> profResult = new java.util.HashMap<>();
                profResult.put("professorId", professor.getId());
                profResult.put("professorName", professor.getNome());
                profResult.put("unlockedCount", unlocked.size());
                profResult.put("achievements", unlocked.stream()
                    .map(ProfessorAchievement::getName)
                    .collect(java.util.stream.Collectors.toList()));
                professorResults.add(profResult);
                totalUnlocked += unlocked.size();
            }
        }

        result.put("totalProfessorsProcessed", professors.size());
        result.put("totalAchievementsUnlocked", totalUnlocked);
        result.put("details", professorResults);
        return result;
    }

    /**
     * Classe auxiliar para armazenar estatísticas do professor.
     */
    @io.micronaut.serde.annotation.Serdeable
    public static class ProfessorStats {
        public int workoutsCreated = 0;
        public int dietsCreated = 0;
        public int studentsRegistered = 0;
        public int analysesPerformed = 0;
        public int assessmentsCreated = 0;
    }

    /**
     * DTO para progresso de conquista do professor.
     */
    @io.micronaut.serde.annotation.Serdeable
    public static class ProfessorAchievementProgressDTO {
        private final ProfessorAchievement achievement;
        private final boolean unlocked;
        private final java.time.LocalDateTime unlockedAt;
        private final int currentProgress;

        public ProfessorAchievementProgressDTO(
                ProfessorAchievement achievement,
                boolean unlocked,
                java.time.LocalDateTime unlockedAt,
                int currentProgress) {
            this.achievement = achievement;
            this.unlocked = unlocked;
            this.unlockedAt = unlockedAt;
            this.currentProgress = currentProgress;
        }

        public ProfessorAchievement getAchievement() {
            return achievement;
        }

        public boolean isUnlocked() {
            return unlocked;
        }

        public java.time.LocalDateTime getUnlockedAt() {
            return unlockedAt;
        }

        public int getCurrentProgress() {
            return currentProgress;
        }
    }
}
