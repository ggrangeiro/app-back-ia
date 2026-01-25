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
        // The professorId can be a PERSONAL (Manager) or a PROFESSOR.
        // We need to fetch students associated with this ID.
        // Existing logic in UsuarioRepository suggests finding by PersonalId or
        // ManagerId.
        // Let's assume we want ALL students in the hierarchy if possible, or just
        // direct students.
        // For simplicity and safety matching the current "Minha Equipe" view context:
        // If the requester is a Personal, we might pass a specific 'professorId' to
        // filter, or 'all'.
        // The Controller will handle the decision of WHICH ID to pass here.
        // Here we assume 'professorId' is the ID whose students we want to analyze.

        // Find students where personal_id = professorId (works for Personal and
        // Professor as they are "personal_id" for students?
        // No, Professor is usually linked via other means or the student has
        // personal_id = Personal, and acts as Professor.
        // Actually, looking at UsuarioRepository: students have `personal_id`.
        // If `professorId` is a Personal, this gets all his students.
        // If `professorId` is a "Professor" role, check logic: usually students still
        // point to Personal, but might be assigned?
        // Let's rely on `usuarioRepository.findByPersonalId` for now as a base.
        // Ideally, we should reuse `getUsers` logic from Controller but that's in
        // Controller.
        // Let's assume the ID passed is the one in `personal_id` column of `usuario`
        // table.

        List<Usuario> students = usuarioRepository.findByPersonalId(professorId);
        if (students.isEmpty()) {
            return new InsightResponse(new HashMap<>(), new HashMap<>(), new ArrayList<>(), new ArrayList<>());
        }

        List<String> studentIds = students.stream().map(u -> String.valueOf(u.getId())).collect(Collectors.toList());
        Map<String, Usuario> studentMap = students.stream()
                .collect(Collectors.toMap(u -> String.valueOf(u.getId()), Function.identity()));

        // 2. Determine Time Range
        Long end = System.currentTimeMillis();
        Long start;

        LocalDateTime now = LocalDateTime.now();
        switch (period.toUpperCase()) {
            case "WEEK":
                start = now.minusWeeks(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                break;
            case "MONTH":
                start = now.minusMonths(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                break;
            case "YEAR":
                start = now.minusYears(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                break;
            case "ALL":
            default:
                start = 0L;
                break;
        }

        // 3. Fetch Checkins
        List<Checkin> checkins;
        if (start == 0L) {
            checkins = checkinRepository.findByUserIdIn(studentIds);
        } else {
            checkins = checkinRepository.findByUserIdInAndTimestampBetween(studentIds, start, end);
        }

        // 4. Aggregate Data

        // A. Day Distribution
        Map<String, Long> dayDist = new LinkedHashMap<>();
        // Initialize days order
        String[] daysOrder = { "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado",
                "domingo" };
        for (String day : daysOrder)
            dayDist.put(day, 0L);

        // B. Hour Distribution
        Map<Integer, Long> hourDist = new TreeMap<>();
        for (int i = 0; i < 24; i++)
            hourDist.put(i, 0L);

        // C. Top Workouts (Count by ID)
        Map<Long, Long> workoutCounts = new HashMap<>();

        // D. Top Students (Count by User ID)
        Map<String, Long> studentCounts = new HashMap<>();

        for (Checkin c : checkins) {
            if (c.getTimestamp() == null)
                continue;

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

            // Workout
            if (c.getTrainingId() != null) {
                workoutCounts.put(c.getTrainingId(), workoutCounts.getOrDefault(c.getTrainingId(), 0L) + 1);
            }

            // Student
            if (c.getUserId() != null) {
                studentCounts.put(c.getUserId(), studentCounts.getOrDefault(c.getUserId(), 0L) + 1);
            }
        }

        // 5. Build DTOs

        // Top Workouts List
        List<TopWorkoutDTO> topWorkouts = workoutCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Descending
                .limit(5)
                .map(entry -> {
                    String name = "Treino #" + entry.getKey();
                    // Resolve Name
                    // Try Structured (V2)
                    Optional<StructuredWorkoutPlan> v2 = structuredWorkoutPlanRepository.findById(entry.getKey());
                    if (v2.isPresent()) {
                        name = v2.get().getTitle();
                    } else {
                        // Try Legacy (V1)
                        Optional<Treino> v1 = treinoRepository.findById(entry.getKey());
                        if (v1.isPresent()) {
                            name = v1.get().getGoal() != null ? v1.get().getGoal() : "Treino Antigo";
                        }
                    }
                    return new TopWorkoutDTO(name, entry.getValue());
                })
                .collect(Collectors.toList());

        // Top Students List
        List<TopStudentDTO> topStudents = studentCounts.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // Descending
                .limit(10)
                .map(entry -> {
                    Usuario u = studentMap.get(entry.getKey());
                    String name = u != null ? u.getNome() : "Desconhecido";
                    String avatar = u != null ? u.getAvatar() : null;
                    return new TopStudentDTO(entry.getKey(), name, avatar, entry.getValue());
                })
                .collect(Collectors.toList());

        return new InsightResponse(dayDist, hourDist, topWorkouts, topStudents);
    }
}
