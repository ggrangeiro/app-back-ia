package gcfv2;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.data.annotation.MappedProperty;
import io.micronaut.serde.annotation.Serdeable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidade para armazenar fotos de evolução dos alunos.
 * Cada foto pertence a uma categoria: FRONT (frente), BACK (costas),
 * LEFT (lado esquerdo), RIGHT (lado direito).
 */
@Serdeable
@MappedEntity("foto_evolucao")
public class FotoEvolucao {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    @MappedProperty("user_id")
    private Long userId;

    @MappedProperty("image_url")
    private String imageUrl;

    @MappedProperty("category")
    private String category; // FRONT, BACK, LEFT, RIGHT

    @MappedProperty("photo_date")
    private LocalDate photoDate;

    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    @MappedProperty("uploaded_by")
    private Long uploadedBy;

    public FotoEvolucao() {
    }

    public FotoEvolucao(Long userId, String imageUrl, String category, LocalDate photoDate, Long uploadedBy) {
        this.userId = userId;
        this.imageUrl = imageUrl;
        this.category = category;
        this.photoDate = photoDate;
        this.uploadedBy = uploadedBy;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDate getPhotoDate() {
        return photoDate;
    }

    public void setPhotoDate(LocalDate photoDate) {
        this.photoDate = photoDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}
