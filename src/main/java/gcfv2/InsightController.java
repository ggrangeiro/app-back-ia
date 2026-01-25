package gcfv2;

import gcfv2.dto.InsightResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.http.HttpMethod;
import jakarta.inject.Inject;

@Controller("/api/insights")
@CrossOrigin(allowedOrigins = { "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "https://fitanalizer.com.br",
        "http://localhost:3000",
        "http://localhost:5173",
        "https://app-back-ia-732767853162.southamerica-east1.run.app" }, allowedMethods = {
                HttpMethod.GET,
                HttpMethod.OPTIONS
        })
public class InsightController {

    @Inject
    private InsightService insightService;

    @Get("/professor/{professorId}")
    public HttpResponse<InsightResponse> getInsights(
            @PathVariable Long professorId,
            @QueryValue(defaultValue = "WEEK") String period,
            @QueryValue(defaultValue = "") String requesterId,
            @QueryValue(defaultValue = "") String requesterRole) {

        // TODO: Implement manual permission check if needed using requesterId and
        // requesterRole
        // For now, we allow fetching insights if the frontend sends the request.

        InsightResponse response = insightService.getInsightsForProfessor(professorId, period);
        return HttpResponse.ok(response);
    }
}
