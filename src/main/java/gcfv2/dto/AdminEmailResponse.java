package gcfv2.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * DTO de resposta para envio de e-mail administrativo.
 */
@Serdeable
public class AdminEmailResponse {

    private boolean success;
    private String message;
    private int recipientCount;

    // Constructors
    public AdminEmailResponse() {
    }

    public AdminEmailResponse(boolean success, String message, int recipientCount) {
        this.success = success;
        this.message = message;
        this.recipientCount = recipientCount;
    }

    // Static factory methods for convenience
    public static AdminEmailResponse success(String message, int recipientCount) {
        return new AdminEmailResponse(true, message, recipientCount);
    }

    public static AdminEmailResponse error(String message) {
        return new AdminEmailResponse(false, message, 0);
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }
}
