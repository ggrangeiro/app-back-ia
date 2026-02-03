package gcfv2.evo;

import gcfv2.StructuredTreino;
import gcfv2.Usuario;
import gcfv2.dto.evo.EvoWorkoutDTO;
import gcfv2.dto.evo.EvoExerciseDTO;
import io.micronaut.json.JsonMapper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Mapper para converter treinos FitAI → formato EVO Academia.
 * Converte a estrutura JSON de daysData em EvoWorkoutDTO.
 */
@Singleton
public class EvoWorkoutMapper {

    private static final Logger LOG = LoggerFactory.getLogger(EvoWorkoutMapper.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final EvoExerciseMappingRepository exerciseMappingRepository;
    private final JsonMapper jsonMapper;

    public EvoWorkoutMapper(EvoExerciseMappingRepository exerciseMappingRepository, JsonMapper jsonMapper) {
        this.exerciseMappingRepository = exerciseMappingRepository;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Converte um treino estruturado FitAI em lista de DTOs EVO.
     * Cada dia de treino vira um EvoWorkoutDTO separado.
     *
     * @param treino Treino estruturado do FitAI
     * @param usuario Usuário alvo (deve ter evoMemberId)
     * @param ownerId ID do owner para buscar mapeamentos de exercícios
     * @return Lista de treinos EVO (um por dia)
     */
    public List<EvoWorkoutDTO> toEvoWorkouts(StructuredTreino treino, Usuario usuario, Long ownerId) {
        List<EvoWorkoutDTO> workouts = new ArrayList<>();
        
        if (treino.getDaysData() == null || treino.getDaysData().isEmpty()) {
            LOG.warn("Treino {} não tem daysData para exportar", treino.getId());
            return workouts;
        }
        
        if (usuario.getEvoMemberId() == null) {
            LOG.warn("Usuário {} não tem evoMemberId configurado", usuario.getId());
            return workouts;
        }
        
        Long evoMemberId;
        try {
            evoMemberId = Long.parseLong(usuario.getEvoMemberId());
        } catch (NumberFormatException e) {
            LOG.error("evoMemberId inválido para usuário {}: {}", usuario.getId(), usuario.getEvoMemberId());
            return workouts;
        }
        
        try {
            // Parse do JSON daysData
            List<Map<String, Object>> days = parseDaysData(treino.getDaysData());
            
            int dayIndex = 0;
            for (Map<String, Object> day : days) {
                dayIndex++;
                EvoWorkoutDTO workout = convertDayToWorkout(day, treino, evoMemberId, ownerId, dayIndex);
                if (workout != null && !workout.getExercises().isEmpty()) {
                    workouts.add(workout);
                }
            }
            
        } catch (Exception e) {
            LOG.error("Erro ao converter treino {} para formato EVO: {}", treino.getId(), e.getMessage());
        }
        
        return workouts;
    }

    /**
     * Converte um dia de treino específico em EvoWorkoutDTO.
     */
    private EvoWorkoutDTO convertDayToWorkout(Map<String, Object> day, StructuredTreino treino, 
                                               Long evoMemberId, Long ownerId, int dayIndex) {
        EvoWorkoutDTO workout = new EvoWorkoutDTO();
        workout.setIdMember(evoMemberId);
        
        // Nome do treino
        String dayLabel = getStringValue(day, "dayLabel", "Dia " + dayIndex);
        String trainingType = getStringValue(day, "trainingType", "Treino " + dayIndex);
        workout.setWorkoutName(trainingType + " - " + dayLabel);
        workout.setTrainingType(trainingType);
        
        // Dia da semana
        String dayOfWeek = getStringValue(day, "dayOfWeek", null);
        if (dayOfWeek != null) {
            workout.setDayOfWeek(dayOfWeek.toUpperCase());
        }
        
        // Descrição/objetivo
        workout.setDescription(treino.getGoal() != null ? treino.getGoal() : "Treino gerado via FitAI");
        
        // Data início (hoje)
        workout.setStartDate(LocalDate.now().format(DATE_FORMAT));
        
        // Processar exercícios
        Object exercisesObj = day.get("exercises");
        if (exercisesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> exercises = (List<Map<String, Object>>) exercisesObj;
            
            int order = 0;
            for (Map<String, Object> ex : exercises) {
                order++;
                EvoExerciseDTO evoEx = convertExercise(ex, ownerId, order);
                if (evoEx != null) {
                    workout.addExercise(evoEx);
                }
            }
        }
        
        return workout;
    }

    /**
     * Converte um exercício FitAI em EvoExerciseDTO.
     */
    private EvoExerciseDTO convertExercise(Map<String, Object> exercise, Long ownerId, int order) {
        EvoExerciseDTO evoEx = new EvoExerciseDTO();
        evoEx.setOrder(order);
        
        // Nome do exercício
        String name = getStringValue(exercise, "name", null);
        if (name == null || name.isEmpty()) {
            return null;
        }
        evoEx.setName(name);
        
        // Tentar encontrar ID do exercício no EVO via mapeamento
        Long evoExerciseId = findEvoExerciseId(name, ownerId);
        if (evoExerciseId != null) {
            evoEx.setIdExercise(evoExerciseId);
        } else {
            // Se não encontrar mapeamento, usa ID genérico (0 = exercício não mapeado)
            // O EVO pode aceitar criação de exercício ou rejeitar
            evoEx.setIdExercise(0L);
            LOG.debug("Exercício '{}' não tem mapeamento EVO configurado", name);
        }
        
        // Séries
        Object setsObj = exercise.get("sets");
        if (setsObj instanceof Number) {
            evoEx.setSeries(((Number) setsObj).intValue());
        } else if (setsObj instanceof String) {
            try {
                evoEx.setSeries(Integer.parseInt((String) setsObj));
            } catch (NumberFormatException e) {
                evoEx.setSeries(3); // Default
            }
        } else {
            evoEx.setSeries(3);
        }
        
        // Repetições
        String reps = getStringValue(exercise, "reps", "10-12");
        evoEx.setRepetitions(reps);
        
        // Descanso
        String rest = getStringValue(exercise, "rest", "60s");
        evoEx.setRestIntervalSeconds(parseRestSeconds(rest));
        
        // Observações/técnica
        String technique = getStringValue(exercise, "technique", null);
        String notes = getStringValue(exercise, "notes", null);
        StringBuilder obs = new StringBuilder();
        if (technique != null) obs.append(technique);
        if (notes != null) {
            if (obs.length() > 0) obs.append(". ");
            obs.append(notes);
        }
        if (obs.length() > 0) {
            evoEx.setObservation(obs.toString());
        }
        
        // Grupo muscular
        String muscleGroup = getStringValue(exercise, "muscleGroup", null);
        if (muscleGroup != null) {
            evoEx.setMuscleGroup(muscleGroup);
        }
        
        // Carga (se disponível)
        String load = getStringValue(exercise, "load", null);
        if (load != null) {
            evoEx.setLoad(load);
        }
        
        return evoEx;
    }

    /**
     * Busca ID do exercício no EVO via mapeamento configurado.
     */
    private Long findEvoExerciseId(String exerciseName, Long ownerId) {
        String normalizedName = EvoExerciseMapping.normalizeName(exerciseName);
        
        Optional<EvoExerciseMapping> mapping = exerciseMappingRepository
                .findByUserIdAndFitaiExerciseName(ownerId, normalizedName);
        
        return mapping.map(EvoExerciseMapping::getEvoExerciseId).orElse(null);
    }

    /**
     * Parse do JSON daysData.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseDaysData(String daysData) throws Exception {
        Object parsed = jsonMapper.readValue(daysData, Object.class);
        
        if (parsed instanceof List) {
            return (List<Map<String, Object>>) parsed;
        } else if (parsed instanceof Map) {
            // Se for um objeto único, wrap em lista
            return Collections.singletonList((Map<String, Object>) parsed);
        }
        
        return new ArrayList<>();
    }

    /**
     * Helper para extrair valor String de um Map.
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        if (val == null) return defaultValue;
        return val.toString();
    }

    /**
     * Converte string de descanso para segundos.
     * Ex: "60s" → 60, "1:30" → 90, "90" → 90
     */
    private int parseRestSeconds(String rest) {
        if (rest == null || rest.isEmpty()) return 60;
        
        rest = rest.toLowerCase().trim();
        
        // Remove "s" ou "seg" ou "segundos"
        rest = rest.replaceAll("(seg|segundos|s)$", "").trim();
        
        // Formato "mm:ss"
        if (rest.contains(":")) {
            String[] parts = rest.split(":");
            try {
                int min = Integer.parseInt(parts[0]);
                int sec = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                return min * 60 + sec;
            } catch (NumberFormatException e) {
                return 60;
            }
        }
        
        // Número direto
        try {
            return Integer.parseInt(rest);
        } catch (NumberFormatException e) {
            return 60;
        }
    }

    /**
     * Resultado do mapeamento de treino.
     */
    public static class WorkoutMappingResult {
        private final int totalExercises;
        private final int mappedExercises;
        private final int unmappedExercises;
        private final List<String> unmappedNames;

        public WorkoutMappingResult(int total, int mapped, List<String> unmappedNames) {
            this.totalExercises = total;
            this.mappedExercises = mapped;
            this.unmappedExercises = total - mapped;
            this.unmappedNames = unmappedNames;
        }

        public int getTotalExercises() { return totalExercises; }
        public int getMappedExercises() { return mappedExercises; }
        public int getUnmappedExercises() { return unmappedExercises; }
        public List<String> getUnmappedNames() { return unmappedNames; }
        public boolean hasUnmapped() { return unmappedExercises > 0; }
    }

    /**
     * Analisa quais exercícios ainda precisam de mapeamento.
     */
    public WorkoutMappingResult analyzeMappingStatus(StructuredTreino treino, Long ownerId) {
        int total = 0;
        int mapped = 0;
        List<String> unmapped = new ArrayList<>();
        
        try {
            List<Map<String, Object>> days = parseDaysData(treino.getDaysData());
            
            for (Map<String, Object> day : days) {
                Object exercisesObj = day.get("exercises");
                if (exercisesObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> exercises = (List<Map<String, Object>>) exercisesObj;
                    
                    for (Map<String, Object> ex : exercises) {
                        String name = getStringValue(ex, "name", null);
                        if (name != null && !name.isEmpty()) {
                            total++;
                            if (findEvoExerciseId(name, ownerId) != null) {
                                mapped++;
                            } else {
                                unmapped.add(name);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Erro ao analisar mapeamento: {}", e.getMessage());
        }
        
        return new WorkoutMappingResult(total, mapped, unmapped);
    }
}
