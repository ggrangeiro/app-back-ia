package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.core.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller("/api/historico")
@CrossOrigin("https://fitai-analyzer-732767853162.us-west1.run.app")
public class HistoricoController {

    private final HistoricoRepository historicoRepository;

    public HistoricoController(HistoricoRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    @Get("/{userId}")
    public HttpResponse<?> listar(
        @PathVariable String userId, 
        @Nullable @QueryValue String exercise 
    ) {
        // Cenário 1: Exercício informado (Retorna Lista simples para o gráfico)
        if (exercise != null && !exercise.isEmpty()) {
            List<Historico> lista = historicoRepository.findByUserIdAndExerciseOrderByTimestampDesc(userId, exercise);
            return HttpResponse.ok(lista);
        }

        // Cenário 2: Exercício NÃO informado (Retorna Agrupado por Exercício)
        List<Historico> todos = historicoRepository.findByUserIdOrderByTimestampDesc(userId);
        
        // Agrupa os resultados usando o campo 'exercise' como chave
        Map<String, List<Historico>> agrupado = todos.stream()
            .collect(Collectors.groupingBy(Historico::getExercise));

        return HttpResponse.ok(agrupado);
    }
    
    // ... manter os outros métodos (salvar, deletar)
}