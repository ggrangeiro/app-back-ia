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
                        achievementRepository
                                        .save(new Achievement("Semana Guerreira", "Complete 4 treinos em uma semana",
                                                        "BADGE_WARRIOR_WEEK", "WEEKLY_COUNT", 4));
                        achievementRepository.save(
                                        new Achievement("Início de Sequência", "Mantenha o foco por 3 dias seguidos",
                                                        "BADGE_STREAK_3", "STREAK", 3));
                        achievementRepository.save(new Achievement("Imparável", "Incriveis 10 dias seguidos de treino!",
                                        "BADGE_STREAK_10", "STREAK", 10));
                        achievementRepository.save(new Achievement("Madrugador", "Treinou antes das 7 da manhã",
                                        "BADGE_EARLY_BIRD",
                                        "TIME_WINDOW", 1));
                        achievementRepository.save(
                                        new Achievement("Coruja", "Treinou depois das 21 da noite", "BADGE_NIGHT_OWL",
                                                        "TIME_WINDOW", 2));

                        // Weather-based achievement - requires location permission from user
                        achievementRepository.save(
                                        new Achievement("Dia Chuvoso", "Treinou mesmo com chuva! Determinação é tudo!",
                                                        "BADGE_RAINY_DAY", "WEATHER", 1));

                        // Workout Like Achievements
                        achievementRepository.save(
                                        new Achievement("Gostou do Treino? 1x", "Curtiu um treino pela primeira vez!",
                                                        "BADGE_LIKE_1", "WORKOUT_LIKE_COUNT", 1));
                        achievementRepository.save(new Achievement("Curtidor de Treinos", "Curtiu 5 treinos!",
                                        "BADGE_LIKE_5", "WORKOUT_LIKE_COUNT", 5));
                        achievementRepository.save(
                                        new Achievement("Super Curtidor", "Curtiu 10 treinos! Você adora treinar!",
                                                        "BADGE_LIKE_10", "WORKOUT_LIKE_COUNT", 10));
                        achievementRepository.save(
                                        new Achievement("Amante de Treinos", "Curtiu 30 treinos! Você realmente ama treinar!",
                                                        "BADGE_LIKE_30", "WORKOUT_LIKE_COUNT", 30));
                        achievementRepository.save(
                                        new Achievement("Fã Incondicional", "Curtiu 50 treinos! Dedicação admirável!",
                                                        "BADGE_LIKE_50", "WORKOUT_LIKE_COUNT", 50));
                        achievementRepository.save(
                                        new Achievement("Lenda dos Treinos", "Curtiu 60 treinos! Você é uma inspiração!",
                                                        "BADGE_LIKE_60", "WORKOUT_LIKE_COUNT", 60));
                }
        }
}
