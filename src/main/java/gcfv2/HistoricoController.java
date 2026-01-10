package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller("/api/historico")
public class HistoricoController {

    private final HistoricoRepository historicoRepository;
    private final UsuarioRepository usuarioRepository;

    @Inject
    public HistoricoController(HistoricoRepository historicoRepository, UsuarioRepository usuarioRepository) {
        this.historicoRepository = historicoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * SALVAR HISTÓRICO / AVALIAÇÃO
     * Suporta criação pelo próprio aluno ou por Personal/Admin (Multi-tenant)
     */
    @Post("/")
    public HttpResponse<?> salvar(
            @Body Historico historico,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            // Validação de Segurança: O requester tem permissão sobre o aluno
            // (targetUserId)?
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, historico.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message",
                                "Acesso negado. Você não tem permissão para registrar dados para este aluno."));
            }

            if (historico.getExercise() == null || historico.getUserId() == null) {
                return HttpResponse.badRequest(Map.of("message", "Os campos exercise (ID) e userId são obrigatórios."));
            }

            // Garante que o timestamp seja gerado se o front não enviar
            if (historico.getTimestamp() == null || historico.getTimestamp() == 0) {
                historico.setTimestamp(System.currentTimeMillis());
            }

            Historico salvo = historicoRepository.save(historico);
            return HttpResponse.created(salvo);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of(
                    "message", "Erro ao salvar histórico no banco de dados.",
                    "details", e.getMessage()));
        }
    }

    /**
     * LISTAR HISTÓRICO
     * Se passar o parâmetro 'exercise', filtra por um exercício específico (usando
     * o ID Técnico)
     */
    @Get("/{userId}")
    public HttpResponse<?> listar(
            @PathVariable String userId,
            @Nullable @QueryValue String exercise,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        try {
            // Validação de Segurança para Visualização
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Você não tem permissão para visualizar o histórico deste aluno."));
            }

            if (exercise != null && !exercise.isEmpty()) {
                // Busca filtrada pelo ID Técnico (ex: BENCH_PRESS)
                List<Historico> lista = historicoRepository.findByUserIdAndExerciseOrderByTimestampDesc(userId,
                        exercise);
                return HttpResponse.ok(lista);
            }

            // Retorna tudo agrupado por exercício para o Dashboard
            List<Historico> todos = historicoRepository.findByUserIdOrderByTimestampDesc(userId);

            Map<String, List<Historico>> agrupado = todos.stream()
                    .collect(Collectors.groupingBy(Historico::getExercise));

            return HttpResponse.ok(agrupado);

        } catch (Exception e) {
            e.printStackTrace();
            return HttpResponse.serverError(Map.of("message", "Erro ao recuperar histórico."));
        }
    }

    /**
     * DELETAR REGISTRO
     */
    @Delete("/{id}")
    public HttpResponse<?> deletar(@PathVariable Long id, @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        try {
            return historicoRepository.findById(id).map(h -> {
                // Apenas Admin ou o próprio dono podem deletar (ou o Personal do aluno)
                if (usuarioRepository.hasPermission(requesterId, requesterRole, h.getUserId())) {
                    historicoRepository.deleteById(id);
                    return HttpResponse.ok(Map.of("message", "Registro removido."));
                }
                return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Sem permissão para deletar."));
            }).orElse(HttpResponse.notFound());

        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao deletar registro."));
        }
    }
}