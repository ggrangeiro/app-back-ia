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

    private Integer credits;

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
}