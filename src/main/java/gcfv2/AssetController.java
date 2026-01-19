package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.server.cors.CrossOrigin;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/api/assets")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173" })
public class AssetController {

    private static final Logger LOG = LoggerFactory.getLogger(AssetController.class);

    @Inject
    private UploadService uploadService;

    @Get("/{+path}")
    public HttpResponse<byte[]> getAsset(@PathVariable String path) {
        try {
            byte[] fileBytes = uploadService.downloadAsset(path);

            // Determine content type based on extension
            MediaType mediaType = MediaType.IMAGE_JPEG_TYPE;
            if (path.toLowerCase().endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG_TYPE;
            } else if (path.toLowerCase().endsWith(".gif")) {
                mediaType = MediaType.IMAGE_GIF_TYPE;
            }

            return HttpResponse.ok(fileBytes).contentType(mediaType);
        } catch (Exception e) {
            LOG.error("Error serving asset {}: {}", path, e.getMessage());
            return HttpResponse.notFound();
        }
    }
}
