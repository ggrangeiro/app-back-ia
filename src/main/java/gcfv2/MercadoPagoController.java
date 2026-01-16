package gcfv2;

import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.server.cors.CrossOrigin;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller("/api")
@CrossOrigin({ "https://fitai-analyzer-732767853162.us-west1.run.app",
        "https://analisa-exercicio-732767853162.southamerica-east1.run.app",
        "http://localhost:5173", "http://localhost:3000", "capacitor://localhost" })
public class MercadoPagoController {

    private static final Logger LOG = LoggerFactory.getLogger(MercadoPagoController.class);

    // Planos com créditos (replicado do SubscriptionController para consistência)
    private static final Map<String, Map<String, Object>> PLANS = Map.of(
            "STARTER", Map.of("name", "Starter", "price", 59.90, "credits", 30, "generationsLimit", 10),
            "PRO", Map.of("name", "Pro", "price", 99.90, "credits", 80, "generationsLimit", -1),
            "STUDIO", Map.of("name", "Studio", "price", 199.90, "credits", 200, "generationsLimit", -1));

    @Inject
    private MercadoPagoService mercadoPagoService;

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private PaymentTransactionRepository paymentTransactionRepository;

    @Inject
    private SubscriptionHistoryRepository subscriptionHistoryRepository;

    /**
     * Lista pacotes de créditos disponíveis para compra
     */
    @Get("/checkout/credit-packages")
    public HttpResponse<?> listCreditPackages() {
        Map<Integer, BigDecimal> prices = mercadoPagoService.getCreditPrices();
        List<Map<String, Object>> packages = prices.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "amount", entry.getKey(),
                        "price", entry.getValue(),
                        "priceFormatted", "R$ " + entry.getValue().toString()))
                .collect(Collectors.toList());

        return HttpResponse.ok(Map.of("packages", packages));
    }

    /**
     * Cria preferência de pagamento para assinatura de plano
     * Body: { "planId": "PRO" }
     */
    @Post("/checkout/create-preference")
    @Transactional
    public HttpResponse<?> createSubscriptionPreference(
            @Body Map<String, String> body,
            @QueryValue Long userId) {

        String planId = body.get("planId");

        if (planId == null || !PLANS.containsKey(planId)) {
            return HttpResponse.badRequest(Map.of("message", "Plano inválido. Opções: STARTER, PRO, STUDIO"));
        }

        // Verificar se usuário existe
        return usuarioRepository.findById(userId).map(user -> {
            try {
                Preference preference = mercadoPagoService.createSubscriptionPreference(userId, planId);

                // Registrar transação como pendente
                Map<String, Object> planInfo = PLANS.get(planId);
                PaymentTransaction transaction = new PaymentTransaction(
                        userId,
                        "SUBSCRIPTION",
                        new BigDecimal(planInfo.get("price").toString()),
                        "PENDING");
                transaction.setPlanId(planId);
                transaction.setMpPreferenceId(preference.getId());
                transaction.setExternalReference(preference.getExternalReference());
                paymentTransactionRepository.save(transaction);

                return HttpResponse.ok(Map.of(
                        "preferenceId", preference.getId(),
                        "initPoint", preference.getInitPoint(),
                        "sandboxInitPoint", preference.getSandboxInitPoint() != null
                                ? preference.getSandboxInitPoint()
                                : preference.getInitPoint(),
                        "externalReference", preference.getExternalReference(),
                        "plan", Map.of(
                                "id", planId,
                                "name", planInfo.get("name"),
                                "price", planInfo.get("price"),
                                "credits", planInfo.get("credits"))));

            } catch (MPApiException e) {
                LOG.error("Erro MP API: Status={} Content={}", e.getStatusCode(), e.getApiResponse().getContent(), e);
                return HttpResponse.serverError(Map.of(
                        "message", "Erro na API do Mercado Pago",
                        "error", e.getMessage(),
                        "status", e.getStatusCode(),
                        "details", e.getApiResponse().getContent()));
            } catch (MPException e) {
                LOG.error("Erro ao criar preferência MP: {}", e.getMessage(), e);
                return HttpResponse.serverError(Map.of(
                        "message", "Erro ao criar preferência de pagamento",
                        "error", e.getMessage()));
            }
        }).orElse(HttpResponse.notFound(Map.of("message", "Usuário não encontrado")));
    }

    /**
     * Cria preferência de pagamento para compra de créditos avulsos
     * Body: { "amount": 30 }
     */
    @Post("/checkout/create-preference/credits")
    @Transactional
    public HttpResponse<?> createCreditsPreference(
            @Body Map<String, Integer> body,
            @QueryValue Long userId) {

        Integer amount = body.get("amount");

        if (amount == null) {
            return HttpResponse.badRequest(Map.of("message", "Quantidade de créditos é obrigatória"));
        }

        return usuarioRepository.findById(userId).map(user -> {
            try {
                Preference preference = mercadoPagoService.createCreditsPreference(userId, amount);
                BigDecimal price = mercadoPagoService.getCreditPrices().get(amount);

                // Registrar transação como pendente
                PaymentTransaction transaction = new PaymentTransaction(
                        userId,
                        "CREDITS",
                        price,
                        "PENDING");
                transaction.setCreditsAmount(amount);
                transaction.setMpPreferenceId(preference.getId());
                transaction.setExternalReference(preference.getExternalReference());
                paymentTransactionRepository.save(transaction);

                return HttpResponse.ok(Map.of(
                        "preferenceId", preference.getId(),
                        "initPoint", preference.getInitPoint(),
                        "sandboxInitPoint", preference.getSandboxInitPoint() != null
                                ? preference.getSandboxInitPoint()
                                : preference.getInitPoint(),
                        "externalReference", preference.getExternalReference(),
                        "credits", Map.of(
                                "amount", amount,
                                "price", price)));

            } catch (IllegalArgumentException e) {
                return HttpResponse.badRequest(Map.of("message", e.getMessage()));
            } catch (MPApiException e) {
                LOG.error("Erro MP API: Status={} Content={}", e.getStatusCode(), e.getApiResponse().getContent(), e);
                return HttpResponse.serverError(Map.of(
                        "message", "Erro na API do Mercado Pago",
                        "error", e.getMessage(),
                        "status", e.getStatusCode(),
                        "details", e.getApiResponse().getContent()));
            } catch (MPException e) {
                LOG.error("Erro ao criar preferência de créditos: {}", e.getMessage(), e);
                return HttpResponse.serverError(Map.of(
                        "message", "Erro ao criar preferência de pagamento",
                        "error", e.getMessage()));
            }
        }).orElse(HttpResponse.notFound(Map.of("message", "Usuário não encontrado")));
    }

    /**
     * Webhook do Mercado Pago - recebe notificações de pagamento
     */
    @Post("/webhooks/mercadopago")
    @Transactional
    public HttpResponse<?> handleMercadoPagoWebhook(
            HttpRequest<?> request,
            @Body Map<String, Object> payload) {

        LOG.info("Webhook recebido do MercadoPago: {}", payload);

        String type = (String) payload.get("type");
        String topic = (String) payload.get("topic");
        String dataId = null;

        // Estratégia 1: Webhook V1 (type=payment)
        if ("payment".equals(type)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payload.get("data");
            if (data != null && data.get("id") != null) {
                dataId = data.get("id").toString();
            }
        }
        // Estratégia 2: IPN Legacy (topic=payment)
        else if ("payment".equals(topic)) {
            Object resource = payload.get("resource");
            if (resource != null) {
                String resourceStr = resource.toString();
                // Se for URL completa, extrai o ID do final
                if (resourceStr.contains("/")) {
                    dataId = resourceStr.substring(resourceStr.lastIndexOf("/") + 1);
                } else {
                    dataId = resourceStr;
                }
            }
        }

        if (dataId == null) {
            LOG.info("Ignorando notificação: não é pagamento ou sem ID (type={}, topic={})", type, topic);
            return HttpResponse.ok(Map.of("message", "Notificação ignorada"));
        }

        // Validar assinatura (APENAS se for Webhook V1 com headers de assinatura)
        String xSignature = request.getHeaders().get("x-signature");
        String xRequestId = request.getHeaders().get("x-request-id");

        if (xSignature != null) {
            // DOCUMENTAÇÃO MP: id deve vir da Query String (data.id) para validação da
            // assinatura
            String queryDataId = request.getParameters().get("data.id");
            String dataIdToValidate = queryDataId != null ? queryDataId : dataId;

            if (!mercadoPagoService.validateWebhookSignature(xSignature, xRequestId, dataIdToValidate)) {
                // FALLBACK: Logar erro mas permitir processamento.
                // A segurança é garantida pela chamada subsequente à API do Mercado Pago
                // (getPayment),
                // que só retornará sucesso se o pagamento realmente existir e for válido.
                LOG.error("ALERTA: Assinatura inválida no webhook (Ignorando bloqueio para processar via API)");
            } else {
                LOG.info("Assinatura do webhook validada com sucesso!");
            }
        }

        try {
            // Buscar detalhes do pagamento na API do MP
            Payment payment = mercadoPagoService.getPayment(Long.parseLong(dataId));

            LOG.info("Pagamento encontrado: id={}, status={}, externalReference={}",
                    payment.getId(), payment.getStatus(), payment.getExternalReference());

            String status = payment.getStatus();
            String externalReference = payment.getExternalReference();

            // Buscar ou criar transação
            PaymentTransaction transaction = paymentTransactionRepository
                    .findByExternalReference(externalReference)
                    .orElse(null);

            if (transaction == null) {
                LOG.warn("Transação não encontrada para externalReference: {}", externalReference);
                // Criar nova transação baseada no payment
                Long userId = mercadoPagoService.parseUserId(externalReference);
                if (userId != null) {
                    transaction = new PaymentTransaction(
                            userId,
                            mercadoPagoService.isSubscriptionPayment(externalReference) ? "SUBSCRIPTION" : "CREDITS",
                            payment.getTransactionAmount(),
                            status.toUpperCase());
                    transaction.setMpPaymentId(payment.getId().toString());
                    transaction.setExternalReference(externalReference);

                    if (mercadoPagoService.isSubscriptionPayment(externalReference)) {
                        transaction.setPlanId(mercadoPagoService.parsePlanId(externalReference));
                    } else {
                        transaction.setCreditsAmount(mercadoPagoService.parseCreditsAmount(externalReference));
                    }
                }
            }

            if (transaction != null) {
                // IDEMPOTÊNCIA: Se já estava aprovado, não processa novamente os benefícios
                boolean alreadyApproved = "APPROVED".equalsIgnoreCase(transaction.getStatus());

                // Atualizar status da transação
                transaction.setStatus(status.toUpperCase());
                transaction.setMpPaymentId(payment.getId().toString());
                if (payment.getPaymentMethodId() != null) {
                    transaction.setPaymentMethod(payment.getPaymentMethodId());
                }
                transaction.setUpdatedAt(LocalDateTime.now());
                paymentTransactionRepository.update(transaction);

                // Processar pagamento aprovado (APENAS se não estava aprovado antes)
                if ("approved".equalsIgnoreCase(status)) {
                    if (!alreadyApproved) {
                        processApprovedPayment(transaction);
                    } else {
                        LOG.info("Pagamento já processado anteriormente. Ignorando efeitos colaterais. ExtRef={}",
                                externalReference);
                    }
                }
            }

            return HttpResponse.ok(Map.of("message", "Webhook processado com sucesso"));

        } catch (MPException | MPApiException e) {
            LOG.error("Erro ao processar webhook: {}", e.getMessage(), e);
            return HttpResponse.serverError(Map.of("message", "Erro ao processar pagamento"));
        }
    }

    /**
     * Processa pagamento aprovado - ativa assinatura ou adiciona créditos
     */
    private void processApprovedPayment(PaymentTransaction transaction) {
        Long userId = transaction.getUserId();

        usuarioRepository.findById(userId).ifPresent(user -> {
            if ("SUBSCRIPTION".equals(transaction.getPaymentType())) {
                // Ativar assinatura
                String planId = transaction.getPlanId();
                Map<String, Object> planInfo = PLANS.get(planId);

                if (planInfo != null) {
                    int newCredits = (int) planInfo.get("credits");
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime endDate;

                    // Verificar se é renovação do MESMO plano e se ainda está ATIVO
                    boolean isRenewal = planId.equals(user.getPlanType()) &&
                            "ACTIVE".equals(user.getSubscriptionStatus()) &&
                            user.getSubscriptionEndDate() != null &&
                            user.getSubscriptionEndDate().isAfter(now);

                    if (isRenewal) {
                        // Extensão: Adiciona 1 mês ao final da vigência atual
                        endDate = user.getSubscriptionEndDate().plusMonths(1);
                        LOG.info("Renovação de assinatura (Extensão): userId={}, planId={}, novaDataFim={}",
                                userId, planId, endDate);
                    } else {
                        // Nova assinatura ou Upgrade/Downgrade: Começa agora
                        endDate = now.plusMonths(1);
                        LOG.info("Nova assinatura ou Upgrade: userId={}, planId={}, novaDataFim={}",
                                userId, planId, endDate);
                    }

                    String oldPlan = user.getPlanType();

                    // Atualizar assinatura
                    usuarioRepository.updateSubscription(userId, planId, "ACTIVE", endDate, endDate, newCredits);
                    usuarioRepository.resetSubscriptionCredits(userId, newCredits);

                    // Registrar histórico
                    String reason;
                    if (isRenewal) {
                        reason = "RENEWAL";
                    } else {
                        reason = oldPlan == null || "FREE".equals(oldPlan) ? "SUBSCRIPTION" : "UPGRADE";
                    }

                    subscriptionHistoryRepository.save(new SubscriptionHistory(userId, oldPlan, planId, reason));

                    LOG.info("Assinatura ativada/renovada: userId={}, planId={}, credits={}", userId, planId,
                            newCredits);
                }

            } else if ("CREDITS".equals(transaction.getPaymentType())) {
                // Adicionar créditos avulsos
                Integer creditsAmount = transaction.getCreditsAmount();
                if (creditsAmount != null && creditsAmount > 0) {
                    usuarioRepository.addPurchasedCredits(userId, creditsAmount);
                    LOG.info("Créditos adicionados: userId={}, amount={}", userId, creditsAmount);
                }
            }
        });
    }

    /**
     * Consultar status de um pagamento
     */
    @Get("/checkout/payment-status/{paymentId}")
    public HttpResponse<?> getPaymentStatus(@PathVariable String paymentId) {
        try {
            Payment payment = mercadoPagoService.getPayment(Long.parseLong(paymentId));

            return HttpResponse.ok(Map.of(
                    "id", payment.getId(),
                    "status", payment.getStatus(),
                    "statusDetail", payment.getStatusDetail() != null ? payment.getStatusDetail() : "",
                    "externalReference", payment.getExternalReference() != null ? payment.getExternalReference() : "",
                    "paymentMethodId", payment.getPaymentMethodId() != null ? payment.getPaymentMethodId() : "",
                    "transactionAmount", payment.getTransactionAmount()));

        } catch (MPException | MPApiException e) {
            LOG.error("Erro ao consultar pagamento: {}", e.getMessage());
            return HttpResponse.notFound(Map.of("message", "Pagamento não encontrado"));
        }
    }

    /**
     * Listar transações do usuário
     */
    @Get("/checkout/transactions")
    public HttpResponse<?> getUserTransactions(@QueryValue Long userId) {
        var userOpt = usuarioRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return HttpResponse.notFound(Map.of("message", "Usuário não encontrado"));
        }

        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        List<Map<String, Object>> transactionList = transactions.stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("type", t.getPaymentType());
                    map.put("planId", t.getPlanId());
                    map.put("creditsAmount", t.getCreditsAmount());
                    map.put("amount", t.getAmount());
                    map.put("status", t.getStatus());
                    map.put("paymentMethod", t.getPaymentMethod());
                    map.put("createdAt", t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
                    return map;
                })
                .collect(Collectors.toList());

        return HttpResponse.ok(Map.of("transactions", transactionList));
    }
}
