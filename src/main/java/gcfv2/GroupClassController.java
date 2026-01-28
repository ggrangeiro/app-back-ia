package gcfv2;

import gcfv2.model.ClassBooking;
import gcfv2.model.GroupClass;
import gcfv2.repository.ClassBookingRepository;
import gcfv2.repository.GroupClassRepository;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller("/api/classes")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app" })
public class GroupClassController {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GroupClassController.class);

    @Inject
    private GroupClassRepository groupClassRepository;

    @Inject
    private ClassBookingRepository classBookingRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    /**
     * CREATE A NEW CLASS
     * Roles: ADMIN, PERSONAL, PROFESSOR
     */
    @Post
    @Transactional
    public HttpResponse<?> createClass(@Body Map<String, Object> body,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        boolean canCreate = "ADMIN".equalsIgnoreCase(requesterRole) ||
                "PERSONAL".equalsIgnoreCase(requesterRole) ||
                "PROFESSOR".equalsIgnoreCase(requesterRole);

        if (!canCreate) {
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Apenas Professores e Personais podem criar aulas."));
        }

        try {
            String name = (String) body.get("name");
            String description = (String) body.get("description");
            String startTimeStr = (String) body.get("startTime"); // ISO 8601
            Integer duration = ((Number) body.get("durationMinutes")).intValue();
            Integer capacity = ((Number) body.get("capacity")).intValue();
            String location = (String) body.get("location");
            String photoUrl = (String) body.get("photoUrl");
            Boolean isRecurrent = body.containsKey("isRecurrent") ? (Boolean) body.get("isRecurrent") : false;
            String recurrenceDays = (String) body.get("recurrenceDays");

            if (name == null || startTimeStr == null || duration == null || capacity == null) {
                return HttpResponse.badRequest(
                        Map.of("message", "Campos obrigatórios: name, startTime, durationMinutes, capacity"));
            }

            LocalDateTime startTime = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ISO_DATE_TIME);

            GroupClass groupClass = new GroupClass();
            groupClass.setName(name);
            groupClass.setDescription(description);
            groupClass.setProfessorId(requesterId); // The creator is the professor
            groupClass.setStartTime(startTime);
            groupClass.setDurationMinutes(duration);
            groupClass.setCapacity(capacity);
            groupClass.setLocation(location);
            groupClass.setPhotoUrl(photoUrl);
            groupClass.setIsRecurrent(isRecurrent);
            groupClass.setRecurrenceDays(recurrenceDays);
            groupClass.setCreatedAt(LocalDateTime.now());

            groupClassRepository.save(groupClass);

            return HttpResponse.created(groupClass);

        } catch (Exception e) {
            LOG.error("Error creating class", e);
            return HttpResponse.serverError(Map.of("message", "Erro ao criar aula: " + e.getMessage()));
        }
    }

    /**
     * LIST CLASSES AUTHORIZED BY PROFESSOR
     */
    @Get("/professor/{id}")
    public HttpResponse<?> getProfessorClasses(@PathVariable Long id) {
        List<GroupClass> classes = groupClassRepository.findByProfessorId(id);

        // Enhance with booking counts
        List<Map<String, Object>> result = classes.stream().map(c -> {
            long bookingCount = classBookingRepository.countByClassIdAndStatus(c.getId(), "CONFIRMED");
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("startTime", c.getStartTime());
            map.put("bookings", bookingCount);
            map.put("capacity", c.getCapacity());
            map.put("location", c.getLocation() != null ? c.getLocation() : "");
            map.put("isRecurrent", c.getIsRecurrent());
            return map;
        }).collect(Collectors.toList());

        return HttpResponse.ok(result);
    }

    /**
     * LIST AVAILABLE CLASSES (FUTURE)
     * For Students
     */
    @Get("/available")
    public HttpResponse<?> getAvailableClasses(@QueryValue(defaultValue = "false") boolean includeFull,
            @QueryValue Optional<Long> requesterId) {

        List<GroupClass> classes;

        if (requesterId.isPresent()) {
            Optional<Usuario> studentOpt = usuarioRepository.findById(requesterId.get());

            if (studentOpt.isPresent()) {
                Usuario student = studentOpt.get();
                LOG.info("Requesting available classes for Student ID: {}, Personal ID: {}", student.getId(),
                        student.getPersonalId());

                if (student.getPersonalId() != null) {
                    LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
                    LOG.info("Filtering classes for Personal ID: {} after time: {}", student.getPersonalId(), now);
                    classes = groupClassRepository.findByProfessorIdAndStartTimeAfterOrderByStartTimeAsc(
                            student.getPersonalId(), now);
                } else {
                    LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
                    LOG.warn("Student has no Personal ID. Showing all classes.");
                    classes = groupClassRepository.findByStartTimeAfterOrderByStartTimeAsc(now);
                }
            } else {
                LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
                LOG.warn("Student not found for ID: {}. Showing all classes.", requesterId.get());
                classes = groupClassRepository.findByStartTimeAfterOrderByStartTimeAsc(now);
            }
        } else {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("America/Sao_Paulo"));
            classes = groupClassRepository.findByStartTimeAfterOrderByStartTimeAsc(now);
        }

        // Transform and filter full if needed (simple implementation)
        List<Map<String, Object>> result = classes.stream().map(c -> {
            long bookingCount = classBookingRepository.countByClassIdAndStatus(c.getId(), "CONFIRMED");
            Optional<Usuario> prof = usuarioRepository.findById(c.getProfessorId());
            String profName = prof.map(Usuario::getNome).orElse("Instrutor");

            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", c.getId());
            map.put("name", c.getName());
            map.put("professorName", profName);
            map.put("startTime", c.getStartTime());
            map.put("durationMinutes", c.getDurationMinutes());
            map.put("capacity", c.getCapacity());
            map.put("bookings", bookingCount);
            map.put("location", c.getLocation() != null ? c.getLocation() : "");
            map.put("photoUrl", c.getPhotoUrl() != null ? c.getPhotoUrl() : "");
            map.put("full", bookingCount >= c.getCapacity());
            return map;
        }).filter(m -> includeFull || !((Boolean) m.get("full"))).collect(Collectors.toList());

        return HttpResponse.ok(result);
    }

    /**
     * BOOK A CLASS
     */
    @Post("/{id}/book")
    @Transactional
    public HttpResponse<?> bookClass(@PathVariable Long id,
            @Body Map<String, Long> body, // Expecting { "studentId": 123 }
            @QueryValue Long requesterId) {

        Long studentId = body.get("studentId");
        if (studentId == null)
            studentId = requesterId; // Fallback to requester

        Optional<GroupClass> classOpt = groupClassRepository.findById(id);
        if (classOpt.isEmpty())
            return HttpResponse.notFound();
        GroupClass groupClass = classOpt.get();

        // Check Capacity
        long currentBookings = classBookingRepository.countByClassIdAndStatus(id, "CONFIRMED");
        if (currentBookings >= groupClass.getCapacity()) {
            return HttpResponse.badRequest(Map.of("message", "Aula lotada."));
        }

        // Check availability (double booking same user)
        Optional<ClassBooking> existing = classBookingRepository.findByClassIdAndStudentId(id, studentId);
        if (existing.isPresent() && "CONFIRMED".equals(existing.get().getStatus())) {
            return HttpResponse.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Você já está agendado para esta aula."));
        }

        ClassBooking booking = existing.orElse(new ClassBooking());
        booking.setClassId(id);
        booking.setStudentId(studentId);
        booking.setStatus("CONFIRMED");
        booking.setCreatedAt(LocalDateTime.now());

        classBookingRepository.save(booking);

        return HttpResponse.created(Map.of("message", "Agendamento confirmado!"));
    }

    /**
     * CANCEL BOOKING
     */
    @Delete("/{id}/book")
    @Transactional
    public HttpResponse<?> cancelBooking(@PathVariable Long id,
            @QueryValue Long requesterId) {

        Optional<ClassBooking> bookingOpt = classBookingRepository.findByClassIdAndStudentId(id, requesterId);
        if (bookingOpt.isEmpty()) {
            return HttpResponse.notFound(Map.of("message", "Agendamento não encontrado."));
        }

        ClassBooking booking = bookingOpt.get();
        booking.setStatus("CANCELLED");
        classBookingRepository.save(booking);
        // Or delete: classBookingRepository.delete(booking);

        return HttpResponse.ok(Map.of("message", "Agendamento cancelado."));
    }

    /**
     * CHECK MY BOOKING STATUS
     */
    @Get("/{id}/status")
    public HttpResponse<?> getBookingStatus(@PathVariable Long id, @QueryValue Long requesterId) {
        Optional<ClassBooking> bookingOpt = classBookingRepository.findByClassIdAndStudentId(id, requesterId);
        if (bookingOpt.isPresent() && "CONFIRMED".equals(bookingOpt.get().getStatus())) {
            return HttpResponse.ok(Map.of("booked", true));
        }
        return HttpResponse.ok(Map.of("booked", false));
    }
}
