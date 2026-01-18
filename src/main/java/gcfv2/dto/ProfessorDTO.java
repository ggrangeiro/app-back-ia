package gcfv2.dto;

import io.micronaut.serde.annotation.Serdeable;

/**
 * DTO para resposta de listagem de professores com estatísticas.
 */
@Serdeable
public class ProfessorDTO {

    private Long id;
    private String name;
    private String email;
    private String role;
    private Long managerId;
    private Integer credits;
    private Long studentsCount;
    private String lastActivity;
    private String avatar;

    public ProfessorDTO() {
    }

    public ProfessorDTO(Long id, String name, String email, String role, Long managerId,
            Integer credits, Long studentsCount, String lastActivity, String avatar) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.managerId = managerId;
        this.credits = credits;
        this.studentsCount = studentsCount;
        this.lastActivity = lastActivity;
        this.avatar = avatar;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public Integer getCredits() {
        return credits;
    }

    public void setCredits(Integer credits) {
        this.credits = credits;
    }

    public Long getStudentsCount() {
        return studentsCount;
    }

    public void setStudentsCount(Long studentsCount) {
        this.studentsCount = studentsCount;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
