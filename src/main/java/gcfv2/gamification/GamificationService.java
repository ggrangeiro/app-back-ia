package gcfv2.gamification;

import gcfv2.Checkin;
import gcfv2.CheckinRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class GamificationService {

    @Inject
    private AchievementRepository achievementRepository;

    @Inject
    private UserAchievementRepository userAchievementRepository;

    @Inject
    private CheckinRepository checkinRepository;

    // Thread-local to pass weather context to achievement check
    private static final ThreadLocal<Boolean> isRainingContext = new ThreadLocal<>();

    /**
     * Sets the weather context for the current check-in.
     * Call this before checkAndUnlockAchievements to enable weather-based achievements.
     */
    public void setWeatherContext(boolean isRaining) {
        isRainingContext.set(isRaining);
    }

    /**
     * Clears the weather context after processing.
     */
    public void clearWeatherContext() {
        isRainingContext.remove();
    }

    public List<Achievement> checkAndUnlockAchievements(String userId) {
        List<Achievement> newUnlocks = new ArrayList<>();
        List<Achievement> activeAchievements = achievementRepository.findByActive(true);
        List<Checkin> userCheckins = checkinRepository.findByUserIdOrderByTimestampDesc(userId);

        for (Achievement achievement : activeAchievements) {
            if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
                continue;
            }

            boolean unlocked = false;
            switch (achievement.getCriteriaType()) {
                case "WEEKLY_COUNT":
                    unlocked = checkWeeklyCount(userCheckins, achievement.getCriteriaThreshold());
                    break;
                case "STREAK":
                    unlocked = checkStreak(userCheckins, achievement.getCriteriaThreshold());
                    break;
                case "TIME_WINDOW":
                    // Example: Early Bird or Night Owl checked here based on last checkin
                    unlocked = checkTimeWindow(userCheckins, achievement.getCriteriaThreshold());
                    break;
                case "WEATHER":
                    // Weather-based achievements (e.g., Rainy Day)
                    unlocked = checkWeatherCondition(achievement.getCriteriaThreshold());
                    break;
            }

            if (unlocked) {
                UserAchievement ua = new UserAchievement(userId, achievement.getId());
                userAchievementRepository.save(ua);
                newUnlocks.add(achievement);
            }
        }
        return newUnlocks;
    }

    private boolean checkWeeklyCount(List<Checkin> checkins, int threshold) {
        if (checkins.isEmpty())
            return false;

        LocalDate now = LocalDate.now();
        LocalDate startOfWeek = now.minusDays(now.getDayOfWeek().getValue() - 1); // Monday

        long count = checkins.stream()
                .map(c -> Instant.ofEpochMilli(c.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDate())
                .filter(d -> !d.isBefore(startOfWeek)) // Current week
                .distinct() // Count days, not multiple checkins per day
                .count();

        return count >= threshold;
    }

    private boolean checkStreak(List<Checkin> checkins, int threshold) {
        if (checkins.size() < threshold)
            return false;

        List<LocalDate> dates = checkins.stream()
                .map(c -> Instant.ofEpochMilli(c.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        if (dates.isEmpty())
            return false;

        int currentStreak = 1;
        LocalDate lastDate = dates.get(0);

        // Check if the streak is current (today or yesterday)
        LocalDate today = LocalDate.now();
        if (!lastDate.equals(today) && !lastDate.equals(today.minusDays(1))) {
            return false;
        }

        for (int i = 1; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            if (date.equals(lastDate.minusDays(1))) {
                currentStreak++;
                lastDate = date;
            } else {
                break;
            }
        }

        return currentStreak >= threshold;
    }

    private boolean checkTimeWindow(List<Checkin> checkins, int thresholdOrType) {
        if (checkins.isEmpty())
            return false;
        // Simple logic: check purely the LAST checkin for the "event" badges
        Checkin last = checkins.get(0);
        int hour = Instant.ofEpochMilli(last.getTimestamp()).atZone(ZoneId.systemDefault()).getHour();

        // thresholds: 1 = Early Bird (< 7), 2 = Night Owl (> 21)
        if (thresholdOrType == 1) { // Early Bird
            return hour < 7;
        } else if (thresholdOrType == 2) { // Night Owl
            return hour >= 21;
        }
        return false;
    }

    /**
     * Checks weather-based conditions.
     * Uses the thread-local context set by setWeatherContext().
     *
     * @param thresholdOrType 1 = Rainy Day (check if it's raining)
     * @return true if weather condition matches
     */
    private boolean checkWeatherCondition(int thresholdOrType) {
        Boolean isRaining = isRainingContext.get();

        if (isRaining == null) {
            // No weather context available (location not provided)
            return false;
        }

        // thresholdOrType: 1 = Rainy Day badge
        if (thresholdOrType == 1) {
            return isRaining;
        }

        return false;
    }
}
