package gcfv2;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.multipart.CompletedFileUpload;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

@Singleton
public class UploadService {

    private static final Logger LOG = LoggerFactory.getLogger(UploadService.class);

    @Value("${gcp.storage.bucket:imagem-ai}")
    private String bucketName;

    private final Storage storage;

    public UploadService() {
        // Initializes storage. If running on GCP, it uses Google Credentials
        // automatically.
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Uploads an image to Google Cloud Storage.
     * 
     * @param file The file to upload
     * @param type 'avatar' or 'logo'
     * @return The public URL of the uploaded image
     * @throws IOException If upload fails
     */
    public String uploadAsset(CompletedFileUpload file, String type) throws IOException {
        String originalFilename = file.getFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String fileName = "assets/" + type + "/" + UUID.randomUUID().toString() + extension;

        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType().map(io.micronaut.http.MediaType::toString).orElse("image/jpeg"))
                .build();

        storage.create(blobInfo, file.getBytes());

        LOG.info("File uploaded to GCS: {}/{}", bucketName, fileName);

        // Return the API Proxy URL instead of GCS direct URL
        // The frontend will request /api/assets/{fileName}
        // which will be handled by AssetController
        return "/api/assets/" + fileName;
    }

    /**
     * Uploads an analysis evidence image to Google Cloud Storage.
     * Path: uploads/users/{userId}/analysis/{timestamp}_{filename}
     *
     * @param file   The file to upload
     * @param userId The ID of the user owning the analysis
     * @return The public URL of the uploaded image
     * @throws IOException If upload fails
     */
    public String uploadUserAnalysisEvidence(CompletedFileUpload file, Long userId) throws IOException {
        String originalFilename = file.getFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            // Default extension if missing
            extension = ".jpg";
        }

        String fileName = "uploads/users/" + userId + "/analysis/" + System.currentTimeMillis() + "_"
                + UUID.randomUUID().toString() + extension;

        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType().map(io.micronaut.http.MediaType::toString).orElse("image/jpeg"))
                .build();

        storage.create(blobInfo, file.getBytes());

        LOG.info("Analysis evidence uploaded to GCS: {}/{}", bucketName, fileName);

        // Return the Proxy URL to avoid 403 on Frontend (same as Avatar/Logo)
        return "/api/assets/" + fileName;
    }

    /**
     * Downloads an asset from Google Cloud Storage.
     * 
     * @param fileName The path/name of the file in the bucket
     * @return The file bytes
     */
    public byte[] downloadAsset(String fileName) {
        BlobId blobId = BlobId.of(bucketName, fileName);
        return storage.readAllBytes(blobId);
    }
}
