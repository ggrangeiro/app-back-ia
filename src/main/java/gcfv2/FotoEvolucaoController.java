package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller para gerenciar fotos de evolução dos alunos.
 * 
 * Endpoints:
 * - POST /api/usuarios/{id}/fotos-evolucao - Upload de nova foto
 * - GET /api/usuarios/{id}/fotos-evolucao - Listar fotos do usuário
 * - DELETE /api/fotos-evolucao/{fotoId} - Remover foto
 */
@Controller("/api")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app" })
public class FotoEvolucaoController {

    private static final Logger LOG = LoggerFactory.getLogger(FotoEvolucaoController.class);

    @Inject
    private FotoEvolucaoRepository fotoEvolucaoRepository;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private UploadService uploadService;

    @Inject
    private NotificationService notificationService;

    /**
     * Upload de foto de evolução
     * 
     * @param id            ID do usuário alvo
     * @param file          Arquivo da foto
     * @param category      Categoria: FRONT, BACK, LEFT, RIGHT
     * @param photoDate     Data da foto (YYYY-MM-DD)
     * @param requesterId   ID de quem está fazendo o upload
     * @param requesterRole Role de quem está fazendo o upload
     */
    @Post(value = "/usuarios/{id}/fotos-evolucao", consumes = MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public HttpResponse<?> uploadFotoEvolucao(
            @PathVariable Long id,
            @Part("file") CompletedFileUpload file,
            @Part("category") String category,
            @Part("photoDate") String photoDate,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        try {
            // 1. Validar Permissão
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, id.toString())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message",
                                "Acesso negado. Você não tem permissão para gerenciar fotos deste aluno."));
            }

            // 2. Validar categoria
            if (category == null || !isValidCategory(category.toUpperCase())) {
                return HttpResponse.badRequest(Map.of("message",
                        "Categoria inválida. Use: FRONT, BACK, LEFT ou RIGHT."));
            }
            String normalizedCategory = category.toUpperCase();

            // 3. Validar data
            LocalDate parsedDate;
            try {
                parsedDate = LocalDate.parse(photoDate);
            } catch (Exception e) {
                return HttpResponse.badRequest(Map.of("message",
                        "Data inválida. Use o formato YYYY-MM-DD."));
            }

            // 4. Validar arquivo (2MB máximo)
            if (file.getSize() > 2 * 1024 * 1024) {
                return HttpResponse.badRequest(Map.of("message", "O arquivo excede o limite de 2MB."));
            }

            String contentType = file.getContentType().map(MediaType::toString).orElse("").toLowerCase();
            if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
                return HttpResponse.badRequest(Map.of("message", "Apenas imagens JPG ou PNG são permitidas."));
            }

            // 5. Upload da imagem
            String imageUrl = uploadService.uploadEvolutionPhoto(file, id, normalizedCategory);

            // 6. Salvar no banco
            FotoEvolucao foto = new FotoEvolucao(id, imageUrl, normalizedCategory, parsedDate, requesterId);
            foto.setCreatedAt(LocalDateTime.now());
            FotoEvolucao saved = fotoEvolucaoRepository.save(foto);

            LOG.info("Foto de evolução salva: userId={}, category={}, photoDate={}", id, normalizedCategory, photoDate);

            // --- NOTIFICAÇÃO ---
            try {
                // Notificar o personal do aluno (id = aluno)
                notificationService.createNotification(id, "PHOTO", "Nova foto de evolução enviada.");
            } catch (Exception e) {
                LOG.error("Erro ao criar notificação de foto: {}", e.getMessage());
            }

            return HttpResponse.created(Map.of(
                    "success", true,
                    "foto", saved));

        } catch (Exception e) {
            LOG.error("Erro ao fazer upload de foto de evolução: {}", e.getMessage());
            return HttpResponse.serverError(Map.of("message", "Erro ao salvar foto: " + e.getMessage()));
        }
    }

    /**
     * Listar fotos de evolução de um usuário
     * 
     * @param id            ID do usuário
     * @param category      (Opcional) Filtrar por categoria
     * @param requesterId   ID de quem está solicitando
     * @param requesterRole Role de quem está solicitando
     */
    @Get("/usuarios/{id}/fotos-evolucao")
    public HttpResponse<?> listarFotosEvolucao(
            @PathVariable Long id,
            @Nullable @QueryValue String category,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        try {
            // 1. Validar Permissão
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, id.toString())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message",
                                "Acesso negado. Você não tem permissão para visualizar fotos deste aluno."));
            }

            // 2. Buscar fotos
            List<FotoEvolucao> fotos;
            if (category != null && !category.isEmpty() && isValidCategory(category.toUpperCase())) {
                fotos = fotoEvolucaoRepository.findByUserIdAndCategoryOrderByPhotoDateDesc(id, category.toUpperCase());
            } else {
                fotos = fotoEvolucaoRepository.findByUserIdOrderByPhotoDateDesc(id);
            }

            return HttpResponse.ok(Map.of(
                    "fotos", fotos,
                    "total", fotos.size()));

        } catch (Exception e) {
            LOG.error("Erro ao listar fotos de evolução: {}", e.getMessage());
            return HttpResponse.serverError(Map.of("message", "Erro ao buscar fotos: " + e.getMessage()));
        }
    }

    /**
     * Deletar foto de evolução
     * 
     * @param fotoId        ID da foto
     * @param requesterId   ID de quem está solicitando
     * @param requesterRole Role de quem está solicitando
     */
    @Delete("/fotos-evolucao/{fotoId}")
    @Transactional
    public HttpResponse<?> deletarFotoEvolucao(
            @PathVariable Long fotoId,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        try {
            // 1. Buscar a foto
            var fotoOpt = fotoEvolucaoRepository.findById(fotoId);
            if (fotoOpt.isEmpty()) {
                return HttpResponse.notFound(Map.of("message", "Foto não encontrada."));
            }

            FotoEvolucao foto = fotoOpt.get();

            // 2. Validar permissão sobre o usuário dono da foto
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, foto.getUserId().toString())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", "Acesso negado. Você não tem permissão para deletar esta foto."));
            }

            // 3. Deletar
            fotoEvolucaoRepository.deleteById(fotoId);

            LOG.info("Foto de evolução deletada: id={}, userId={}", fotoId, foto.getUserId());

            return HttpResponse.ok(Map.of(
                    "success", true,
                    "message", "Foto removida com sucesso."));

        } catch (Exception e) {
            LOG.error("Erro ao deletar foto de evolução: {}", e.getMessage());
            return HttpResponse.serverError(Map.of("message", "Erro ao deletar foto: " + e.getMessage()));
        }
    }

    private boolean isValidCategory(String category) {
        return "FRONT".equals(category) || "BACK".equals(category)
                || "LEFT".equals(category) || "RIGHT".equals(category);
    }
}
