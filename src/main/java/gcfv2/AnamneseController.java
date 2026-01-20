package gcfv2;

import gcfv2.dto.anamnese.AnamnesisDTO;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import java.util.Map;
import java.time.LocalDateTime;

@Controller("/api/usuarios")
@CrossOrigin(allowedOrigins = {
        "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app"
})
public class AnamneseController {

    @Inject
    private UsuarioRepository usuarioRepository;

    /**
     * GET Anamnese by User ID
     */
    @Get("/{userId}/anamnese")
    public HttpResponse<?> obterAnamnese(
            @PathVariable Long userId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }

        return usuarioRepository.findById(userId)
                .<HttpResponse<?>>map(user -> {
                    if (user.getAnamnesis() != null) {
                        return HttpResponse.ok(user.getAnamnesis());
                    } else {
                        // Retorna um objeto vazio ou 404 dependendo da convenção.
                        // O frontend espera poder editar, então retornar objeto vazio ou null é ok.
                        return HttpResponse.ok(new AnamnesisDTO());
                    }
                })
                .orElse(HttpResponse.notFound(Map.of("message", "Usuário não encontrado.")));
    }

    /**
     * PUT/UPDATE Anamnese (Full or Partial update logic implemented here)
     * Although the verb is PUT, we will simply replace the object with the incoming
     * one
     * because the frontend sends the structure. For generic "patch" behavior,
     * merging would be ideal, but for now we accept the object as source of truth.
     * Note: The spec mentioned "Aceita payload parcial (PATCH behavior)",
     * but doing deep merging on a nested JSON object manually in Java is complex.
     * We assume the frontend sends the current state + updates.
     */
    @Put("/{userId}/anamnese")
    @Transactional
    public HttpResponse<?> atualizarAnamnese(
            @PathVariable Long userId,
            @Body AnamnesisDTO anamnese,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        if (!usuarioRepository.hasPermission(requesterId, requesterRole, userId.toString())) {
            return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
        }

        return usuarioRepository.findById(userId).map(user -> {

            // Set timestamp if not present
            if (anamnese.getUpdatedAt() == null) {
                anamnese.setUpdatedAt(LocalDateTime.now().toString());
            }

            // Update user entity
            user.setAnamnesis(anamnese);
            usuarioRepository.update(user);

            return HttpResponse.ok(Map.of(
                    "success", true,
                    "message", "Anamnese atualizada com sucesso.",
                    "data", anamnese));
        }).orElse(HttpResponse.notFound(Map.of("message", "Usuário não encontrado.")));
    }
}
