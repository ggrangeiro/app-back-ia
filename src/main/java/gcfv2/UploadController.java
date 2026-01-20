package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.multipart.CompletedFileUpload;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller("/api/usuarios")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173" })
public class UploadController {

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);

    @Inject
    private UploadService uploadService;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Post(value = "/{id}/upload-asset", consumes = MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public HttpResponse<?> uploadAsset(
            @PathVariable Long id,
            @Part("file") CompletedFileUpload file,
            @Part("type") String type,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        try {
            // 1. Validar Permissão
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, id.toString())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
            }

            // 2. Validar Tipo de Asset vs Role
            if ("logo".equalsIgnoreCase(type)) {
                if (!"PERSONAL".equalsIgnoreCase(requesterRole) && !"ADMIN".equalsIgnoreCase(requesterRole)) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Apenas Personais ou Admins podem subir logos."));
                }
            } else if ("analysis_evidence".equalsIgnoreCase(type)) {
                // Permitido para todos que tem permissão sobre o usuário (já validado acima)
            } else if ("email_image".equalsIgnoreCase(type)) {
                // Apenas ADMIN pode fazer upload de imagens para e-mails
                if (!"ADMIN".equalsIgnoreCase(requesterRole)) {
                    return HttpResponse.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("message", "Apenas administradores podem subir imagens para e-mails."));
                }
            } else if (!"avatar".equalsIgnoreCase(type)) {
                return HttpResponse.badRequest(
                        Map.of("message",
                                "Tipo de asset inválido. Use 'avatar', 'logo', 'analysis_evidence' ou 'email_image'."));
            }

            // 3. Validar Arquivo (Tamanho máximo 2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                return HttpResponse.badRequest(Map.of("message", "O arquivo excede o limite de 2MB."));
            }

            String contentType = file.getContentType().map(MediaType::toString).orElse("").toLowerCase();
            if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
                return HttpResponse.badRequest(Map.of("message", "Apenas imagens JPG ou PNG são permitidas."));
            }

            // 4. Upload para o "Storage"
            String imageUrl;
            if ("analysis_evidence".equalsIgnoreCase(type)) {
                imageUrl = uploadService.uploadUserAnalysisEvidence(file, id);
            } else {
                imageUrl = uploadService.uploadAsset(file, type);
            }

            // 5. Atualizar Banco de Dados (Apenas para Avatar e Logo)
            if ("avatar".equalsIgnoreCase(type)) {
                usuarioRepository.updateAvatar(id, imageUrl);
            } else if ("logo".equalsIgnoreCase(type)) {
                usuarioRepository.updateBrandLogo(id, imageUrl);
            }
            // analysis_evidence não atualiza coluna no usuário

            return HttpResponse.ok(Map.of(
                    "success", true,
                    "imageUrl", imageUrl));

        } catch (Exception e) {
            LOG.error("Erro no upload de asset: {}", e.getMessage());
            return HttpResponse.serverError(Map.of("message", "Erro ao realizar upload: " + e.getMessage()));
        }
    }

    /**
     * UPLOAD DE MÚLTIPLAS IMAGENS PARA ANÁLISE
     * Permite subir várias fotos de uma vez para compor uma análise
     * (Postura/Composição Corporal)
     * 
     * POST /api/usuarios/{id}/upload-assets-batch?requesterId=X&requesterRole=Y
     * Content-Type: multipart/form-data
     * Body: files[] (array de arquivos)
     * 
     * Response: { success: true, imageUrls: ["url1", "url2", ...] }
     */
    @Post(value = "/{id}/upload-assets-batch", consumes = MediaType.MULTIPART_FORM_DATA)
    @Transactional
    public HttpResponse<?> uploadAssetsBatch(
            @PathVariable Long id,
            @Part("files") List<CompletedFileUpload> files,
            @QueryValue Long requesterId,
            @QueryValue String requesterRole) {

        try {
            // 1. Validar Permissão
            if (!usuarioRepository.hasPermission(requesterId, requesterRole, id.toString())) {
                return HttpResponse.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Acesso negado."));
            }

            // 2. Validar que há arquivos
            if (files == null || files.isEmpty()) {
                return HttpResponse.badRequest(Map.of("message", "Nenhum arquivo enviado."));
            }

            // 3. Limitar número de arquivos (máximo 10)
            if (files.size() > 10) {
                return HttpResponse.badRequest(Map.of("message", "Máximo de 10 imagens por análise."));
            }

            List<String> uploadedUrls = new ArrayList<>();

            for (CompletedFileUpload file : files) {
                // Validar tamanho (2MB por arquivo)
                if (file.getSize() > 2 * 1024 * 1024) {
                    LOG.warn("Arquivo {} excede limite de 2MB, ignorando.", file.getFilename());
                    continue;
                }

                // Validar tipo
                String contentType = file.getContentType().map(MediaType::toString).orElse("").toLowerCase();
                if (!contentType.equals("image/jpeg") && !contentType.equals("image/png")) {
                    LOG.warn("Arquivo {} não é JPG/PNG, ignorando.", file.getFilename());
                    continue;
                }

                // Upload
                String imageUrl = uploadService.uploadUserAnalysisEvidence(file, id);
                uploadedUrls.add(imageUrl);
            }

            if (uploadedUrls.isEmpty()) {
                return HttpResponse.badRequest(Map.of("message", "Nenhum arquivo válido foi enviado."));
            }

            LOG.info("Upload batch concluído para usuário {}: {} imagens", id, uploadedUrls.size());

            return HttpResponse.ok(Map.of(
                    "success", true,
                    "imageUrls", uploadedUrls,
                    "imageUrl", uploadedUrls.get(0) // Backward compatibility: primeira imagem
            ));

        } catch (Exception e) {
            LOG.error("Erro no upload batch de assets: {}", e.getMessage());
            return HttpResponse.serverError(Map.of("message", "Erro ao realizar upload: " + e.getMessage()));
        }
    }
}
