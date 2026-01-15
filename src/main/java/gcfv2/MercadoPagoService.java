package gcfv2;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Singleton
public class MercadoPagoService {

    private static final Logger LOG = LoggerFactory.getLogger(MercadoPagoService.class);

    // Planos disponíveis
    private static final Map<String, Map<String, Object>> PLANS = Map.of(
            "STARTER", Map.of("name", "Plano Starter", "price", 59.90, "credits", 30),
            "PRO", Map.of("name", "Plano Pro", "price", 99.90, "credits", 80),
            "STUDIO", Map.of("name", "Plano Studio", "price", 199.90, "credits", 200));

    // Preços por pacote de créditos
    private static final Map<Integer, BigDecimal> CREDIT_PRICES = Map.of(
            10, new BigDecimal("19.90"),
            30, new BigDecimal("49.90"),
            50, new BigDecimal("79.90"),
            100, new BigDecimal("149.90"));

    @Value("${mercadopago.access-token:}")
    private String accessToken;

    @Value("${mercadopago.secret-key:}")
    private String secretKey;

    @Value("${mercadopago.back-url.success:https://fitai-analyzer-732767853162.us-west1.run.app/checkout/success}")
    private String successUrl;

    @Value("${mercadopago.back-url.failure:https://fitai-analyzer-732767853162.us-west1.run.app/checkout/failure}")
    private String failureUrl;

    @Value("${mercadopago.back-url.pending:https://fitai-analyzer-732767853162.us-west1.run.app/checkout/pending}")
    private String pendingUrl;

    @Value("${mercadopago.webhook-url:}")
    private String webhookUrl;

    /**
     * Cria preferência de pagamento para assinatura de plano
     */
    public Preference createSubscriptionPreference(Long userId, String planId) throws MPException, MPApiException {
        // DEBUG CRÍTICO: Verificar variáveis de ambiente "cruas"
        String rawEnvToken = System.getenv("MP_ACCESS_TOKEN");
        if (rawEnvToken == null) {
            LOG.warn("DEBUG: System.getenv('MP_ACCESS_TOKEN') retornou NULL!");
        } else {
            LOG.info("DEBUG: System.getenv('MP_ACCESS_TOKEN') = {}...",
                    rawEnvToken.substring(0, Math.min(rawEnvToken.length(), 5)));
        }

        if (accessToken == null || accessToken.trim().isEmpty()) {
            LOG.warn("MERCADOPAGO ACCESS TOKEN (Injected) ESTÁ VAZIO OU NULO! Valor: '{}'", accessToken);
        } else {
            LOG.info("MercadoPago Access Token configurado: {}...",
                    accessToken.substring(0, Math.min(accessToken.length(), 5)));
        }

        if (!PLANS.containsKey(planId)) {
            throw new IllegalArgumentException("Plano inválido: " + planId);
        }

        MercadoPagoConfig.setAccessToken(accessToken);

        Map<String, Object> planInfo = PLANS.get(planId);
        String externalReference = "user_" + userId + "_plan_" + planId + "_" + System.currentTimeMillis();

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id(planId)
                .title((String) planInfo.get("name"))
                .description("Assinatura mensal - " + planInfo.get("name"))
                .quantity(1)
                .currencyId("BRL")
                .unitPrice(new BigDecimal(planInfo.get("price").toString()))
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(successUrl)
                .failure(failureUrl)
                .pending(pendingUrl)
                .build();

        PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(externalReference);

        // Adiciona webhook URL se configurado
        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            requestBuilder.notificationUrl(webhookUrl);
        }

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(requestBuilder.build());

        LOG.info("Preferência criada: {} para usuário {} plano {}", preference.getId(), userId, planId);
        return preference;
    }

    /**
     * Cria preferência de pagamento para compra de créditos avulsos
     */
    public Preference createCreditsPreference(Long userId, Integer creditsAmount) throws MPException, MPApiException {
        if (accessToken == null || accessToken.trim().isEmpty()) {
            LOG.warn("MERCADOPAGO ACCESS TOKEN ESTÁ VAZIO OU NULO!");
        }

        if (!CREDIT_PRICES.containsKey(creditsAmount)) {
            throw new IllegalArgumentException("Pacote de créditos inválido: " + creditsAmount +
                    ". Opções válidas: " + CREDIT_PRICES.keySet());
        }

        MercadoPagoConfig.setAccessToken(accessToken);

        BigDecimal price = CREDIT_PRICES.get(creditsAmount);
        String externalReference = "user_" + userId + "_credits_" + creditsAmount + "_" + System.currentTimeMillis();

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id("CREDITS_" + creditsAmount)
                .title(creditsAmount + " Créditos Avulsos")
                .description("Pacote de " + creditsAmount + " créditos para análises de vídeo")
                .quantity(1)
                .currencyId("BRL")
                .unitPrice(price)
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(successUrl)
                .failure(failureUrl)
                .pending(pendingUrl)
                .build();

        PreferenceRequest.PreferenceRequestBuilder requestBuilder = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                .autoReturn("approved")
                .externalReference(externalReference);

        if (webhookUrl != null && !webhookUrl.isEmpty()) {
            requestBuilder.notificationUrl(webhookUrl);
        }

        PreferenceClient client = new PreferenceClient();
        Preference preference = client.create(requestBuilder.build());

        LOG.info("Preferência de créditos criada: {} para usuário {} quantidade {}",
                preference.getId(), userId, creditsAmount);
        return preference;
    }

    /**
     * Busca detalhes de um pagamento pelo ID
     */
    public Payment getPayment(Long paymentId) throws MPException, MPApiException {
        MercadoPagoConfig.setAccessToken(accessToken);
        PaymentClient client = new PaymentClient();
        return client.get(paymentId);
    }

    /**
     * Valida a assinatura HMAC do webhook do MercadoPago
     */
    public boolean validateWebhookSignature(String xSignature, String xRequestId, String dataId) {
        if (secretKey == null || secretKey.isEmpty()) {
            LOG.warn("Secret key não configurada - ignorando validação de assinatura");
            return true; // Em desenvolvimento, pode aceitar sem validação
        }

        try {
            // Parse x-signature: ts=1234567890,v1=abc123...
            String ts = null;
            String v1 = null;

            for (String part : xSignature.split(",")) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim();
                    String value = keyValue[1].trim();
                    if ("ts".equals(key)) {
                        ts = value;
                    } else if ("v1".equals(key)) {
                        v1 = value;
                    }
                }
            }

            if (ts == null || v1 == null) {
                LOG.error("Formato inválido do x-signature: {}", xSignature);
                return false;
            }

            // Monta o manifest template
            String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";

            // Calcula HMAC SHA256
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(secretKeySpec);
            byte[] hash = hmacSha256.doFinal(manifest.getBytes(StandardCharsets.UTF_8));

            // Converte para hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String calculatedSignature = hexString.toString();
            boolean valid = calculatedSignature.equals(v1);

            if (!valid) {
                LOG.error("Assinatura inválida. Esperado: {}, Recebido: {}", v1, calculatedSignature);
            }

            return valid;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("Erro ao validar assinatura: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrai userId do external_reference
     * Formato: user_{userId}_plan_{planId}_{timestamp} ou
     * user_{userId}_credits_{amount}_{timestamp}
     */
    public Long parseUserId(String externalReference) {
        if (externalReference == null || !externalReference.startsWith("user_")) {
            return null;
        }
        String[] parts = externalReference.split("_");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]);
            } catch (NumberFormatException e) {
                LOG.error("Erro ao parsear userId: {}", externalReference);
            }
        }
        return null;
    }

    /**
     * Extrai planId do external_reference
     */
    public String parsePlanId(String externalReference) {
        if (externalReference == null || !externalReference.contains("_plan_")) {
            return null;
        }
        String[] parts = externalReference.split("_");
        // Formato: user_{userId}_plan_{planId}_{timestamp}
        for (int i = 0; i < parts.length; i++) {
            if ("plan".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }

    /**
     * Extrai quantidade de créditos do external_reference
     */
    public Integer parseCreditsAmount(String externalReference) {
        if (externalReference == null || !externalReference.contains("_credits_")) {
            return null;
        }
        String[] parts = externalReference.split("_");
        // Formato: user_{userId}_credits_{amount}_{timestamp}
        for (int i = 0; i < parts.length; i++) {
            if ("credits".equals(parts[i]) && i + 1 < parts.length) {
                try {
                    return Integer.parseInt(parts[i + 1]);
                } catch (NumberFormatException e) {
                    LOG.error("Erro ao parsear credits amount: {}", externalReference);
                }
            }
        }
        return null;
    }

    /**
     * Verifica se é pagamento de assinatura ou créditos
     */
    public boolean isSubscriptionPayment(String externalReference) {
        return externalReference != null && externalReference.contains("_plan_");
    }

    public Map<String, Map<String, Object>> getPlans() {
        return PLANS;
    }

    public Map<Integer, BigDecimal> getCreditPrices() {
        return CREDIT_PRICES;
    }
}
