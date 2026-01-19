package gcfv2.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * DTO para requisição de envio de e-mail administrativo.
 * 
 * Usado pelo endpoint POST /api/notifications/admin/send-email
 */
@Serdeable
public class AdminEmailRequest {

    /**
     * Público alvo do e-mail.
     * Valores aceitos:
     * - ALL: Todos os usuários
     * - PERSONALS: Apenas personal trainers
     * - PERSONALS_AND_PROFESSORS: Personal trainers e professores
     * - STUDENTS: Apenas alunos (role = 'user')
     * - SPECIFIC: E-mail específico (requer specificEmail)
     */
    private String targetAudience;

    /**
     * E-mail específico do destinatário.
     * Obrigatório apenas quando targetAudience == "SPECIFIC"
     */
    private String specificEmail;

    /**
     * Assunto do e-mail (obrigatório)
     */
    private String subject;

    /**
     * Corpo do e-mail em texto ou HTML básico (obrigatório)
     */
    private String body;

    // Constructors
    public AdminEmailRequest() {
    }

    public AdminEmailRequest(String targetAudience, String specificEmail, String subject, String body) {
        this.targetAudience = targetAudience;
        this.specificEmail = specificEmail;
        this.subject = subject;
        this.body = body;
    }

    // Getters and Setters
    public String getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(String targetAudience) {
        this.targetAudience = targetAudience;
    }

    public String getSpecificEmail() {
        return specificEmail;
    }

    public void setSpecificEmail(String specificEmail) {
        this.specificEmail = specificEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    /**
     * URL opcional da imagem a ser exibida no e-mail.
     * Deve ser uma URL pública acessível (ex: retorno do upload-asset com
     * type=email_image)
     */
    private String imageUrl;

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
