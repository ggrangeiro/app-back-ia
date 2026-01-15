package gcfv2;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Serdeable
@MappedEntity("payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private Long userId;

    @MappedProperty("plan_id")
    private String planId;

    @MappedProperty("credits_amount")
    private Integer creditsAmount;

    @MappedProperty("payment_type")
    private String paymentType; // SUBSCRIPTION, CREDITS

    @MappedProperty("mp_payment_id")
    private String mpPaymentId;

    @MappedProperty("mp_preference_id")
    private String mpPreferenceId;

    @MappedProperty("external_reference")
    private String externalReference;

    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

    private BigDecimal amount;

    @MappedProperty("payment_method")
    private String paymentMethod;

    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    @MappedProperty("updated_at")
    private LocalDateTime updatedAt;

    public PaymentTransaction() {
    }

    public PaymentTransaction(Long userId, String paymentType, BigDecimal amount, String status) {
        this.userId = userId;
        this.paymentType = paymentType;
        this.amount = amount;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public Integer getCreditsAmount() {
        return creditsAmount;
    }

    public void setCreditsAmount(Integer creditsAmount) {
        this.creditsAmount = creditsAmount;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getMpPaymentId() {
        return mpPaymentId;
    }

    public void setMpPaymentId(String mpPaymentId) {
        this.mpPaymentId = mpPaymentId;
    }

    public String getMpPreferenceId() {
        return mpPreferenceId;
    }

    public void setMpPreferenceId(String mpPreferenceId) {
        this.mpPreferenceId = mpPreferenceId;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
