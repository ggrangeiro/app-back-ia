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

    /**
     * Envia e-mail notificando que o plano expirou e voltou para FREE
     */
    public boolean sendPlanExpiredEmail(String toEmail, String userName, String oldPlan) {
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            LOG.warn("Resend API key não configurada. E-mail de expiração não será enviado.");
            return false;
        }

        String planDisplayName = switch (oldPlan.toUpperCase()) {
            case "PRO" -> "Pro";
            case "PREMIUM" -> "Premium";
            default -> oldPlan;
        };

        String htmlContent = String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body {
                                    font-family: 'Segoe UI', Arial, sans-serif;
                                    background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                                    padding: 40px 20px;
                                    margin: 0;
                                }
                                .container {
                                    max-width: 520px;
                                    margin: 0 auto;
                                    background: rgba(255, 255, 255, 0.95);
                                    border-radius: 24px;
                                    padding: 40px;
                                    box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
                                    backdrop-filter: blur(10px);
                                }
                                .header {
                                    text-align: center;
                                    margin-bottom: 30px;
                                }
                                .header .emoji {
                                    font-size: 48px;
                                    margin-bottom: 15px;
                                }
                                .header h1 {
                                    background: linear-gradient(135deg, #6366f1, #8b5cf6);
                                    -webkit-background-clip: text;
                                    -webkit-text-fill-color: transparent;
                                    background-clip: text;
                                    margin: 0;
                                    font-size: 28px;
                                }
                                .content {
                                    color: #374151;
                                    line-height: 1.7;
                                    font-size: 16px;
                                }
                                .highlight-box {
                                    background: linear-gradient(135deg, #fef3c7 0%%, #fde68a 100%%);
                                    border-left: 4px solid #f59e0b;
                                    padding: 16px 20px;
                                    border-radius: 12px;
                                    margin: 25px 0;
                                }
                                .highlight-box .title {
                                    font-weight: bold;
                                    color: #92400e;
                                    margin-bottom: 8px;
                                }
                                .free-features {
                                    background: #f3f4f6;
                                    border-radius: 16px;
                                    padding: 20px;
                                    margin: 25px 0;
                                }
                                .free-features h3 {
                                    color: #4b5563;
                                    margin: 0 0 15px 0;
                                    font-size: 16px;
                                }
                                .free-features ul {
                                    margin: 0;
                                    padding-left: 20px;
                                    color: #6b7280;
                                }
                                .free-features li {
                                    margin: 8px 0;
                                }
                                .button {
                                    display: inline-block;
                                    background: linear-gradient(135deg, #6366f1, #8b5cf6);
                                    color: #fff !important;
                                    padding: 16px 32px;
                                    border-radius: 12px;
                                    text-decoration: none;
                                    font-weight: bold;
                                    font-size: 16px;
                                    box-shadow: 0 10px 25px -5px rgba(99, 102, 241, 0.4);
                                    transition: transform 0.2s;
                                }
                                .button:hover {
                                    transform: translateY(-2px);
                                }
                                .footer {
                                    text-align: center;
                                    color: #9ca3af;
                                    font-size: 13px;
                                    margin-top: 35px;
                                    padding-top: 20px;
                                    border-top: 1px solid #e5e7eb;
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="header">
                                    <div class="emoji">📋</div>
                                    <h1>Sua assinatura expirou</h1>
                                </div>
                                <div class="content">
                                    <p>Olá <strong>%s</strong>,</p>
                                    <p>Esperamos que você esteja bem! 💪</p>

                                    <div class="highlight-box">
                                        <div class="title">⏰ O que aconteceu?</div>
                                        Sua assinatura do plano <strong>%s</strong> expirou e sua conta foi automaticamente convertida para o <strong>plano Free</strong>.
                                    </div>

                                    <div class="free-features">
                                        <h3>✨ No plano Free você ainda pode:</h3>
                                        <ul>
                                            <li>Acessar suas análises anteriores</li>
                                            <li>Visualizar treinos e dietas salvas</li>
                                            <li>Usar funcionalidades básicas do app</li>
                                        </ul>
                                    </div>

                                    <p>Sentimos sua falta nos recursos premium! Se quiser voltar a ter acesso completo, é só renovar sua assinatura:</p>

                                    <p style="text-align: center; margin: 30px 0;">
                                        <a href="%s" class="button">🚀 Renovar Assinatura</a>
                                    </p>

                                    <p style="color: #6b7280; font-size: 14px;">Ficou com alguma dúvida? Responda este e-mail que teremos prazer em ajudar!</p>
                                </div>
                                <div class="footer">
                                    <p>© 2026 FitAI - Análise de Exercícios</p>
                                    <p>Você recebeu este e-mail porque sua assinatura expirou.</p>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                userName != null ? userName : "Usuário",
                planDisplayName,
                frontendUrl + "/subscription");

        String jsonBody = String.format("""
                {
                    "from": "%s",
                    "to": ["%s"],
                    "subject": "📋 Sua assinatura expirou - FitAI",
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
                LOG.info("📧 E-mail de expiração enviado com sucesso para: {}", toEmail);
                return true;
            } else {
                LOG.error("❌ Erro ao enviar e-mail de expiração. Status: {}, Response: {}",
                        response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            LOG.error("❌ Exceção ao enviar e-mail de expiração: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Envia e-mail de boas-vindas com resumo de funcionalidades
     */
    public boolean sendWelcomeEmail(String toEmail, String userName, String role) {
        if (resendApiKey == null || resendApiKey.isEmpty()) {
            LOG.warn("Resend API key não configurada. E-mail de boas-vindas não será enviado.");
            return false;
        }

        String featuresHtml = "";
        String welcomeMessage = "";

        if ("PERSONAL".equalsIgnoreCase(role)) {
            welcomeMessage = "É um prazer ter você como parceiro! Prepare-se para elevar o nível da sua consultoria.";
            featuresHtml = """
                    <div class="feature-item">
                        <div class="icon">👥</div>
                        <div class="text"><strong>Gestão Completa de Alunos</strong><br>Organize e monitore todos os seus alunos em um só lugar.</div>
                    </div>
                    <div class="feature-item">
                        <div class="icon">💪</div>
                        <div class="text"><strong>Prescrição Inteligente</strong><br>Crie treinos e dietas personalizados com auxílio da nossa IA.</div>
                    </div>
                    <div class="feature-item">
                        <div class="icon">📊</div>
                        <div class="text"><strong>Relatórios de Progresso</strong><br>Acompanhe a evolução de cada aluno com gráficos detalhados.</div>
                    </div>
                    <div class="feature-item">
                        <div class="icon">🤖</div>
                        <div class="text"><strong>IA Avançada</strong><br>Acesso total às ferramentas de análise biomecânica e correção.</div>
                    </div>
                    """;
        } else {
            welcomeMessage = "Sua jornada para uma vida mais saudável começa agora! Estamos aqui para te guiar.";
            featuresHtml = """
                    <div class="feature-item">
                        <div class="icon">🏋️</div>
                        <div class="text"><strong>Treinos Personalizados</strong><br>Fichas de treino adaptadas ao seu objetivo e nível.</div>
                    </div>
                    <div class="feature-item">
                        <div class="icon">🥗</div>
                        <div class="text"><strong>Consultoria Nutricional AI</strong><br>Dietas flexíveis com substituições e cálculo de macros.</div>
                    </div>
                    <div class="feature-item">
                        <div class="icon">📈</div>
                        <div class="text"><strong>Evolução Corporal</strong><br>Registre e visualize seu progresso semana a semana.</div>
                    </div>
                    <div class="feature-item">
                        <div class="icon">🎥</div>
                        <div class="text"><strong>Análise de Execução</strong><br>Use nossa IA para corrigir sua postura nos exercícios.</div>
                    </div>
                    """;
        }

        String htmlContent = String.format(
                """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f2f5; padding: 0; margin: 0; }
                                .wrapper { width: 100%%; table-layout: fixed; background-color: #f0f2f5; padding-bottom: 40px; }
                                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.05); }
                                .header { background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%); padding: 40px 20px; text-align: center; color: white; }
                                .header h1 { margin: 0; font-size: 28px; font-weight: 700; margin-bottom: 10px; }
                                .header p { margin: 0; font-size: 16px; opacity: 0.9; }
                                .content { padding: 40px 30px; color: #334155; line-height: 1.6; }
                                .welcome-text { font-size: 18px; margin-bottom: 30px; text-align: center; color: #1e293b; }
                                .features-box { background: #f8fafc; border-radius: 12px; padding: 25px; margin: 30px 0; border: 1px solid #e2e8f0; }
                                .features-title { font-size: 16px; text-transform: uppercase; letter-spacing: 1px; color: #64748b; font-weight: 700; margin-bottom: 20px; text-align: center; }
                                .feature-item { display: flex; align-items: flex-start; margin-bottom: 20px; }
                                .feature-item:last-child { margin-bottom: 0; }
                                .feature-item .icon { background: #e0e7ff; color: #4f46e5; width: 40px; height: 40px; border-radius: 50%%; display: flex; align-items: center; justify-content: center; font-size: 20px; margin-right: 15px; flex-shrink: 0; }
                                .feature-item .text { font-size: 15px; color: #475569; }
                                .cta-button { display: block; width: fit-content; margin: 35px auto 0; background: #4f46e5; color: #ffffff !important; padding: 16px 40px; border-radius: 50px; text-decoration: none; font-weight: 600; font-size: 16px; text-align: center; box-shadow: 0 4px 6px -1px rgba(79, 70, 229, 0.2); transition: transform 0.2s; }
                                .cta-button:hover { background: #4338ca; transform: translateY(-2px); }
                                .footer { background: #f8fafc; padding: 20px; text-align: center; color: #94a3b8; font-size: 12px; border-top: 1px solid #e2e8f0; }
                                .social-links { margin-top: 10px; }
                                .social-links a { color: #64748b; text-decoration: none; margin: 0 5px; }
                            </style>
                        </head>
                        <body>
                            <div class="wrapper">
                                <div class="container">
                                    <div class="header">
                                        <h1>Bem-vindo(a) à FitAI! 🚀</h1>
                                        <p>%s</p>
                                    </div>
                                    <div class="content">
                                        <p class="welcome-text">Olá, <strong>%s</strong>! Estamos muito felizes em ter você conosco.</p>
                                       \s
                                        <div class="features-box">
                                            <div class="features-title">✨ O que você pode fazer agora</div>
                                            %s
                                        </div>

                                        <p style="text-align: center; margin-top: 30px;">
                                            Para começar, acesse sua conta clicando no botão abaixo:
                                        </p>

                                        <a href="https://analisa-exercicio-732767853162.southamerica-east1.run.app/" class="cta-button">Acessar Plataforma</a>
                                    </div>
                                    <div class="footer">
                                        <p>© 2026 FitAI - Tecnologia em Performance</p>
                                        <p>Você recebeu este e-mail porque se cadastrou na FitAI.</p>
                                    </div>
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                welcomeMessage,
                userName,
                featuresHtml);

        String jsonBody = String.format("""
                {
                    "from": "%s",
                    "to": ["%s"],
                    "subject": "🚀 Bem-vindo(a) à FitAI - Vamos começar?",
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
                LOG.info("✅ E-mail de boas-vindas enviado para: {}", toEmail);
                return true;
            } else {
                LOG.error("❌ Erro ao enviar welcome email. Status: {}, Response: {}",
                        response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            LOG.error("❌ Exceção ao enviar welcome email: {}", e.getMessage());
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
