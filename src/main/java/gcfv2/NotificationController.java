package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import jakarta.inject.Inject;
import java.util.Map;

@Controller("/api/notifications")
@CrossOrigin(allowedOrigins = {
        "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app"
})
public class NotificationController {

    @Inject
    private NotificationService notificationService;

    @Get
    public HttpResponse<?> list(@QueryValue Long requesterId, @QueryValue String requesterRole) {
        // Apenas para listar as próprias notificações (Professor/Personal)
        return HttpResponse.ok(notificationService.listForRecipient(requesterId));
    }

    @Delete("/{id}")
    public HttpResponse<?> delete(@PathVariable Long id, @QueryValue Long requesterId,
            @QueryValue String requesterRole) {
        notificationService.delete(id, requesterId);
        return HttpResponse.ok(Map.of("message", "Notificação removida."));
    }

    @Delete("/all")
    public HttpResponse<?> deleteAll(@QueryValue Long requesterId, @QueryValue String requesterRole) {
        notificationService.deleteAllForRecipient(requesterId);
        return HttpResponse.ok(Map.of("message", "Todas as notificações foram removidas."));
    }
}
