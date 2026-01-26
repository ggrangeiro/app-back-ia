package gcfv2.weather;

import io.micronaut.context.annotation.Value;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;

/**
 * Service to check current weather conditions using Google Weather API.
 * Used for weather-based achievements (e.g., "Rainy Day" badge).
 */
@Singleton
public class WeatherService {

    @Inject
    private ObjectMapper objectMapper;

    // Uses the same API key as Gemini (Google Cloud API key)
    @Value("${gemini.api-key:}")
    private String apiKey;

    /**
     * Weather condition types that indicate rain.
     */
    private static final String[] RAIN_CONDITIONS = {
            "RAIN", "LIGHT_RAIN", "HEAVY_RAIN", "RAIN_SHOWERS",
            "THUNDERSTORM", "DRIZZLE", "FREEZING_RAIN"
    };

    /**
     * Checks the current weather conditions at the given coordinates.
     *
     * @param latitude  Location latitude
     * @param longitude Location longitude
     * @return WeatherResult with condition info, or null if API call fails
     */
    @SuppressWarnings("unchecked")
    public WeatherResult getCurrentConditions(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("[WeatherService] API key not configured. Skipping weather check.");
            return null;
        }

        HttpURLConnection connection = null;
        try {
            String urlString = String.format(
                    "https://weather.googleapis.com/v1/currentConditions:lookup?key=%s&location.latitude=%f&location.longitude=%f",
                    apiKey, latitude, longitude);

            // Log masked URL
            String maskedUrl = urlString.replace(apiKey, "HIDDEN_API_KEY");
            System.out.println("[WeatherService] Calling API: " + maskedUrl);

            URI uri = URI.create(urlString);
            connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            System.out.println("[WeatherService] API Response Code: " + responseCode);

            if (responseCode != 200) {
                System.out.println("[WeatherService] API returned status: " + responseCode);
                // Read error stream if available
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    System.out.println("[WeatherService] Error Body: " + errorResponse.toString());
                } catch (Exception ex) {
                }

                return null;
            }

            // Read response
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            String jsonStr = response.toString();
            System.out.println("[WeatherService] Raw API Response: " + jsonStr);

            // Parse JSON response
            Map<String, Object> jsonResponse = objectMapper.readValue(jsonStr, Map.class);

            // Extract weather condition
            String conditionType = "UNKNOWN";
            String description = "";
            int precipitationProbability = 0;

            // Parse weatherCondition
            if (jsonResponse.containsKey("weatherCondition")) {
                Map<String, Object> weatherCondition = (Map<String, Object>) jsonResponse.get("weatherCondition");
                if (weatherCondition != null) {
                    conditionType = (String) weatherCondition.getOrDefault("type", "UNKNOWN");

                    // Get description
                    if (weatherCondition.containsKey("description")) {
                        Map<String, Object> desc = (Map<String, Object>) weatherCondition.get("description");
                        if (desc != null) {
                            description = (String) desc.getOrDefault("text", "");
                        }
                    }
                }
            }

            // Parse precipitation probability
            if (jsonResponse.containsKey("precipitation")) {
                Map<String, Object> precipitation = (Map<String, Object>) jsonResponse.get("precipitation");
                if (precipitation != null && precipitation.containsKey("probability")) {
                    Map<String, Object> probability = (Map<String, Object>) precipitation.get("probability");
                    if (probability != null) {
                        Object percent = probability.get("percent");
                        if (percent instanceof Number) {
                            precipitationProbability = ((Number) percent).intValue();
                        }
                    }
                }
            }

            System.out.println("[WeatherService] Weather at (" + latitude + ", " + longitude + "): " + conditionType
                    + " - " + description);
            return new WeatherResult(conditionType, description, precipitationProbability);

        } catch (Exception e) {
            System.out.println("[WeatherService] Error fetching weather: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Checks if the current weather indicates rain.
     *
     * @param latitude  Location latitude
     * @param longitude Location longitude
     * @return true if it's raining or high probability of rain, false otherwise
     */
    public boolean isRaining(Double latitude, Double longitude) {
        WeatherResult result = getCurrentConditions(latitude, longitude);

        if (result == null) {
            return false;
        }

        // Check if condition type indicates rain
        String upperType = result.getConditionType().toUpperCase();
        for (String rainCondition : RAIN_CONDITIONS) {
            if (upperType.contains(rainCondition)) {
                System.out.println("[WeatherService] Rain detected: " + result.getConditionType());
                return true;
            }
        }

        // Also consider high precipitation probability as "raining"
        if (result.getPrecipitationProbability() >= 70) {
            System.out.println(
                    "[WeatherService] High precipitation probability: " + result.getPrecipitationProbability() + "%");
            return true;
        }

        return false;
    }

    /**
     * Result class for weather data.
     */
    public static class WeatherResult {
        private final String conditionType;
        private final String description;
        private final int precipitationProbability;

        public WeatherResult(String conditionType, String description, int precipitationProbability) {
            this.conditionType = conditionType;
            this.description = description;
            this.precipitationProbability = precipitationProbability;
        }

        public String getConditionType() {
            return conditionType;
        }

        public String getDescription() {
            return description;
        }

        public int getPrecipitationProbability() {
            return precipitationProbability;
        }

        @Override
        public String toString() {
            return String.format("WeatherResult{type='%s', desc='%s', precip=%d%%}",
                    conditionType, description, precipitationProbability);
        }
    }
}
