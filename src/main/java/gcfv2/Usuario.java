package gcfv2;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Serdeable
@Introspected
@MappedEntity("usuario")
public class Usuario {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;
    
    @MappedProperty("nome") 
    private String name;
    
    private String email;
    private String senha;
    private String role;
    private String avatar;

    // IMPORTANTE: O nome desta variável deve bater com o @Join no Repository
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = "usuario")
    private List<UsuarioExercicio> assignedExercisesList = new ArrayList<>();

    public Usuario() {}

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public List<UsuarioExercicio> getAssignedExercisesList() { return assignedExercisesList; }
    public void setAssignedExercisesList(List<UsuarioExercicio> assignedExercisesList) { this.assignedExercisesList = assignedExercisesList; }

    // Este método gera o campo "assignedExercises" no JSON para o React
    @Transient
    public List<String> getAssignedExercises() {
        if (assignedExercisesList == null) return new ArrayList<>();
        return assignedExercisesList.stream()
                                    .map(UsuarioExercicio::getExercicio)
                                    .collect(Collectors.toList());
    }
}