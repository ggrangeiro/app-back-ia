package gcfv2;

import gcfv2.dto.AdminEmailRequest;
import gcfv2.dto.AdminEmailResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller para notificações administrativas.
 * 
 * Permite que administradores enviem e-mails para segmentos específicos de
 * usuários.
 */
@CrossOrigin("*")
@Controller("/api/notifications")
public class AdminNotificationController {

    private static final Logger LOG = LoggerFactory.getLogger(AdminNotificationController.class);

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private EmailService emailService;

    /**
     * Endpoint para envio de e-mails em massa pelo administrador.
     * 
     * POST /api/notifications/admin/send-email
     * 
     * Query Params:
     * - requesterId: ID do usuário logado
     * - requesterRole: Role do usuário logado (deve ser ADMIN)
     * 
     * Body:
     * {
     * "targetAudience": "ALL" | "PERSONALS" | "PERSONALS_AND_PROFESSORS" |
     * "STUDENTS" | "SPECIFIC",
     * "specificEmail": "usuario@exemplo.com", // Obrigatório apenas se
     * targetAudience == 'SPECIFIC'
     * "subject": "Título do E-mail",
     * "body": "Conteúdo do e-mail"
     * }
     */
    @Inject
    @Value("${app.backend-url}")
    private String backendUrl;

    @Post("/admin/send-email")
    public HttpResponse<AdminEmailResponse> sendAdminEmail(
            @Body AdminEmailRequest request,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole,
            io.micronaut.http.HttpRequest<?> httpRequest) {

        LOG.info("📧 Recebendo requisição de envio de e-mail administrativo de userId={}, role={}",
                requesterId, requesterRole);

        // Usar a URL configurada explicitamente (backendUrl injetada)
        // Isso remove a complexidade de tentar adivinhar a URL em ambientes cloud/proxy
        final String requestBaseUrl = backendUrl;
        LOG.info("🌐 URL base configurada: {}", requestBaseUrl);

        // 1. Verificar autorização - apenas ADMIN pode usar esta feature
        if (requesterRole == null || !"ADMIN".equalsIgnoreCase(requesterRole)) {
            LOG.warn("⚠️ Tentativa de acesso não autorizado ao envio de e-mails. userId={}, role={}",
                    requesterId, requesterRole);
            return HttpResponse.status(HttpStatus.FORBIDDEN)
                    .body(AdminEmailResponse
                            .error("Acesso negado. Apenas administradores podem enviar e-mails em massa."));
        }

        // 2. Validar campos obrigatórios
        if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
            return HttpResponse.badRequest()
                    .body(AdminEmailResponse.error("O campo 'subject' (assunto) é obrigatório."));
        }

        if (request.getBody() == null || request.getBody().trim().isEmpty()) {
            return HttpResponse.badRequest()
                    .body(AdminEmailResponse.error("O campo 'body' (corpo do e-mail) é obrigatório."));
        }

        if (request.getTargetAudience() == null || request.getTargetAudience().trim().isEmpty()) {
            return HttpResponse.badRequest()
                    .body(AdminEmailResponse.error("O campo 'targetAudience' é obrigatório."));
        }

        // 3. Resolver lista de destinatários com base no targetAudience
        List<String> emails = new ArrayList<>();
        String targetAudience = request.getTargetAudience().toUpperCase().trim();

        try {
            switch (targetAudience) {
                case "SPECIFIC":
                    if (request.getSpecificEmail() == null || request.getSpecificEmail().trim().isEmpty()) {
                        return HttpResponse.badRequest()
                                .body(AdminEmailResponse.error(
                                        "O campo 'specificEmail' é obrigatório quando targetAudience é 'SPECIFIC'."));
                    }
                    // Validação básica de e-mail
                    if (!request.getSpecificEmail().contains("@")) {
                        return HttpResponse.badRequest()
                                .body(AdminEmailResponse.error("O e-mail especificado é inválido."));
                    }
                    emails.add(request.getSpecificEmail().trim());
                    break;

                case "ALL":
                    usuarioRepository.findAll().forEach(u -> {
                        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                            emails.add(u.getEmail());
                        }
                    });
                    break;

                case "PERSONALS":
                    usuarioRepository.findByRole("personal").forEach(u -> {
                        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                            emails.add(u.getEmail());
                        }
                    });
                    break;

                case "PERSONALS_AND_PROFESSORS":
                    usuarioRepository.findByRole("personal").forEach(u -> {
                        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                            emails.add(u.getEmail());
                        }
                    });
                    usuarioRepository.findByRole("professor").forEach(u -> {
                        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                            emails.add(u.getEmail());
                        }
                    });
                    break;

                case "STUDENTS":
                    usuarioRepository.findByRole("user").forEach(u -> {
                        if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                            emails.add(u.getEmail());
                        }
                    });
                    break;

                default:
                    return HttpResponse.badRequest()
                            .body(AdminEmailResponse.error(
                                    "Valor inválido para 'targetAudience'. Valores aceitos: ALL, PERSONALS, PERSONALS_AND_PROFESSORS, STUDENTS, SPECIFIC."));
            }
        } catch (Exception e) {
            LOG.error("❌ Erro ao buscar destinatários: {}", e.getMessage());
            return HttpResponse.serverError()
                    .body(AdminEmailResponse.error("Erro ao resolver lista de destinatários."));
        }

        if (emails.isEmpty()) {
            return HttpResponse.ok()
                    .body(AdminEmailResponse.success("Nenhum destinatário encontrado para o critério selecionado.", 0));
        }

        LOG.info("📬 Enviando e-mail para {} destinatário(s). Assunto: {}", emails.size(), request.getSubject());

        // 4. Enviar e-mails (fire-and-forget para não bloquear a requisição)
        final String subject = request.getSubject().trim();
        final String body = request.getBody().trim();
        final String imageUrl = request.getImageUrl() != null ? request.getImageUrl().trim() : null;
        final int totalRecipients = emails.size();

        // Fire-and-forget: Enviar em background
        CompletableFuture.runAsync(() -> {
            int successCount = 0;
            int failCount = 0;

            for (String email : emails) {
                try {
                    boolean sent = emailService.sendAdminBroadcastEmail(email, subject, body, imageUrl, requestBaseUrl);
                    if (sent) {
                        successCount++;
                    } else {
                        failCount++;
                        LOG.warn("⚠️ Falha ao enviar e-mail para: {}", email);
                    }
                } catch (Exception e) {
                    failCount++;
                    LOG.error("❌ Exceção ao enviar e-mail para {}: {}", email, e.getMessage());
                }
            }

            LOG.info("✅ Envio de e-mails concluído. Sucesso: {}, Falha: {}", successCount, failCount);
        });

        // 5. Retornar resposta imediata (não espera o envio terminar)
        return HttpResponse.ok()
                .body(AdminEmailResponse.success(
                        String.format("E-mails sendo enviados para %d destinatário(s).", totalRecipients),
                        totalRecipients));
    }
}
