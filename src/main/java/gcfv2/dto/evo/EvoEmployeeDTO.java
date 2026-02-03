package gcfv2.dto.evo;

import io.micronaut.serde.annotation.Serdeable;

/**
 * DTO para representar um funcionário/empregado vindo da API EVO.
 * Baseado no endpoint GET /api/v2/employees
 */
@Serdeable
public class EvoEmployeeDTO {

    private Long idEmployee;
    private String name;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String cellPhone;
    private String photo;
    private String gender; // M ou F
    private String document; // CPF
    private Long idBranch;
    private String branchName;
    private String role; // Cargo no EVO (instrutor, recepcionista, etc.)
    private String department;
    private Boolean isActive;
    private String hireDate;
    private String lastAccessDate;

    // Construtor padrão
    public EvoEmployeeDTO() {}

    // Getters e Setters

    public Long getIdEmployee() {
        return idEmployee;
    }

    public void setIdEmployee(Long idEmployee) {
        this.idEmployee = idEmployee;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public Long getIdBranch() {
        return idBranch;
    }

    public void setIdBranch(Long idBranch) {
        this.idBranch = idBranch;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getHireDate() {
        return hireDate;
    }

    public void setHireDate(String hireDate) {
        this.hireDate = hireDate;
    }

    public String getLastAccessDate() {
        return lastAccessDate;
    }

    public void setLastAccessDate(String lastAccessDate) {
        this.lastAccessDate = lastAccessDate;
    }

    /**
     * Retorna o nome completo (firstName + lastName ou name)
     */
    public String getFullName() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (lastName != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName);
        }
        return sb.toString();
    }

    /**
     * Retorna o telefone preferencial (cellPhone ou phone)
     */
    public String getPreferredPhone() {
        return cellPhone != null && !cellPhone.isEmpty() ? cellPhone : phone;
    }

    /**
     * Verifica se é um instrutor/professor baseado no cargo
     */
    public boolean isInstructor() {
        if (role == null) return false;
        String lowerRole = role.toLowerCase();
        return lowerRole.contains("instrutor") || 
               lowerRole.contains("professor") || 
               lowerRole.contains("personal") ||
               lowerRole.contains("trainer");
    }
}
