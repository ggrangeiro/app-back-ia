package gcfv2.gamification;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class GamificationInitializer implements ApplicationEventListener<StartupEvent> {

    @Inject
    private AchievementRepository achievementRepository;

    @Override
    public void onApplicationEvent(StartupEvent event) {
        if (achievementRepository.count() == 0) {
            achievementRepository.save(new Achievement("Semana Guerreira", "Complete 4 treinos em uma semana",
                    "BADGE_WARRIOR_WEEK", "WEEKLY_COUNT", 4));
            achievementRepository.save(new Achievement("Início de Sequência", "Mantenha o foco por 3 dias seguidos",
                    "BADGE_STREAK_3", "STREAK", 3));
            achievementRepository.save(new Achievement("Imparável", "Incriveis 10 dias seguidos de treino!",
                    "BADGE_STREAK_10", "STREAK", 10));
            achievementRepository.save(new Achievement("Madrugador", "Treinou antes das 7 da manhã", "BADGE_EARLY_BIRD",
                    "TIME_WINDOW", 1));
            achievementRepository.save(
                    new Achievement("Coruja", "Treinou depois das 21 da noite", "BADGE_NIGHT_OWL", "TIME_WINDOW", 2));
            // Add more as defined in the plan
        }
    }
}
