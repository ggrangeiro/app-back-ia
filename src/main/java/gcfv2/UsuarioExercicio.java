package gcfv2;

import io.micronaut.data.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import com.fasterxml.jackson.annotation.JsonIgnore; // Importante!

@Serdeable
@MappedEntity("usuario_exercicios")
public class UsuarioExercicio {
    
    @Id
    @GeneratedValue
    private Long id;
    
    private String exercicio;

    @Relation(Relation.Kind.MANY_TO_ONE)
    @MappedProperty("usuario_id")
    @JsonIgnore // <--- ADICIONE ESTA LINHA AQUI!
    private Usuario usuario;

    public UsuarioExercicio() {
    }

    // Getters e Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExercicio() { return exercicio; }
    public void setExercicio(String exercicio) { this.exercicio = exercicio; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}