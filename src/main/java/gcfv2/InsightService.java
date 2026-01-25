package gcfv2;

import gcfv2.dto.InsightResponse;
import gcfv2.dto.InsightResponse.TopStudentDTO;
import gcfv2.dto.InsightResponse.TopWorkoutDTO;
import jakarta.inject.Singleton;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class InsightService {

    private final UsuarioRepository usuarioRepository;
    private final CheckinRepository checkinRepository;
    private final StructuredWorkoutPlanRepository structuredWorkoutPlanRepository;
    private final TreinoRepository treinoRepository;

    public InsightService(UsuarioRepository usuarioRepository, CheckinRepository checkinRepository,
            StructuredWorkoutPlanRepository structuredWorkoutPlanRepository, TreinoRepository treinoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.checkinRepository = checkinRepository;
        this.structuredWorkoutPlanRepository = structuredWorkoutPlanRepository;
        this.treinoRepository = treinoRepository;
    }

    public InsightResponse getInsightsForProfessor(Long professorId, String period) {
        // 1. Identify Students
        List<Usuario> students = usuarioRepository.findByPersonalId(professorId);
        if (students.isEmpty()) {
            return new InsightResponse(new HashMap<>(), new HashMap<>(), new ArrayList<>(), new ArrayList<>(), null,
                    new ArrayList<>());
        }

        List<String> studentIds = students.stream().map(u -> String.valueOf(u.getId())).collect(Collectors.toList());
        Map<String, Usuario> studentMap = students.stream()
                .collect(Collectors.toMap(u -> String.valueOf(u.getId()), Function.identity()));

        // 2. Determine Time Range
        LocalDateTime now = LocalDateTime.now();
        long end = System.currentTimeMillis();
        long startWeek = now.minusWeeks(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long startMonth = now.minusMonths(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long startYear = now.minusYears(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        long mainStart;
        switch (period.toUpperCase()) {
            case "WEEK":
                mainStart = startWeek;
                break;
            case "MONTH":
                mainStart = startMonth;
                break;
            case "YEAR":
                mainStart = startYear;
                break;
            case "ALL":
            default:
                mainStart = 0L;
                break;
        }

        // 3. Fetch Checkins
        // Fetch checkins for the max range (YEAR) to calculate summary stats
        List<Checkin> allCheckins = checkinRepository.findByUserIdInAndTimestampBetween(studentIds, startYear, end);

        // Filter valid checkins
        List<Checkin> validCheckins = allCheckins.stream()
                .filter(c -> c.getTimestamp() != null)
                .collect(Collectors.toList());

        // 4. Aggregate Feedback Summary (Week, Month, Year)
        long weekLikes = 0, weekDislikes = 0;
        long monthLikes = 0, monthDislikes = 0;
        long yearLikes = 0, yearDislikes = 0;

        for (Checkin c : validCheckins) {
            if (c.getFeedback() == null)
                continue;

            String fb = c.getFeedback().toUpperCase();
            boolean isLike = fb.contains("LIKE") && !fb.contains("DISLIKE"); // Simple heuristic or exact match?
            if (fb.equals("LIKE"))
                isLike = true; // Exact match preference
            boolean isDislike = fb.equals("DISLIKE");

            long ts = c.getTimestamp();

            if (ts >= startWeek) {
                if (isLike)
                    weekLikes++;
                if (isDislike)
                    weekDislikes++;
            }
            if (ts >= startMonth) {
                if (isLike)
                    monthLikes++;
                if (isDislike)
                    monthDislikes++;
            }
            // Year (all fetched are within year/limit)
            if (isLike)
                yearLikes++;
            if (isDislike)
                yearDislikes++;
        }

        InsightResponse.PeriodStats weekStats = new InsightResponse.PeriodStats(weekLikes, weekDislikes);
        InsightResponse.PeriodStats monthStats = new InsightResponse.PeriodStats(monthLikes, monthDislikes);
        InsightResponse.PeriodStats yearStats = new InsightResponse.PeriodStats(yearLikes, yearDislikes);
        InsightResponse.FeedbackSummary feedbackSummary = new InsightResponse.FeedbackSummary(weekStats, monthStats,
                yearStats);

        // 5. Aggregate View Data (Day, Hour, Top) based on requested 'period'
        List<Checkin> periodCheckins = validCheckins.stream()
                .filter(c -> c.getTimestamp() >= mainStart)
                .collect(Collectors.toList());

        // A. Day Distribution
        Map<String, Long> dayDist = new LinkedHashMap<>();
        String[] daysOrder = { "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado",
                "domingo" };
        for (String day : daysOrder)
            dayDist.put(day, 0L);

        // B. Hour Distribution
        Map<Integer, Long> hourDist = new TreeMap<>();
        for (int i = 0; i < 24; i++)
            hourDist.put(i, 0L);

        // C. Top Workouts & Students
        Map<Long, Long> workoutCounts = new HashMap<>();
        Map<String, Long> studentCounts = new HashMap<>();

        // Helper cache for personal names
        Map<Long, String> personalNames = new HashMap<>();

        // D. Feedback Details List (for the requested period)
        List<InsightResponse.FeedbackDetailDTO> feedbackDetails = new ArrayList<>();

        for (Checkin c : periodCheckins) {
            LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getTimestamp()),
                    ZoneId.systemDefault());

            // Day
            String dayName = date.getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, new Locale.Builder().setLanguage("pt").setRegion("BR").build())
                    .toLowerCase();
            dayDist.put(dayName, dayDist.getOrDefault(dayName, 0L) + 1);

            // Hour
            int hour = date.getHour();
            hourDist.put(hour, hourDist.getOrDefault(hour, 0L) + 1);

            // Counts
            if (c.getTrainingId() != null)
                workoutCounts.put(c.getTrainingId(), workoutCounts.getOrDefault(c.getTrainingId(), 0L) + 1);
            if (c.getUserId() != null)
                studentCounts.put(c.getUserId(), studentCounts.getOrDefault(c.getUserId(), 0L) + 1);

            // Feedback Detail
            if (c.getFeedback() != null) {
                Usuario student = studentMap.get(c.getUserId());
                String studentName = student != null ? student.getNome() : "Desconhecido";

                // Resolve Professor Name
                String professorName = "N/A";
                if (student != null && student.getPersonalId() != null) {
                    Long pid = student.getPersonalId();
                    if (!personalNames.containsKey(pid)) {
                        Optional<Usuario> p = usuarioRepository.findById(pid);
                        personalNames.put(pid, p.isPresent() ? p.get().getNome() : "Desconhecido");
                    }
                    professorName = personalNames.get(pid);
                }

                // Resolve Workout Name
                String workoutName = "Treino Removido";
                if (c.getTrainingId() != null) {
                    Optional<StructuredWorkoutPlan> v2 = structuredWorkoutPlanRepository.findById(c.getTrainingId());
                    if (v2.isPresent()) {
                        workoutName = v2.get().getTitle();
                    } else {
                        Optional<Treino> v1 = treinoRepository.findById(c.getTrainingId());
                        if (v1.isPresent()) {
                            workoutName = v1.get().getGoal() != null ? v1.get().getGoal() : "Treino Antigo";
                        }
                    }
                }

                feedbackDetails.add(new InsightResponse.FeedbackDetailDTO(
                        studentName,
                        workoutName,
                        professorName,
                        c.getFeedback(),
                        c.getTimestamp()));
            }
        }

        // Sort feedback details by date desc
        feedbackDetails.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));

        // 6. Build Top Lists
        List<TopWorkoutDTO> topWorkouts = workoutCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .map(entry -> {
                    String name = "Treino #" + entry.getKey();
                    Optional<StructuredWorkoutPlan> v2 = structuredWorkoutPlanRepository.findById(entry.getKey());
                    if (v2.isPresent()) {
                        name = v2.get().getTitle();
                    } else {
                        Optional<Treino> v1 = treinoRepository.findById(entry.getKey());
                        if (v1.isPresent()) {
                            name = v1.get().getGoal() != null ? v1.get().getGoal() : "Treino Antigo";
                        }
                    }
                    return new TopWorkoutDTO(name, entry.getValue());
                })
                .collect(Collectors.toList());

        List<TopStudentDTO> topStudents = studentCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .map(entry -> {
                    Usuario u = studentMap.get(entry.getKey());
                    String name = u != null ? u.getNome() : "Desconhecido";
                    String avatar = u != null ? u.getAvatar() : null;
                    return new TopStudentDTO(entry.getKey(), name, avatar, entry.getValue());
                })
                .collect(Collectors.toList());

        return new InsightResponse(dayDist, hourDist, topWorkouts, topStudents, feedbackSummary, feedbackDetails);
    }
}
