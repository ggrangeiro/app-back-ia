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
     * Call this before checkAndUnlockAchievements to enable weather-based
     * achievements.
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
                case "WORKOUT_LIKE_COUNT":
                    unlocked = checkWorkoutLikeCount(userId, achievement.getCriteriaThreshold());
                    break;
                case "UNIQUE_LOCATIONS":
                    // Gym Nomad: check unique locations
                    unlocked = checkUniqueLocations(userId, achievement.getCriteriaThreshold());
                    break;
                case "DISTANCE_TRAVELLED":
                    // Traveler: check distance from previous check-in with location
                    unlocked = checkDistanceTravelled(userId, achievement.getCriteriaThreshold());
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

    @Inject
    private gcfv2.WorkoutExecutionRepository workoutExecutionRepository;

    private boolean checkWorkoutLikeCount(String userId, int threshold) {
        try {
            Long uid = Long.parseLong(userId);

            // Buscar todas as execuções com like
            List<gcfv2.WorkoutExecution> likedExecutions = workoutExecutionRepository.findByUserIdAndLikedTrue(uid);

            // Contar apenas DIAS ÚNICOS com like (máximo 1 like por dia)
            long uniqueDaysWithLike = likedExecutions.stream()
                    .filter(e -> e.getExecutedAt() != null)
                    .map(e -> Instant.ofEpochMilli(e.getExecutedAt())
                            .atZone(ZoneId.of("America/Sao_Paulo"))
                            .toLocalDate())
                    .distinct()
                    .count();

            System.out.println("[GAMIFICATION] Checking likes for user " + userId +
                    ". Total likes: " + likedExecutions.size() +
                    ", Unique days with like: " + uniqueDaysWithLike +
                    ", Threshold: " + threshold);

            return uniqueDaysWithLike >= threshold;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Checks if the user has visited enough unique locations.
     * Uses LocationUtils to group nearby coordinates.
     */
    private boolean checkUniqueLocations(String userId, int threshold) {
        List<Checkin> checkins = checkinRepository.findByUserIdAndLatitudeIsNotNullAndLongitudeIsNotNull(userId);
        if (checkins.isEmpty()) {
            return false;
        }

        List<gcfv2.utils.LocationUtils.Coordinate> uniqueLocs = new ArrayList<>();

        for (Checkin c : checkins) {
            boolean isNew = true;
            for (gcfv2.utils.LocationUtils.Coordinate known : uniqueLocs) {
                // Threshold 0.1km = 100 meters
                if (gcfv2.utils.LocationUtils.isSameLocation(c.getLatitude(), c.getLongitude(), known.lat, known.lon,
                        0.1)) {
                    isNew = false;
                    break;
                }
            }
            if (isNew) {
                uniqueLocs.add(new gcfv2.utils.LocationUtils.Coordinate(c.getLatitude(), c.getLongitude()));
            }
        }

        return uniqueLocs.size() >= threshold;
    }

    /**
     * Checks if the *latest* check-in is far enough from the *previous* valid
     * check-in.
     */
    private boolean checkDistanceTravelled(String userId, int thresholdKm) {
        // We need the latest check-in to compare against history
        List<Checkin> checkins = checkinRepository.findByUserIdAndLatitudeIsNotNullAndLongitudeIsNotNull(userId);

        // Sort by timestamp descending (just to be safe, though repository method name
        // suggests ordering might be needed if not implicit)
        // Ideally, we should add OrderByTimestampDesc to the repository method name or
        // sort here.
        // Let's sort here to be robust.
        checkins.sort((c1, c2) -> Long.compare(c2.getTimestamp(), c1.getTimestamp()));

        if (checkins.size() < 2) {
            return false;
        }

        Checkin latest = checkins.get(0);
        Checkin previous = checkins.get(1); // The one before the latest

        double dist = gcfv2.utils.LocationUtils.calculateDistanceKm(
                latest.getLatitude(), latest.getLongitude(),
                previous.getLatitude(), previous.getLongitude());

        System.out
                .println("[GAMIFICATION] Distance from last check-in: " + dist + "km (Threshold: " + thresholdKm + ")");
        return dist >= thresholdKm;
    }
}
