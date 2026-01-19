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
}
