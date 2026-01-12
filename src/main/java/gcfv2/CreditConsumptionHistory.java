package gcfv2;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;

import java.time.LocalDateTime;

@Serdeable
@MappedEntity("credit_consumption_history")
public class CreditConsumptionHistory {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private Long userId;

    private String reason; // DIETA, TREINO, ANALISE

    @MappedProperty("analysis_type")
    private String analysisType; // Tipo específico de análise

    @MappedProperty("credits_consumed")
    private Integer creditsConsumed;

    @MappedProperty("was_free")
    private Boolean wasFree; // Indica se usou geração gratuita

    @MappedProperty("credit_source")
    private String creditSource; // SUBSCRIPTION, PURCHASED, FREE

    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    public CreditConsumptionHistory() {
    }

    public CreditConsumptionHistory(Long userId, String reason, String analysisType,
            Integer creditsConsumed, Boolean wasFree, String creditSource) {
        this.userId = userId;
        this.reason = reason;
        this.analysisType = analysisType;
        this.creditsConsumed = creditsConsumed;
        this.wasFree = wasFree;
        this.creditSource = creditSource;
        this.createdAt = LocalDateTime.now();
    }

    // Getters e Setters
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(String analysisType) {
        this.analysisType = analysisType;
    }

    public Integer getCreditsConsumed() {
        return creditsConsumed;
    }

    public void setCreditsConsumed(Integer creditsConsumed) {
        this.creditsConsumed = creditsConsumed;
    }

    public Boolean getWasFree() {
        return wasFree;
    }

    public void setWasFree(Boolean wasFree) {
        this.wasFree = wasFree;
    }

    public String getCreditSource() {
        return creditSource;
    }

    public void setCreditSource(String creditSource) {
        this.creditSource = creditSource;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
