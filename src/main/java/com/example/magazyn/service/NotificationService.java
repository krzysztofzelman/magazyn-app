package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.entity.Batch;
import com.example.magazyn.entity.Product;
import com.example.magazyn.entity.Tenant;
import com.example.magazyn.repository.BatchRepository;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "notifications.enabled", havingValue = "true", matchIfMissing = false)
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final BatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final EmailService emailService;
    private final TenantRepository tenantRepository;
    private final EntityManager entityManager;
    private final int expiryWarningDays;

    public NotificationService(BatchRepository batchRepository,
                               ProductRepository productRepository,
                               EmailService emailService,
                               TenantRepository tenantRepository,
                               EntityManager entityManager,
                               @Value("${notifications.expiry-warning-days:14}") int expiryWarningDays) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.emailService = emailService;
        this.tenantRepository = tenantRepository;
        this.entityManager = entityManager;
        this.expiryWarningDays = expiryWarningDays;
    }

    @Scheduled(cron = "${notifications.cron:0 0 7 * * *}") // every day at 07:00 by default
    @Transactional
    public void sendDailyNotifications() {
        log.info("Running daily notification check...");

        List<Tenant> activeTenants = tenantRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .collect(Collectors.toList());

        if (activeTenants.isEmpty()) {
            log.info("No active tenants found — skipping notifications");
            return;
        }

        for (Tenant tenant : activeTenants) {
            TenantContext.setTenantId(tenant.getId());
            try {
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenant.getId());

                log.debug("Processing notifications for tenant id={}", tenant.getId());
                sendExpiryNotifications(tenant.getId());
                sendLowStockNotifications(tenant.getId());
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void sendExpiryNotifications(Long tenantId) {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(expiryWarningDays);

        List<Batch> expiringBatches = batchRepository.findExpiringBatches(today, threshold);
        if (expiringBatches.isEmpty()) {
            log.debug("No expiring batches for tenant id={}", tenantId);
            return;
        }

        String subject = String.format("[Magazyn] %d partii wygasa w ci\u0105gu %d dni",
                expiringBatches.size(), expiryWarningDays);

        String text = expiringBatches.stream()
                .map(b -> String.format("Produkt: %s | Partia: %s | Wa\u017Cna do: %s | Ilo\u015B\u0107: %d %s",
                        b.getProduct().getName(),
                        b.getLotNumber() != null ? b.getLotNumber() : "-",
                        b.getExpiryDate(),
                        b.getQuantity(),
                        b.getProduct().getUnit()))
                .collect(Collectors.joining("\n"));

        emailService.sendHtmlToAdmins(subject, EmailService.bodyText(text), tenantId);
    }

    private void sendLowStockNotifications(Long tenantId) {
        List<Product> lowStockProducts = productRepository.findProductsBelowMinStock();
        if (lowStockProducts.isEmpty()) {
            log.debug("No low-stock products for tenant id={}", tenantId);
            return;
        }

        String subject = String.format("[Magazyn] %d produkt\u00F3w poni\u017Cej minimalnego stanu",
                lowStockProducts.size());

        String text = lowStockProducts.stream()
                .map(p -> String.format("Produkt: %s | SKU: %s | Stan: %d %s | Minimum: %d",
                        p.getName(),
                        p.getSku(),
                        p.getQuantity(),
                        p.getUnit(),
                        p.getMinQuantity()))
                .collect(Collectors.joining("\n"));

        emailService.sendHtmlToAdmins(subject, EmailService.bodyText(text), tenantId);
    }
}
