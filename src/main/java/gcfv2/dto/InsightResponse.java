package gcfv2.dto;

import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.Map;

@Serdeable
public class InsightResponse {
    private Map<String, Long> dayDistribution;
    private Map<Integer, Long> hourDistribution;
    private List<TopWorkoutDTO> topWorkouts;
    private List<TopStudentDTO> topStudents;
    private FeedbackSummary feedbackSummary;
    private List<FeedbackDetailDTO> feedbackDetails;

    public InsightResponse(Map<String, Long> dayDistribution, Map<Integer, Long> hourDistribution,
            List<TopWorkoutDTO> topWorkouts, List<TopStudentDTO> topStudents,
            FeedbackSummary feedbackSummary, List<FeedbackDetailDTO> feedbackDetails) {
        this.dayDistribution = dayDistribution;
        this.hourDistribution = hourDistribution;
        this.topWorkouts = topWorkouts;
        this.topStudents = topStudents;
        this.feedbackSummary = feedbackSummary;
        this.feedbackDetails = feedbackDetails;
    }

    // Existing constructor for compatibility
    public InsightResponse(Map<String, Long> dayDistribution, Map<Integer, Long> hourDistribution,
            List<TopWorkoutDTO> topWorkouts, List<TopStudentDTO> topStudents) {
        this(dayDistribution, hourDistribution, topWorkouts, topStudents, null, null);
    }

    public Map<String, Long> getDayDistribution() {
        return dayDistribution;
    }

    public void setDayDistribution(Map<String, Long> dayDistribution) {
        this.dayDistribution = dayDistribution;
    }

    public Map<Integer, Long> getHourDistribution() {
        return hourDistribution;
    }

    public void setHourDistribution(Map<Integer, Long> hourDistribution) {
        this.hourDistribution = hourDistribution;
    }

    public List<TopWorkoutDTO> getTopWorkouts() {
        return topWorkouts;
    }

    public void setTopWorkouts(List<TopWorkoutDTO> topWorkouts) {
        this.topWorkouts = topWorkouts;
    }

    public List<TopStudentDTO> getTopStudents() {
        return topStudents;
    }

    public void setTopStudents(List<TopStudentDTO> topStudents) {
        this.topStudents = topStudents;
    }

    public FeedbackSummary getFeedbackSummary() {
        return feedbackSummary;
    }

    public void setFeedbackSummary(FeedbackSummary feedbackSummary) {
        this.feedbackSummary = feedbackSummary;
    }

    public List<FeedbackDetailDTO> getFeedbackDetails() {
        return feedbackDetails;
    }

    public void setFeedbackDetails(List<FeedbackDetailDTO> feedbackDetails) {
        this.feedbackDetails = feedbackDetails;
    }

    @Serdeable
    public static class TopWorkoutDTO {
        private String name;
        private Long count;

        public TopWorkoutDTO(String name, Long count) {
            this.name = name;
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }
    }

    @Serdeable
    public static class TopStudentDTO {
        private String userId;
        private String name;
        private String avatar;
        private Long checkinCount;

        public TopStudentDTO(String userId, String name, String avatar, Long checkinCount) {
            this.userId = userId;
            this.name = name;
            this.avatar = avatar;
            this.checkinCount = checkinCount;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAvatar() {
            return avatar;
        }

        public void setAvatar(String avatar) {
            this.avatar = avatar;
        }

        public Long getCheckinCount() {
            return checkinCount;
        }

        public void setCheckinCount(Long checkinCount) {
            this.checkinCount = checkinCount;
        }
    }

    @Serdeable
    public static class FeedbackSummary {
        private PeriodStats week;
        private PeriodStats month;
        private PeriodStats year;

        public FeedbackSummary(PeriodStats week, PeriodStats month, PeriodStats year) {
            this.week = week;
            this.month = month;
            this.year = year;
        }

        public PeriodStats getWeek() {
            return week;
        }

        public PeriodStats getMonth() {
            return month;
        }

        public PeriodStats getYear() {
            return year;
        }
    }

    @Serdeable
    public static class PeriodStats {
        private long likes;
        private long dislikes;

        public PeriodStats(long likes, long dislikes) {
            this.likes = likes;
            this.dislikes = dislikes;
        }

        public long getLikes() {
            return likes;
        }

        public long getDislikes() {
            return dislikes;
        }
    }

    @Serdeable
    public static class FeedbackDetailDTO {
        private String studentName;
        private String workoutName;
        private String professorName;
        private String feedbackType;
        private Long timestamp;

        public FeedbackDetailDTO(String studentName, String workoutName, String professorName, String feedbackType,
                Long timestamp) {
            this.studentName = studentName;
            this.workoutName = workoutName;
            this.professorName = professorName;
            this.feedbackType = feedbackType;
            this.timestamp = timestamp;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getWorkoutName() {
            return workoutName;
        }

        public String getProfessorName() {
            return professorName;
        }

        public String getFeedbackType() {
            return feedbackType;
        }

        public Long getTimestamp() {
            return timestamp;
        }
    }
}
