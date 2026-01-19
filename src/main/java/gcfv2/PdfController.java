package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpMethod;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/api/pdf")
@CrossOrigin(allowedOrigins = { "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br" }, allowedMethods = {
                HttpMethod.POST,
                HttpMethod.OPTIONS
        })
public class PdfController {

    @Inject
    private PdfService pdfService;

    private static final Logger LOG = LoggerFactory.getLogger(PdfController.class);

    @Post("/generate")
    @Produces(MediaType.APPLICATION_PDF)
    public HttpResponse<byte[]> generatePdf(@Body PdfRequest request) {
        LOG.info("Recebida requisição para gerar PDF: {}", request.fileName());
        try {
            byte[] pdf = pdfService.generatePdf(request.html());

            if (pdf == null || pdf.length == 0) {
                LOG.error("PDF gerado está vazio (0 bytes).");
                return HttpResponse.serverError();
            }

            String fileName = request.fileName() != null ? request.fileName() : "documento";
            if (!fileName.toLowerCase().endsWith(".pdf")) {
                fileName += ".pdf";
            }

            LOG.info("Retornando PDF: {} ({} bytes)", fileName, pdf.length);
            return HttpResponse.ok(pdf)
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Length", String.valueOf(pdf.length))
                    .contentType(MediaType.APPLICATION_PDF);
        } catch (Exception e) {
            LOG.error("Erro ao processar PDF: {}", e.getMessage(), e);
            return HttpResponse.serverError();
        }
    }
}
