package gcfv2;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.data.annotation.Transient;
import io.micronaut.serde.annotation.Serdeable; // IMPORTANTE: Adicione este import

@Serdeable // ISSO RESOLVE O ERRO DE CODEC/JSON
@MappedEntity("usuario")
public class Usuario {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    private String nome;
    private String email;
    private String senha;
    private String avatar;
    private String role;
    private String telefone;

    @MappedProperty("personal_id")
    private Long personalId;

    @MappedProperty("access_level")
    private String accessLevel;

    private Integer credits;

    @MappedProperty("plan_type")
    private String planType;

    @MappedProperty("subscription_status")
    private String subscriptionStatus;

    @MappedProperty("subscription_end_date")
    private java.time.LocalDateTime subscriptionEndDate;

    @MappedProperty("credits_reset_date")
    private java.time.LocalDateTime creditsResetDate;

    @MappedProperty("generations_used_cycle")
    private Integer generationsUsedCycle;

    @MappedProperty("subscription_credits")
    private Integer subscriptionCredits;

    @MappedProperty("purchased_credits")
    private Integer purchasedCredits;

    @Transient
    private Object latestWorkout;

    // Construtor padrão vazio é obrigatório para o Micronaut
    public Usuario() {
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Long getPersonalId() {
        return personalId;
    }

    public void setPersonalId(Long personalId) {
        this.personalId = personalId;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public Object getLatestWorkout() {
        return latestWorkout;
    }

    public void setLatestWorkout(Object latestWorkout) {
        this.latestWorkout = latestWorkout;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public java.time.LocalDateTime getSubscriptionEndDate() {
        return subscriptionEndDate;
    }

    public void setSubscriptionEndDate(java.time.LocalDateTime subscriptionEndDate) {
        this.subscriptionEndDate = subscriptionEndDate;
    }

    public java.time.LocalDateTime getCreditsResetDate() {
        return creditsResetDate;
    }

    public void setCreditsResetDate(java.time.LocalDateTime creditsResetDate) {
        this.creditsResetDate = creditsResetDate;
    }

    public Integer getGenerationsUsedCycle() {
        return generationsUsedCycle;
    }

    public void setGenerationsUsedCycle(Integer generationsUsedCycle) {
        this.generationsUsedCycle = generationsUsedCycle;
    }

    public Integer getSubscriptionCredits() {
        return subscriptionCredits;
    }

    public void setSubscriptionCredits(Integer subscriptionCredits) {
        this.subscriptionCredits = subscriptionCredits;
    }

    public Integer getPurchasedCredits() {
        return purchasedCredits;
    }

    public void setPurchasedCredits(Integer purchasedCredits) {
        this.purchasedCredits = purchasedCredits;
    }

    /**
     * Calcula o total de créditos (retrocompatibilidade)
     */
    @Transient
    public Integer getTotalCredits() {
        int sub = subscriptionCredits != null ? subscriptionCredits : 0;
        int pur = purchasedCredits != null ? purchasedCredits : 0;
        return sub + pur;
    }
}