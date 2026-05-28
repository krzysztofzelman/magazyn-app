package com.example.magazyn.service;

import com.example.magazyn.entity.Batch;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.BatchRepository;
import com.example.magazyn.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
    private final int expiryWarningDays;

    public NotificationService(BatchRepository batchRepository,
                               ProductRepository productRepository,
                               EmailService emailService,
                               @Value("${notifications.expiry-warning-days:14}") int expiryWarningDays) {
        this.batchRepository = batchRepository;
        this.productRepository = productRepository;
        this.emailService = emailService;
        this.expiryWarningDays = expiryWarningDays;
    }

    @Scheduled(cron = "${notifications.cron:0 0 7 * * *}") // every day at 07:00 by default
    public void sendDailyNotifications() {
        log.info("Running daily notification check...");
        sendExpiryNotifications();
        sendLowStockNotifications();
    }

    private void sendExpiryNotifications() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(expiryWarningDays);

        List<Batch> expiringBatches = batchRepository.findExpiringBatches(today, threshold);
        if (expiringBatches.isEmpty()) {
            log.info("No expiring batches found");
            return;
        }

        String subject = String.format("[Magazyn] %d partii wygasa w ci\u0105gu %d dni",
                expiringBatches.size(), expiryWarningDays);

        String text = expiringBatches.stream()
                .map(b -> String.format("Produkt: %s | Partia: %s | Wa\u017Cna do: %s | Ilo\u015B\u0107: %.2f %s",
                        b.getProduct().getName(),
                        b.getLotNumber() != null ? b.getLotNumber() : "-",
                        b.getExpiryDate(),
                        b.getQuantity(),
                        b.getProduct().getUnit()))
                .collect(Collectors.joining("\n"));

        emailService.sendToAdmins(subject, text);
    }

    private void sendLowStockNotifications() {
        List<Product> lowStockProducts = productRepository.findProductsBelowMinStock();
        if (lowStockProducts.isEmpty()) {
            log.info("No products below minimum stock");
            return;
        }

        String subject = String.format("[Magazyn] %d produkt\u00F3w poni\u017Cej minimalnego stanu",
                lowStockProducts.size());

        String text = lowStockProducts.stream()
                .map(p -> String.format("Produkt: %s | SKU: %s | Stan: %.2f %s | Minimum: %.2f",
                        p.getName(),
                        p.getSku(),
                        p.getQuantity(),
                        p.getUnit(),
                        p.getMinQuantity()))
                .collect(Collectors.joining("\n"));

        emailService.sendToAdmins(subject, text);
    }
}
