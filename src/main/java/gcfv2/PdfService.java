package gcfv2;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Margin;
import jakarta.inject.Singleton;

@Singleton
public class PdfService {

    public byte[] generatePdf(String htmlContent) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            String fullHtml = String.format(
                    "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                            "<link href='https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap' rel='stylesheet'>"
                            +
                            "<style>%s</style></head><body>" +
                            "<div id='pdf-container'>%s</div></body></html>",
                    getFitAiStyles(), htmlContent);

            page.setContent(fullHtml);

            // Wait for fonts to load or some delay if needed, but setContent is usually
            // enough for static HTML

            byte[] pdfBytes = page.pdf(new Page.PdfOptions()
                    .setFormat("A4")
                    .setPrintBackground(true)
                    .setMargin(new Margin().setTop("20px").setBottom("20px").setLeft("20px").setRight("20px")));

            browser.close();
            return pdfBytes;
        }
    }

    private String getFitAiStyles() {
        return "body { font-family: 'Plus Jakarta Sans', sans-serif; color: #0f172a; margin: 0; padding: 0; }" +
                "#pdf-container { padding: 20px; }" +
                ".slate-900 { color: #0f172a; }" +
                ".text-slate-900 { color: #0f172a; }" +
                // Add common Tailwind utilities that might be used if they are not in the
                // provided HTML
                "table { width: 100%; border-collapse: collapse; }" +
                "th, td { border: 1px solid #e2e8f0; padding: 8px; text-align: left; }";
    }
}
