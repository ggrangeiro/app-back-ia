package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpMethod;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.List;

@Controller("/api/checkins")
@CrossOrigin(allowedOrigins = { "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app" }, allowedMethods = {
                HttpMethod.GET,
                HttpMethod.POST,
                HttpMethod.OPTIONS
        })
public class CheckinController {

    @Inject
    private CheckinRepository checkinRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private TreinoRepository treinoRepository;

    @Inject
    private StructuredWorkoutPlanRepository structuredWorkoutPlanRepository;

    @Post("/")
    public HttpResponse<?> salvar(@Body Checkin checkin,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            // Regra 1: Validação de Permissões
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, checkin.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message",
                                "Acesso negado. Sem permissão para registrar check-in para este aluno."));
            }

            // Regra 2: Validar trainingId
            if (checkin.getTrainingId() == null) {
                return HttpResponse.badRequest(Map.of("message", "O trainingId é obrigatório."));
            }

            // Tenta encontrar em Treino V2 (Structured) primeiro - formato mais recente
            var treinoV2 = structuredWorkoutPlanRepository.findById(checkin.getTrainingId());
            if (treinoV2.isPresent()) {
                Long ownerId = treinoV2.get().getUserId();
                Long checkinUserId = Long.parseLong(checkin.getUserId());

                if (ownerId.equals(checkinUserId)) {
                    return saveCheckinInternal(checkin);
                }
                // Se não pertence ao usuário em V2, continua verificando V1
            }

            // Tenta encontrar em Treino V1
            var treinoV1 = treinoRepository.findById(checkin.getTrainingId());
            if (treinoV1.isPresent()) {
                if (treinoV1.get().getUserId().equals(checkin.getUserId())) {
                    return saveCheckinInternal(checkin);
                }
                // Se não pertence ao usuário em V1, continua para verificar se foi encontrado
                // em algum lugar
            }

            // Se encontrou em alguma tabela mas não pertence ao usuário
            if (treinoV2.isPresent() || treinoV1.isPresent()) {
                return HttpResponse.badRequest(Map.of("message", "O treino informado não pertence ao aluno."));
            }

            return HttpResponse.badRequest(Map.of("message", "Treino (trainingId) não encontrado em base V1 nem V2."));

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao salvar check-in: " + e.getMessage()));
        }
    }

    private HttpResponse<?> saveCheckinInternal(Checkin checkin) {
        // Garante status completed se não enviado
        if (checkin.getStatus() == null || checkin.getStatus().isEmpty()) {
            checkin.getStatus();
            checkin.setStatus("completed");
        }

        // Garante timestamp se não enviado
        if (checkin.getTimestamp() == null || checkin.getTimestamp() == 0) {
            checkin.setTimestamp(System.currentTimeMillis());
        }

        Checkin salvo = checkinRepository.save(checkin);
        return HttpResponse.created(salvo);
    }

    @Get("/{userId}")
    public HttpResponse<?> listar(@PathVariable String userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
            }

            List<Checkin> checkins = checkinRepository.findByUserIdOrderByTimestampDesc(userId);

            // Mapear para DTO com nome do treino
            List<gcfv2.dto.checkin.CheckinResponse> responseList = new java.util.ArrayList<>();

            for (Checkin c : checkins) {
                gcfv2.dto.checkin.CheckinResponse resp = new gcfv2.dto.checkin.CheckinResponse();
                // Copiar propriedades
                resp.setId(c.getId());
                resp.setUserId(c.getUserId());
                resp.setTrainingId(c.getTrainingId());
                resp.setData(c.getData());
                resp.setStatus(c.getStatus());
                resp.setComment(c.getComment());
                resp.setFeedback(c.getFeedback());
                resp.setTimestamp(c.getTimestamp());

                // Resolver nome do treino
                String workoutName = "Treino Removido ou Não Encontrado";
                if (c.getTrainingId() != null) {
                    var v2 = structuredWorkoutPlanRepository.findById(c.getTrainingId());
                    if (v2.isPresent()) {
                        workoutName = v2.get().getTitle();
                    } else {
                        var v1 = treinoRepository.findById(c.getTrainingId());
                        if (v1.isPresent()) {
                            workoutName = v1.get().getGoal(); // V1 usa goal como 'nome' muitas vezes ou type
                        }
                    }
                }
                resp.setWorkoutName(workoutName);
                responseList.add(resp);
            }

            return HttpResponse.ok(responseList);

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao listar check-ins: " + e.getMessage()));
        }
    }

    /**
     * Retorna os check-ins de uma semana específica formatados para exibição
     * visual.
     */
    @Get("/{userId}/week")
    public HttpResponse<?> getWeeklyCheckIns(
            @PathVariable String userId,
            @QueryValue(defaultValue = "") String weekStart,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
            }

            // Calcular início e fim da semana
            java.time.LocalDate startDate;
            if (weekStart == null || weekStart.isEmpty()) {
                // Usar segunda-feira da semana atual
                java.time.LocalDate today = java.time.LocalDate.now();
                startDate = today.with(java.time.DayOfWeek.MONDAY);
            } else {
                startDate = java.time.LocalDate.parse(weekStart);
            }
            java.time.LocalDate endDate = startDate.plusDays(6); // Domingo

            // Converter para timestamps (início do dia e fim do dia)
            long startTimestamp = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endTimestamp = endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant()
                    .toEpochMilli();

            // Buscar check-ins da semana
            List<Checkin> weekCheckins = checkinRepository.findByUserIdAndTimestampBetween(userId, startTimestamp,
                    endTimestamp);

            // Criar mapa de check-ins por data
            java.util.Map<String, Checkin> checkinsByDate = new java.util.HashMap<>();
            for (Checkin c : weekCheckins) {
                if (c.getTimestamp() != null) {
                    java.time.LocalDate date = java.time.Instant.ofEpochMilli(c.getTimestamp())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                    String dateStr = date.toString();
                    // Guardar apenas o primeiro check-in do dia
                    if (!checkinsByDate.containsKey(dateStr)) {
                        checkinsByDate.put(dateStr, c);
                    }
                }
            }

            // Buscar weeklyGoal do usuário
            int weeklyGoal = 5;
            try {
                Long userIdLong = Long.parseLong(userId);
                usuarioRepository.findById(userIdLong).ifPresent(u -> {
                    // Não podemos modificar weeklyGoal local, então faremos abaixo
                });
                var userOpt = usuarioRepository.findById(userIdLong);
                if (userOpt.isPresent()) {
                    Integer goal = userOpt.get().getWeeklyGoal();
                    if (goal != null) {
                        weeklyGoal = goal;
                    }
                }
            } catch (NumberFormatException ignored) {
            }

            // Labels de dias
            String[] dayOfWeekKeys = { "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday" };
            String[] dayLabels = { "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom" };

            // Construir lista de dias
            List<gcfv2.dto.checkin.DayCheckIn> days = new java.util.ArrayList<>();
            int totalCheckIns = 0;

            for (int i = 0; i < 7; i++) {
                java.time.LocalDate currentDay = startDate.plusDays(i);
                String dateStr = currentDay.toString();
                Checkin checkin = checkinsByDate.get(dateStr);

                gcfv2.dto.checkin.DayCheckIn dayCheckIn = new gcfv2.dto.checkin.DayCheckIn();
                dayCheckIn.setDayOfWeek(dayOfWeekKeys[i]);
                dayCheckIn.setDayLabel(dayLabels[i]);
                dayCheckIn.setDate(dateStr);
                dayCheckIn.setHasCheckIn(checkin != null);

                if (checkin != null) {
                    totalCheckIns++;
                    gcfv2.dto.checkin.CheckInDetail detail = new gcfv2.dto.checkin.CheckInDetail();
                    detail.setId(checkin.getId() != null ? checkin.getId().toString() : null);
                    detail.setTimestamp(checkin.getTimestamp());
                    detail.setComment(checkin.getComment());
                    detail.setFeedback(checkin.getFeedback()); // Set Feedback

                    // Resolver Workout Name para Week View
                    String workoutName = "Treino";
                    if (checkin.getTrainingId() != null) {
                        var v2 = structuredWorkoutPlanRepository.findById(checkin.getTrainingId());
                        if (v2.isPresent()) {
                            workoutName = v2.get().getTitle();
                        } else {
                            var v1 = treinoRepository.findById(checkin.getTrainingId());
                            if (v1.isPresent()) {
                                workoutName = v1.get().getGoal();
                            }
                        }
                    }
                    detail.setWorkoutName(workoutName);

                    dayCheckIn.setCheckIn(detail);
                } else {
                    dayCheckIn.setCheckIn(null);
                }

                days.add(dayCheckIn);
            }

            // Calcular weekLabel: "Semana X de Mês"
            int dayOfMonth = startDate.getDayOfMonth();
            int weekOfMonth = (dayOfMonth - 1) / 7 + 1;
            String[] monthNames = { "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro" };
            String monthName = monthNames[startDate.getMonthValue() - 1];
            String weekLabel = "Semana " + weekOfMonth + " de " + monthName;

            // Construir resposta
            gcfv2.dto.checkin.WeeklyCheckInsResponse response = new gcfv2.dto.checkin.WeeklyCheckInsResponse();
            response.setWeekStart(startDate.toString());
            response.setWeekEnd(endDate.toString());
            response.setWeekLabel(weekLabel);
            response.setWeeklyGoal(weeklyGoal);
            response.setTotalCheckIns(totalCheckIns);
            response.setDays(days);

            return HttpResponse.ok(response);

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao buscar check-ins semanais: " + e.getMessage()));
        }
    }

    /**
     * Retorna informações sobre a sequência de dias consecutivos de treino
     * (streak).
     */
    @Get("/{userId}/streak")
    public HttpResponse<?> getUserStreak(
            @PathVariable String userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
            }

            // Buscar todos os check-ins ordenados por timestamp
            List<Checkin> allCheckins = checkinRepository.findByUserId(userId);

            if (allCheckins.isEmpty()) {
                gcfv2.dto.checkin.UserStreakResponse response = new gcfv2.dto.checkin.UserStreakResponse(0, 0, null,
                        false);
                return HttpResponse.ok(response);
            }

            // Converter para conjunto de datas únicas
            java.util.Set<java.time.LocalDate> checkInDates = new java.util.TreeSet<>();
            for (Checkin c : allCheckins) {
                if (c.getTimestamp() != null) {
                    java.time.LocalDate date = java.time.Instant.ofEpochMilli(c.getTimestamp())
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate();
                    checkInDates.add(date);
                }
            }

            if (checkInDates.isEmpty()) {
                gcfv2.dto.checkin.UserStreakResponse response = new gcfv2.dto.checkin.UserStreakResponse(0, 0, null,
                        false);
                return HttpResponse.ok(response);
            }

            // Ordenar datas em ordem decrescente
            java.util.List<java.time.LocalDate> sortedDates = new java.util.ArrayList<>(checkInDates);
            sortedDates.sort(java.util.Comparator.reverseOrder());

            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate yesterday = today.minusDays(1);
            java.time.LocalDate lastCheckInDate = sortedDates.get(0);
            boolean isActiveToday = lastCheckInDate.equals(today);

            // Calcular currentStreak
            int currentStreak = 0;
            java.time.LocalDate expectedDate = isActiveToday ? today : yesterday;

            // Se o último check-in não é de hoje nem de ontem, currentStreak = 0
            if (!lastCheckInDate.equals(today) && !lastCheckInDate.equals(yesterday)) {
                currentStreak = 0;
            } else {
                for (java.time.LocalDate date : sortedDates) {
                    if (date.equals(expectedDate)) {
                        currentStreak++;
                        expectedDate = expectedDate.minusDays(1);
                    } else if (date.isBefore(expectedDate)) {
                        break;
                    }
                }
            }

            // Calcular longestStreak
            int longestStreak = 0;
            int tempStreak = 1;
            java.util.List<java.time.LocalDate> ascendingDates = new java.util.ArrayList<>(checkInDates);
            java.util.Collections.sort(ascendingDates);

            for (int i = 1; i < ascendingDates.size(); i++) {
                java.time.LocalDate prev = ascendingDates.get(i - 1);
                java.time.LocalDate curr = ascendingDates.get(i);

                if (prev.plusDays(1).equals(curr)) {
                    tempStreak++;
                } else {
                    longestStreak = Math.max(longestStreak, tempStreak);
                    tempStreak = 1;
                }
            }
            longestStreak = Math.max(longestStreak, tempStreak);

            gcfv2.dto.checkin.UserStreakResponse response = new gcfv2.dto.checkin.UserStreakResponse(
                    currentStreak,
                    longestStreak,
                    lastCheckInDate.toString(),
                    isActiveToday);

            return HttpResponse.ok(response);

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao calcular streak: " + e.getMessage()));
        }
    }
}
