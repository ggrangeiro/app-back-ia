package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import gcfv2.dto.ActivityDTO;
import gcfv2.dto.ProfessorStatsDTO;
import gcfv2.dto.ProductivitySummaryDTO;
import gcfv2.dto.ProductivitySummaryDTO.ProfessorProductivityDTO;

/**
 * Controller para gestão de atividades de professores.
 * Permite consultas e dashboard de produtividade.
 */
@Controller("/api/activities")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
                "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
                "https://fitanalizer.com.br",
                "http://localhost:3000",
                "http://localhost:5173",
                "https://app-back-ia-732767853162.southamerica-east1.run.app" })
public class ActivityController {

        @Inject
        private AtividadeProfessorRepository activityRepository;

        @Inject
        private UsuarioRepository usuarioRepository;

        /**
         * Lista atividades dos professores com filtros e paginação.
         * 
         * GET /api/activities/professors
         * Query Params:
         * - managerId (obrigatório): ID do personal
         * - professorId (opcional): Filtrar por professor específico
         * - actionType (opcional): Filtrar por tipo de ação
         * - startDate (opcional): Data inicial (ISO format, default: 30 dias atrás)
         * - endDate (opcional): Data final (ISO format, default: hoje)
         * - page (opcional): Página (default: 0)
         * - size (opcional): Tamanho da página (default: 50, max: 100)
         */
        @Get("/professors")
        public HttpResponse<?> listActivities(
                        @QueryValue Long managerId,
                        @QueryValue Long requesterId,
                        @QueryValue String requesterRole,
                        @Nullable @QueryValue Long professorId,
                        @Nullable @QueryValue String actionType,
                        @Nullable @QueryValue String startDate,
                        @Nullable @QueryValue String endDate,
                        @Nullable @QueryValue Integer page,
                        @Nullable @QueryValue Integer size) {

                // Validar permissão: apenas o personal dono ou admin
                if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
                        if (!"PERSONAL".equalsIgnoreCase(requesterRole) || !requesterId.equals(managerId)) {
                                return HttpResponse.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("message",
                                                                "Acesso negado. Você não pode visualizar essas atividades."));
                        }
                }

                // Parse de datas
                LocalDateTime start = startDate != null
                                ? LocalDate.parse(startDate).atStartOfDay()
                                : LocalDate.now().minusDays(30).atStartOfDay();
                LocalDateTime end = endDate != null
                                ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                                : LocalDate.now().atTime(LocalTime.MAX);

                // Paginação
                int pageNum = page != null ? page : 0;
                int pageSize = size != null ? Math.min(size, 100) : 50;

                try {
                        // Buscar todas atividades do manager e filtrar programaticamente
                        List<AtividadeProfessor> allActivities = activityRepository
                                        .findByManagerIdOrderByCreatedAtDesc(managerId);

                        // Aplicar filtros
                        List<AtividadeProfessor> filtered = allActivities.stream()
                                        .filter(a -> professorId == null || professorId.equals(a.getProfessorId()))
                                        .filter(a -> actionType == null
                                                        || actionType.equalsIgnoreCase(a.getActionType()))
                                        .filter(a -> a.getCreatedAt() != null &&
                                                        !a.getCreatedAt().isBefore(start) &&
                                                        !a.getCreatedAt().isAfter(end))
                                        .toList();

                        long totalElements = filtered.size();

                        // Aplicar paginação
                        int fromIndex = pageNum * pageSize;
                        int toIndex = Math.min(fromIndex + pageSize, filtered.size());
                        List<AtividadeProfessor> paged = fromIndex < filtered.size()
                                        ? filtered.subList(fromIndex, toIndex)
                                        : new ArrayList<>();

                        // Converter para DTOs com nome do professor
                        List<ActivityDTO> dtos = new ArrayList<>();
                        for (AtividadeProfessor activity : paged) {
                                ActivityDTO dto = new ActivityDTO();
                                dto.setId(activity.getId());
                                dto.setProfessorId(activity.getProfessorId());

                                // Buscar nome do professor
                                usuarioRepository.findById(activity.getProfessorId())
                                                .ifPresent(prof -> dto.setProfessorName(prof.getNome()));

                                dto.setActionType(activity.getActionType());
                                dto.setTargetUserId(activity.getTargetUserId());
                                dto.setTargetUserName(activity.getTargetUserName());
                                dto.setResourceType(activity.getResourceType());
                                dto.setResourceId(activity.getResourceId());
                                dto.setCreatedAt(activity.getCreatedAt() != null ? activity.getCreatedAt().toString()
                                                : null);

                                dtos.add(dto);
                        }

                        return HttpResponse.ok(Map.of(
                                        "activities", dtos,
                                        "pagination", Map.of(
                                                        "page", pageNum,
                                                        "size", pageSize,
                                                        "totalElements", totalElements,
                                                        "totalPages",
                                                        (int) Math.ceil((double) totalElements / pageSize))));

                } catch (Exception e) {
                        return HttpResponse.serverError(Map.of(
                                        "message", "Erro ao buscar atividades: " + e.getMessage()));
                }
        }

        /**
         * Dashboard de produtividade dos professores.
         * 
         * GET /api/activities/professors/summary
         * Query Params:
         * - managerId (obrigatório): ID do personal
         * - period (opcional): 'day' | 'week' | 'month' (default: week)
         */
        @Get("/professors/summary")
        public HttpResponse<?> getProductivitySummary(
                        @QueryValue Long managerId,
                        @QueryValue Long requesterId,
                        @QueryValue String requesterRole,
                        @Nullable @QueryValue String period) {

                // Validar permissão
                if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
                        if (!"PERSONAL".equalsIgnoreCase(requesterRole) || !requesterId.equals(managerId)) {
                                return HttpResponse.status(HttpStatus.FORBIDDEN)
                                                .body(Map.of("message", "Acesso negado."));
                        }
                }

                // Calcular período
                String periodType = period != null ? period : "week";
                LocalDateTime endDate = LocalDateTime.now();
                LocalDateTime startDate;

                switch (periodType.toLowerCase()) {
                        case "day":
                                startDate = LocalDate.now().atStartOfDay();
                                break;
                        case "month":
                                startDate = LocalDate.now().minusMonths(1).atStartOfDay();
                                break;
                        case "week":
                        default:
                                startDate = LocalDate.now().minusWeeks(1).atStartOfDay();
                                break;
                }

                try {
                        // Buscar professores do manager
                        List<Usuario> professors = usuarioRepository.findProfessorsByManagerId(managerId);

                        List<ProfessorProductivityDTO> professorStats = new ArrayList<>();
                        int totalStudents = 0, totalWorkouts = 0, totalDiets = 0, totalAnalysis = 0;

                        for (Usuario professor : professors) {
                                ProfessorProductivityDTO profDTO = new ProfessorProductivityDTO();
                                profDTO.setId(professor.getId());
                                profDTO.setName(professor.getNome());
                                profDTO.setAvatar(professor.getAvatar());

                                // Contar ações por tipo
                                int students = activityRepository.countByProfessorIdAndActionTypeAndPeriod(
                                                professor.getId(), "STUDENT_CREATED", startDate, endDate);
                                int workouts = activityRepository.countByProfessorIdAndActionTypeAndPeriod(
                                                professor.getId(), "WORKOUT_GENERATED", startDate, endDate);
                                int diets = activityRepository.countByProfessorIdAndActionTypeAndPeriod(
                                                professor.getId(), "DIET_GENERATED", startDate, endDate);
                                int analysis = activityRepository.countByProfessorIdAndActionTypeAndPeriod(
                                                professor.getId(), "ANALYSIS_PERFORMED", startDate, endDate);

                                ProfessorStatsDTO stats = new ProfessorStatsDTO(students, workouts, diets, analysis);
                                profDTO.setStats(stats);

                                // Última atividade
                                activityRepository.findLastActivityByProfessorId(professor.getId())
                                                .ifPresent(last -> profDTO.setLastActivity(
                                                                last.getCreatedAt() != null
                                                                                ? last.getCreatedAt().toString()
                                                                                : null));

                                professorStats.add(profDTO);

                                // Somar totais
                                totalStudents += students;
                                totalWorkouts += workouts;
                                totalDiets += diets;
                                totalAnalysis += analysis;
                        }

                        // Montar resposta
                        ProductivitySummaryDTO summary = new ProductivitySummaryDTO();
                        summary.setPeriod(periodType);
                        summary.setStartDate(startDate.toLocalDate().toString());
                        summary.setEndDate(endDate.toLocalDate().toString());
                        summary.setProfessors(professorStats);
                        summary.setTotals(
                                        new ProfessorStatsDTO(totalStudents, totalWorkouts, totalDiets, totalAnalysis));

                        return HttpResponse.ok(summary);

                } catch (Exception e) {
                        return HttpResponse.serverError(Map.of(
                                        "message", "Erro ao gerar resumo de produtividade: " + e.getMessage()));
                }
        }
}
