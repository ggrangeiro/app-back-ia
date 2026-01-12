package gcfv2;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller("/api")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
                "https://analisa-exercicio-732767853162.southamerica-east1.run.app" })
public class SubscriptionController {

        @Inject
        private UsuarioRepository usuarioRepository;

        @Inject
        private SubscriptionHistoryRepository subscriptionHistoryRepository;

        // Definição dos planos
        private static final Map<String, Map<String, Object>> PLANS = Map.of(
                        "FREE", Map.of("name", "Gratuito", "price", 0.0, "credits", 0, "generationsLimit", 0),
                        "STARTER", Map.of("name", "Starter", "price", 59.90, "credits", 30, "generationsLimit", 10),
                        "PRO", Map.of("name", "Pro", "price", 99.90, "credits", 80, "generationsLimit", -1),
                        "STUDIO", Map.of("name", "Studio", "price", 199.90, "credits", 200, "generationsLimit", -1));

        /**
         * Lista todos os planos disponíveis
         */
        @Get("/plans")
        public HttpResponse<?> listPlans() {
                return HttpResponse.ok(Map.of(
                                "plans", List.of(
                                                Map.of("id", "FREE", "name", "Gratuito", "price", 0.0, "credits", 0,
                                                                "generationsLimit", 0,
                                                                "features", List.of()),
                                                Map.of("id", "STARTER", "name", "Starter", "price", 59.90, "credits",
                                                                30, "generationsLimit",
                                                                10, "features",
                                                                List.of("30 análises de vídeo/mês",
                                                                                "10 gerações de treino/dieta")),
                                                Map.of("id", "PRO", "name", "Pro", "price", 99.90, "credits", 80,
                                                                "generationsLimit", -1,
                                                                "features",
                                                                List.of("80 análises de vídeo/mês",
                                                                                "Gerações ilimitadas",
                                                                                "Relatórios PDF")),
                                                Map.of("id", "STUDIO", "name", "Studio", "price", 199.90, "credits",
                                                                200, "generationsLimit",
                                                                -1, "features",
                                                                List.of("200 análises de vídeo/mês",
                                                                                "Gerações ilimitadas",
                                                                                "White Label")))));
        }

        /**
         * Retorna dados do usuário logado com informações de plano e usage
         */
        @Get("/me")
        public HttpResponse<?> getMe(@QueryValue Long userId) {
                return usuarioRepository.findById(userId).map(user -> {
                        Map<String, Object> planInfo = PLANS.getOrDefault(user.getPlanType(), PLANS.get("FREE"));
                        int generationsLimit = (int) planInfo.getOrDefault("generationsLimit", 0);

                        int subCredits = user.getSubscriptionCredits() != null ? user.getSubscriptionCredits() : 0;
                        int purCredits = user.getPurchasedCredits() != null ? user.getPurchasedCredits() : 0;
                        int totalCredits = subCredits + purCredits;

                        return HttpResponse.ok(Map.of(
                                        "id", user.getId(),
                                        "name", user.getNome() != null ? user.getNome() : "",
                                        "email", user.getEmail() != null ? user.getEmail() : "",
                                        "role", user.getRole() != null ? user.getRole() : "USER",
                                        "plan", Map.of(
                                                        "type",
                                                        user.getPlanType() != null ? user.getPlanType() : "FREE",
                                                        "status",
                                                        user.getSubscriptionStatus() != null
                                                                        ? user.getSubscriptionStatus()
                                                                        : "INACTIVE",
                                                        "renewsAt",
                                                        user.getSubscriptionEndDate() != null
                                                                        ? user.getSubscriptionEndDate().toString()
                                                                        : null),
                                        "usage", Map.of(
                                                        "credits", totalCredits,
                                                        "subscriptionCredits", subCredits,
                                                        "purchasedCredits", purCredits,
                                                        "generations",
                                                        user.getGenerationsUsedCycle() != null
                                                                        ? user.getGenerationsUsedCycle()
                                                                        : 0,
                                                        "generationsLimit", generationsLimit)));
                }).orElse(HttpResponse.notFound());
        }

        /**
         * Assinar ou alterar plano
         * Body: { "planId": "PRO" }
         */
        @Post("/subscriptions/subscribe")
        @Transactional
        public HttpResponse<?> subscribe(
                        @Body Map<String, String> body,
                        @QueryValue Long userId) {

                String planId = body.get("planId");

                if (planId == null || !PLANS.containsKey(planId)) {
                        return HttpResponse.badRequest(Map.of("message", "Plano inválido."));
                }

                return usuarioRepository.findById(userId).map(user -> {
                        String oldPlan = user.getPlanType();
                        Map<String, Object> planInfo = PLANS.get(planId);
                        int newCredits = (int) planInfo.get("credits");

                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime endDate = now.plusMonths(1);

                        // Atualizar assinatura e setar subscription_credits
                        usuarioRepository.updateSubscription(
                                        userId,
                                        planId,
                                        "ACTIVE",
                                        endDate,
                                        endDate,
                                        newCredits);
                        usuarioRepository.resetSubscriptionCredits(userId, newCredits);

                        // Registrar histórico
                        String reason = oldPlan == null || "FREE".equals(oldPlan) ? "SUBSCRIPTION"
                                        : (PLANS.get(planId).get("price").toString()
                                                        .compareTo(PLANS.getOrDefault(oldPlan, PLANS.get("FREE"))
                                                                        .get("price").toString()) > 0
                                                                                        ? "UPGRADE"
                                                                                        : "DOWNGRADE");
                        subscriptionHistoryRepository.save(new SubscriptionHistory(userId, oldPlan, planId, reason));

                        return HttpResponse.ok(Map.of(
                                        "message", "Assinatura realizada com sucesso!",
                                        "plan", planId,
                                        "credits", newCredits,
                                        "expiresAt", endDate.toString()));
                }).orElse(HttpResponse.notFound());
        }

        /**
         * Cancelar assinatura
         */
        @Post("/subscriptions/cancel")
        @Transactional
        public HttpResponse<?> cancel(@QueryValue Long userId) {
                return usuarioRepository.findById(userId).map(user -> {
                        String oldPlan = user.getPlanType();

                        usuarioRepository.updateSubscriptionStatus(userId, "CANCELED");

                        subscriptionHistoryRepository
                                        .save(new SubscriptionHistory(userId, oldPlan, oldPlan, "CANCELLATION"));

                        return HttpResponse.ok(Map.of(
                                        "message", "Assinatura cancelada. Você mantém acesso até a data de expiração.",
                                        "expiresAt",
                                        user.getSubscriptionEndDate() != null ? user.getSubscriptionEndDate().toString()
                                                        : "N/A"));
                }).orElse(HttpResponse.notFound());
        }

        /**
         * Compra de créditos avulsos
         * Body: { "amount": 10 }
         */
        @Post("/credits/purchase")
        @Transactional
        public HttpResponse<?> purchaseCredits(
                        @Body Map<String, Integer> body,
                        @QueryValue Long userId) {

                Integer amount = body.get("amount");

                if (amount == null || amount <= 0) {
                        return HttpResponse.badRequest(Map.of("message", "Quantidade inválida."));
                }

                return usuarioRepository.findById(userId).map(user -> {
                        usuarioRepository.addPurchasedCredits(userId, amount);

                        int newPurchased = (user.getPurchasedCredits() != null ? user.getPurchasedCredits() : 0)
                                        + amount;

                        return HttpResponse.ok(Map.of(
                                        "message", "Créditos avulsos adicionados com sucesso!",
                                        "purchasedCredits", newPurchased));
                }).orElse(HttpResponse.notFound());
        }

        /**
         * Webhook de renovação (para gateway de pagamento)
         * Body: { "userId": 123, "event": "payment_success" }
         */
        @Post("/webhooks/payment-gateway")
        @Transactional
        public HttpResponse<?> handleWebhook(@Body Map<String, Object> payload) {
                Long userId = ((Number) payload.get("userId")).longValue();
                String event = (String) payload.get("event");

                if (!"payment_success".equals(event)) {
                        return HttpResponse.ok(Map.of("message", "Evento ignorado."));
                }

                return usuarioRepository.findById(userId).map(user -> {
                        Map<String, Object> planInfo = PLANS.getOrDefault(user.getPlanType(), PLANS.get("FREE"));
                        int newCredits = (int) planInfo.get("credits");

                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime endDate = now.plusMonths(1);

                        // Renovar: Resetar apenas subscription_credits (purchased_credits permanece!)
                        usuarioRepository.resetSubscriptionCredits(userId, newCredits);
                        usuarioRepository.updateSubscriptionStatus(userId, "ACTIVE");

                        return HttpResponse.ok(Map.of("message", "Renovação processada com sucesso."));
                }).orElse(HttpResponse.notFound());
        }
}
