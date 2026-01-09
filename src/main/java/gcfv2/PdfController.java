package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpMethod;
import jakarta.inject.Inject;

@Controller("/api/pdf")
@CrossOrigin(allowedOrigins = "https://fitai-analyzer-732767853162.us-west1.run.app", allowedMethods = {
        HttpMethod.POST,
        HttpMethod.OPTIONS
})
public class PdfController {

    @Inject
    private PdfService pdfService;

    @Post("/generate")
    @Produces(MediaType.APPLICATION_PDF)
    public HttpResponse<byte[]> generatePdf(@Body PdfRequest request) {
        try {
            byte[] pdf = pdfService.generatePdf(request.html());
            String fileName = request.fileName() != null ? request.fileName() : "documento";
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                fileName += ".pdf";
            }

            return HttpResponse.ok(pdf)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_PDF);
        } catch (Exception e) {
            // Log the error (can use a logger if available)
            e.printStackTrace();
            return HttpResponse.serverError();
        }
    }
}
