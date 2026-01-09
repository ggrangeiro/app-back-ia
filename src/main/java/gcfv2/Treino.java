package gcfv2;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedProperty; // <--- ADICIONE ESTE
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.DateCreated;
import java.time.LocalDateTime;
import io.micronaut.serde.annotation.Serdeable; // <--- ADICIONE ESTE IMPORT


@Serdeable // <--- ADICIONE ESTA ANOTAÇÃO AQUI
@MappedEntity("treinos")
public class Treino {
    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;
    @MappedProperty("user_id") // <--- MAPEIA PARA user_id DO BANCO
    private String userId;
    private String content;
    private String goal;
    @DateCreated
    @MappedProperty("created_at") // <--- MAPEIA PARA created_at DO BANCO
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}