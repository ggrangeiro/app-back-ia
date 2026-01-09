package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpMethod; // IMPORTANTE: Para evitar erro de compilação
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.List;

@Controller("/api/dietas")
@CrossOrigin(
    allowedOrigins = "https://fitai-analyzer-732767853162.us-west1.run.app",
    allowedMethods = {
        HttpMethod.GET, 
        HttpMethod.POST, 
        HttpMethod.DELETE, 
        HttpMethod.OPTIONS
    }
)
public class DietaController {

    @Inject
    private DietaRepository dietaRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Post("/")
    public HttpResponse<?> salvar(@Body Dieta dieta, 
                                 @QueryValue Long requesterId, 
                                 @QueryValue String requesterRole) {
        try {
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, dieta.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não tem vínculo com este aluno."));
            }

            Dieta salva = dietaRepository.save(dieta);
            return HttpResponse.created(salva);
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of("message", "Erro ao salvar dieta: " + e.getMessage()));
        }
    }

    @Get("/{userId}")
    public HttpResponse<?> listar(@PathVariable String userId, 
                                 @QueryValue Long requesterId, 
                                 @QueryValue String requesterRole) {
        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId)) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }
        return HttpResponse.ok(dietaRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    /**
     * EXCLUIR DIETA
     * Adicionado para resolver a falta do endpoint de exclusão.
     */
    @Delete("/{id}")
    @Transactional
    public HttpResponse<?> excluir(
            @PathVariable Long id, 
            @QueryValue Long requesterId, 
            @QueryValue String requesterRole) {
        
        return dietaRepository.findById(id).map(dieta -> {
            // Verifica se quem está tentando excluir tem permissão sobre o dono da dieta
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, dieta.getUserId())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não pode excluir esta dieta."));
            }

            dietaRepository.delete(dieta);
            return HttpResponse.ok(Map.of("message", "Dieta excluída com sucesso."));
            
        }).orElse(HttpResponse.notFound(Map.of("message", "Dieta não encontrada.")));
    }
}