package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import io.micronaut.http.HttpMethod; // Import necessário para os métodos
import jakarta.inject.Inject;
import java.util.Map;
import java.util.List;

@Controller("/api/treinos")
@CrossOrigin(
    allowedOrigins = "https://fitai-analyzer-732767853162.us-west1.run.app",
    allowedMethods = {
        HttpMethod.GET, 
        HttpMethod.POST, 
        HttpMethod.DELETE, 
        HttpMethod.OPTIONS
    }
)
public class TreinoController {

    @Inject
    private TreinoRepository treinoRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Post("/")
    public HttpResponse<?> salvar(@Body Treino treino, 
                                 @QueryValue Long requesterId, 
                                 @QueryValue String requesterRole) {
        try {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, treino.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Sem permissão para este aluno."));
            }

            Treino salvo = treinoRepository.save(treino);
            return HttpResponse.created(salvo);
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao salvar treino: " + e.getMessage()));
        }
    }

    @Get("/{userId}")
    public HttpResponse<?> listar(@PathVariable String userId, 
                                 @QueryValue Long requesterId, 
                                 @QueryValue String requesterRole) {
        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }
        return HttpResponse.ok(treinoRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Delete("/{id}")
    @Transactional
    public HttpResponse<?> excluir(
            @PathVariable Long id, 
            @QueryValue Long requesterId, 
            @QueryValue String requesterRole) {
        
        return treinoRepository.findById(id).map(treino -> {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, treino.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não pode excluir este treino."));
            }

            treinoRepository.delete(treino);
            return HttpResponse.ok(Map.of("message", "Treino excluído com sucesso."));
            
        }).orElse(HttpResponse.notFound(Map.of("message", "Treino não encontrado.")));
    }
}