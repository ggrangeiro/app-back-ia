package gcfv2;

import io.micronaut.data.annotation.*;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@MappedEntity("exercises")
public class Exercise {

    @Id
    private String id; // Usaremos os IDs de texto como 'SQUAT', 'PUSHUP'
    
    private String name;
    private String description;
    private String category;
    
    @MappedProperty("image_url")
    private String imageUrl;
    
    private Boolean active;

    public Exercise() {}

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}