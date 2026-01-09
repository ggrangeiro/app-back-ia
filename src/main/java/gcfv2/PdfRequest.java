package gcfv2;

import io.micronaut.serde.annotation.Serdeable;
import java.util.Map;

@Serdeable
public record PdfRequest(
        String html,
        String fileName,
        Map<String, Object> metadata) {
}
