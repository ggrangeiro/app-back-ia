package gcfv2;

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.List;

@Singleton
public class SubscriptionScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionScheduler.class);

    @Inject
    private UsuarioRepository usuarioRepository;

    @Inject
    private SubscriptionHistoryRepository subscriptionHistoryRepository;

    @Inject
    private EmailService emailService;

    /**
     * Cron Job: Roda todos os dias às 03:00 da manhã.
     * Verifica assinaturas onde subscription_end_date < AGORA e faz downgrade para
     * FREE.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void processExpiredSubscriptions() {
        LOG.info("🕒 [Cron] Iniciando verificação de assinaturas expiradas...");

        try {
            LocalDateTime now = LocalDateTime.now();
            List<Usuario> expiredUsers = usuarioRepository.findExpiredSubscriptions(now);

            if (expiredUsers.isEmpty()) {
                LOG.info("✅ [Cron] Nenhuma assinatura expirada encontrada.");
                return;
            }

            for (Usuario user : expiredUsers) {
                try {
                    LOG.info("📉 [Cron] Expirando assinatura do usuário: {} (ID: {}, Plano: {})",
                            user.getEmail(), user.getId(), user.getPlanType());

                    String oldPlan = user.getPlanType();

                    // Downgrade para FREE + Status INACTIVE + Reset de Créditos Assinatura
                    usuarioRepository.executeDowngradeToFree(user.getId());

                    // Registrar no Histórico
                    SubscriptionHistory history = new SubscriptionHistory(
                            user.getId(),
                            oldPlan,
                            "FREE",
                            "EXPIRATION_AUTO_DOWNGRADE");
                    subscriptionHistoryRepository.save(history);

                    // Enviar e-mail notificando o usuário
                    if (user.getEmail() != null && !user.getEmail().isBlank()) {
                        boolean emailSent = emailService.sendPlanExpiredEmail(
                                user.getEmail(),
                                user.getNome(),
                                oldPlan);
                        if (emailSent) {
                            LOG.info("📧 [Cron] E-mail de expiração enviado para: {}", user.getEmail());
                        } else {
                            LOG.warn("⚠️ [Cron] Falha ao enviar e-mail de expiração para: {}", user.getEmail());
                        }
                    }

                } catch (Exception e) {
                    LOG.error("❌ [Cron] Erro ao processar expiração para usuário " + user.getId(), e);
                }
            }
            LOG.info("🏁 [Cron] Processamento concluído. Total processado: {}", expiredUsers.size());

        } catch (Exception e) {
            LOG.error("❌ [Cron] Falha crítica na execução do job de expiração", e);
        }
    }
}
