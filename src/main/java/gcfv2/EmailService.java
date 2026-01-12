package gcfv2;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Singleton
public class EmailService {

    private static final Logger LOG = LoggerFactory.getLogger(EmailService.class);

    @Value("${resend.api-key:}")
    private String resendApiKey;

    @Value("${resend.from-email:noreply@resend.dev}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Envia e-mail de reset de senha usando a API do Resend
     */
    public boolean sendPasswordResetEmail(String toEmail, String token, String userName) {
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            LOG.warn("Resend API key não configurada. E-mail não será enviado.");
            LOG.info("Link de reset (para testes): {}/reset-password?token={}", frontendUrl, token);
            return false;
        }

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        String htmlContent = String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                body { font-family: 'Segoe UI', Arial, sans-serif; background: #f4f4f4; padding: 20px; }
                                .container { max-width: 500px; margin: 0 auto; background: #fff; border-radius: 12px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
                                .header { text-align: center; margin-bottom: 20px; }
                                .header h1 { color: #6366f1; margin: 0; }
                                .content { color: #333; line-height: 1.6; }
                                .button { display: inline-block; background: linear-gradient(135deg, #6366f1, #8b5cf6); color: #fff; padding: 14px 28px; border-radius: 8px; text-decoration: none; font-weight: bold; margin: 20px 0; }
                                .footer { text-align: center; color: #888; font-size: 12px; margin-top: 30px; }
                                .warning { background: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px; margin: 15px 0; border-radius: 4px; }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <h1>🔐 Redefinir Senha</h1>
                                </div>
                                <div class="content">
                                    <p>Olá <strong>%s</strong>,</p>
                                    <p>Recebemos uma solicitação para redefinir a senha da sua conta.</p>
                                    <p style="text-align: center;">
                                        <a href="%s" class="button">Redefinir Minha Senha</a>
                                    </p>
                                    <div class="warning">
                                        ⚠️ Este link expira em <strong>30 minutos</strong>. Se você não solicitou esta alteração, ignore este e-mail.
                                    </div>
                                </div>
                                <div class="footer">
                                    <p>© 2026 FitAI - Análise de Exercícios</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                userName, resetLink);

        String jsonBody = String.format("""
                {
                    "from": "%s",
                    "to": ["%s"],
                    "subject": "🔐 Redefinir sua senha - FitAI",
                    "html": %s
                }
                """, fromEmail, toEmail, escapeJson(htmlContent));

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                LOG.info("E-mail de reset enviado com sucesso para: {}", toEmail);
                return true;
            } else {
                LOG.error("Erro ao enviar e-mail. Status: {}, Response: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            LOG.error("Exceção ao enviar e-mail de reset: {}", e.getMessage());
            return false;
        }
    }

    private String escapeJson(String text) {
        return "\"" + text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
