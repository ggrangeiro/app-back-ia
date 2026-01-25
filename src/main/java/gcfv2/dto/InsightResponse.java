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

    public InsightResponse(Map<String, Long> dayDistribution, Map<Integer, Long> hourDistribution,
            List<TopWorkoutDTO> topWorkouts, List<TopStudentDTO> topStudents) {
        this.dayDistribution = dayDistribution;
        this.hourDistribution = hourDistribution;
        this.topWorkouts = topWorkouts;
        this.topStudents = topStudents;
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
}
