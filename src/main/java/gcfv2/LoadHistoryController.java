package gcfv2;

import gcfv2.dto.LoadHistoryResponse;
import gcfv2.dto.LoadHistoryResponse.LoadHistoryEntry;
import gcfv2.dto.LoadHistoryResponse.ProgressionSuggestion;
import gcfv2.dto.LastUsedLoadsResponse;
import gcfv2.dto.LastUsedLoadsResponse.LoadEntry;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@Controller("/api/v2/exercises")
@CrossOrigin("https://fitai-analyzer-732767853162.us-west1.run.app")
public class LoadHistoryController {

    private final ExerciseExecutionRepository exerciseExecutionRepository;
    private final WorkoutExecutionRepository workoutExecutionRepository;
    private final PermissionService permissionService;

    @Inject
    public LoadHistoryController(
        ExerciseExecutionRepository exerciseExecutionRepository,
        WorkoutExecutionRepository workoutExecutionRepository,
        PermissionService permissionService
    ) {
        this.exerciseExecutionRepository = exerciseExecutionRepository;
        this.workoutExecutionRepository = workoutExecutionRepository;
        this.permissionService = permissionService;
    }

    /**
     * GET /api/v2/exercises/{exerciseName}/load-history
     * Obter histórico de cargas de um exercício específico para progressão
     */
    @Get("/{exerciseName}/load-history")
    public HttpResponse<?> getLoadHistory(
        @PathVariable String exerciseName,
        @QueryValue Long userId,
        @QueryValue Long requesterId,
        @QueryValue String requesterRole,
        @Nullable @QueryValue Integer limit
    ) {
        try {
            // Validação de permissões
            if (!permissionService.canAccessUserData(requesterId, requesterRole, userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para acessar dados deste usuário"));
            }

            int limitValue = (limit != null && limit > 0) ? limit : 10;

            // Buscar histórico de execuções do exercício
            List<ExerciseExecution> executions = exerciseExecutionRepository.findByUserIdAndExerciseName(
                userId, exerciseName, limitValue
            );

            // Construir histórico
            List<LoadHistoryEntry> history = new ArrayList<>();
            for (ExerciseExecution exec : executions) {
                // Buscar a execução do treino para pegar o executedAt
                var workoutExecOpt = workoutExecutionRepository.findById(exec.getWorkoutExecutionId());
                if (workoutExecOpt.isPresent()) {
                    LoadHistoryEntry entry = new LoadHistoryEntry();
                    entry.setExecutionId(workoutExecOpt.get().getId());
                    entry.setExecutedAt(workoutExecOpt.get().getExecutedAt());
                    entry.setActualLoad(exec.getActualLoad());
                    entry.setSetsCompleted(exec.getSetsCompleted());
                    history.add(entry);
                }
            }

            // Gerar sugestão de progressão
            ProgressionSuggestion suggestion = generateProgressionSuggestion(history, exerciseName);

            // Montar resposta
            LoadHistoryResponse response = new LoadHistoryResponse();
            response.setExerciseName(exerciseName);
            response.setHistory(history);
            response.setProgressionSuggestion(suggestion);

            return HttpResponse.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                "error", "Erro ao buscar histórico de cargas: " + e.getMessage()
            ));
        }
    }

    /**
     * Gera sugestão de progressão baseada no histórico
     * Lógica simples: se completou todas as séries nas últimas 2 sessões com a mesma carga,
     * sugere aumento de 5-10%
     */
    private ProgressionSuggestion generateProgressionSuggestion(List<LoadHistoryEntry> history, String exerciseName) {
        ProgressionSuggestion suggestion = new ProgressionSuggestion();

        if (history.isEmpty()) {
            suggestion.setCurrentLoad("Nenhuma execução registrada");
            suggestion.setNextSuggestedLoad("Comece com carga leve");
            suggestion.setReason("Primeiro treino deste exercício. Foque na técnica.");
            return suggestion;
        }

        // Pegar as duas últimas execuções
        LoadHistoryEntry latest = history.get(0);
        suggestion.setCurrentLoad(latest.getActualLoad() != null ? latest.getActualLoad() : "Não registrada");

        if (history.size() < 2) {
            suggestion.setNextSuggestedLoad(latest.getActualLoad());
            suggestion.setReason("Continue com a mesma carga e foque na técnica.");
            return suggestion;
        }

        LoadHistoryEntry previous = history.get(1);

        // Verificar se completou todas as séries nas últimas 2 sessões
        boolean completedAllSetsInBoth = (latest.getSetsCompleted() != null && latest.getSetsCompleted() >= 3) &&
                                         (previous.getSetsCompleted() != null && previous.getSetsCompleted() >= 3);

        // Verificar se a carga foi a mesma
        boolean samLoad = latest.getActualLoad() != null &&
                           latest.getActualLoad().equals(previous.getActualLoad());

        if (completedAllSetsInBoth && samLoad) {
            // Sugerir aumento
            String nextLoad = suggestLoadIncrease(latest.getActualLoad());
            suggestion.setNextSuggestedLoad(nextLoad);
            suggestion.setReason(String.format(
                "Você completou %d séries com %s nas últimas 2 sessões. Tente aumentar a carga!",
                latest.getSetsCompleted(), latest.getActualLoad()
            ));
        } else {
            // Manter carga
            suggestion.setNextSuggestedLoad(latest.getActualLoad());
            suggestion.setReason("Continue praticando com a carga atual para dominar a técnica.");
        }

        return suggestion;
    }

    /**
     * Sugere aumento de carga (lógica simplificada)
     * Tenta extrair número da string e aumentar 5-10%
     */
    private String suggestLoadIncrease(String currentLoad) {
        if (currentLoad == null || currentLoad.isEmpty()) {
            return "Aumente gradualmente";
        }

        // Verificar se é peso corporal
        if (currentLoad.toLowerCase().contains("corporal") ||
            currentLoad.toLowerCase().contains("corpo") ||
            currentLoad.toLowerCase().contains("bodyweight")) {
            return "Adicione lastro ou tente variação mais difícil";
        }

        // Tentar extrair número da carga
        try {
            String numberPart = currentLoad.replaceAll("[^0-9.]", "");
            if (!numberPart.isEmpty()) {
                double currentWeight = Double.parseDouble(numberPart);
                double suggestedWeight;

                // Aumentar 2.5kg se < 20kg, senão 5kg
                if (currentWeight < 20) {
                    suggestedWeight = currentWeight + 2.5;
                } else {
                    suggestedWeight = currentWeight + 5;
                }

                // Manter o resto da string (unidade)
                String unit = currentLoad.replaceAll("[0-9.]", "").trim();
                return String.format("%.1f%s", suggestedWeight, unit.isEmpty() ? "kg" : unit);
            }
        } catch (NumberFormatException e) {
            // Se não conseguir parsear, retornar mensagem genérica
        }

        return "Aumente gradualmente a carga";
    }

    /**
     * GET /api/v2/exercises/last-loads
     * Obter as últimas cargas utilizadas em todos os exercícios de um usuário
     * Útil para pré-popular os campos de carga ao iniciar um novo treino
     */
    @Get("/last-loads")
    public HttpResponse<?> getLastUsedLoads(
        @QueryValue Long userId,
        @QueryValue Long requesterId,
        @QueryValue String requesterRole
    ) {
        try {
            // Validação de permissões
            if (!permissionService.canAccessUserData(requesterId, requesterRole, userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você não tem permissão para acessar dados deste usuário"));
            }

            // Buscar últimas cargas utilizadas
            List<Object[]> results = exerciseExecutionRepository.findLastUsedLoadsByUserId(userId);

            // Converter para mapa exercício -> última carga
            Map<String, LoadEntry> loads = new HashMap<>();
            for (Object[] row : results) {
                String exerciseName = (String) row[0];
                String actualLoad = (String) row[1];
                Long executedAt = row[2] != null ? ((Number) row[2]).longValue() : null;

                loads.put(exerciseName, new LoadEntry(actualLoad, executedAt));
            }

            LastUsedLoadsResponse response = new LastUsedLoadsResponse(loads);
            return HttpResponse.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                "error", "Erro ao buscar últimas cargas: " + e.getMessage()
            ));
        }
    }
}
