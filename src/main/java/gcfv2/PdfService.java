package gcfv2;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Margin;
import com.microsoft.playwright.options.WaitUntilState;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

@Singleton
public class PdfService {

    private static final Logger LOG = LoggerFactory.getLogger(PdfService.class);

    public byte[] generatePdf(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            LOG.warn("Recebido conteúdo HTML vazio para geração de PDF.");
            return new byte[0];
        }

        LOG.info("Iniciando geração de PDF para conteúdo de tamanho: {} caracteres", htmlContent.length());

        try (Playwright playwright = Playwright.create()) {
            LOG.debug("Lançando navegador Chromium...");
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList(
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-dev-shm-usage",
                            "--font-render-hinting=none")));

            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            String fullHtml = String.format(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                            "<link href='https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap' rel='stylesheet'>"
                            +
                            "<style>%s</style></head><body>" +
                            "<div id='pdf-container'>%s</div></body></html>",
                    getFitAiStyles(), htmlContent);

            LOG.debug("Definindo conteúdo HTML...");
            page.setContent(fullHtml, new Page.SetContentOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

            LOG.debug("Gerando PDF...");
            byte[] pdfBytes = page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("20px").setBottom("20px").setLeft("20px").setRight("20px")));

            browser.close();
            LOG.info("PDF gerado com sucesso. Tamanho: {} bytes", pdfBytes.length);
            return pdfBytes;
        } catch (Exception e) {
            LOG.error("Erro durante a geração do PDF: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String getFitAiStyles() {
        return "body { font-family: 'Plus Jakarta Sans', sans-serif; color: #0f172a; margin: 0; padding: 0; }" +
                "#pdf-container { padding: 20px; }" +
                ".slate-900 { color: #0f172a; }" +
                ".text-slate-900 { color: #0f172a; }" +
                "table { width: 100%; border-collapse: collapse; }" +
                "th, td { border: 1px solid #e2e8f0; padding: 8px; text-align: left; }";
    }
}
