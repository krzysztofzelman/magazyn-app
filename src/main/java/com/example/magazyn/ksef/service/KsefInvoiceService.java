package com.example.magazyn.ksef.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.entity.Invoice;
import com.example.magazyn.entity.InvoiceItem;
import com.example.magazyn.ksef.config.KsefConfig;
import com.example.magazyn.ksef.exception.KsefAuthenticationException;
import com.example.magazyn.ksef.exception.KsefCommunicationException;
import com.example.magazyn.ksef.exception.KsefValidationException;
import com.example.magazyn.ksef.model.dto.*;
import com.example.magazyn.ksef.model.entity.KsefInvoice;
import com.example.magazyn.ksef.model.enums.KSeFOperationType;
import com.example.magazyn.ksef.model.enums.KSeFStatus;
import com.example.magazyn.ksef.repository.KsefInvoiceRepository;
import com.example.magazyn.ksef.repository.KsefSessionRepository;
import com.example.magazyn.repository.InvoiceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Core KSeF invoice service handling:
 * - Sending invoices to KSeF API
 * - Checking invoice status
 * - Retrieving invoices from KSeF
 * - Correcting invoices
 * - Retry with exponential backoff
 */
@Service
public class KsefInvoiceService {

    private static final Logger log = LoggerFactory.getLogger(KsefInvoiceService.class);
    private static final int MAX_RETRIES = 3;

    private final KsefConfig config;
    private final KsefInvoiceRepository ksefInvoiceRepository;
    private final InvoiceRepository invoiceRepository;
    private final KsefAuthService authService;
    private final KsefValidationService validationService;
    private final KsefAuditService auditService;
    private final KsefSessionRepository sessionRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KsefInvoiceService(KsefConfig config,
                              KsefInvoiceRepository ksefInvoiceRepository,
                              InvoiceRepository invoiceRepository,
                              KsefAuthService authService,
                              KsefValidationService validationService,
                              KsefAuditService auditService,
                              KsefSessionRepository sessionRepository) {
        this.config = config;
        this.ksefInvoiceRepository = ksefInvoiceRepository;
        this.invoiceRepository = invoiceRepository;
        this.authService = authService;
        this.validationService = validationService;
        this.auditService = auditService;
        this.sessionRepository = sessionRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectionTimeout()))
                .build();
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Send an internal invoice to KSeF.
     * Validates the invoice, converts to KSeF format, and sends via API.
     */
    @Transactional
    public KSeFSendInvoiceResponse sendInvoice(Long invoiceId, String username) {
        Long tenantId = TenantContext.getTenantId();

        // Check if already sent
        Optional<KsefInvoice> existing = ksefInvoiceRepository.findByInvoiceIdAndTenantId(invoiceId, tenantId);
        if (existing.isPresent()) {
            KsefInvoice ksefInv = existing.get();
            if (ksefInv.getStatus() == KSeFStatus.ACCEPTED || ksefInv.getStatus() == KSeFStatus.PROCESSED) {
                return new KSeFSendInvoiceResponse(
                        ksefInv.getKsefReferenceNumber(),
                        ksefInv.getStatus().name(),
                        "Faktura została już wysłana do KSeF",
                        ksefInv.getId()
                );
            }
        }

        // Load internal invoice
        Invoice invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        // Convert to KSeF format
        KSeFInvoiceRequest ksefRequest = convertToKsefRequest(invoice);

        // Validate
        List<String> validationErrors = validationService.validate(ksefRequest);
        if (!validationErrors.isEmpty()) {
            auditService.log(
                    KSeFOperationType.VALIDATION_ERROR, username, config.getNip(),
                    existing.map(KsefInvoice::getId).orElse(null), null,
                    "Validation failed: " + String.join("; ", validationErrors),
                    false, String.join("; ", validationErrors), "VALIDATION_ERROR", null);
            throw new KsefValidationException(validationErrors);
        }

        // Send with retry
        return sendWithRetry(ksefRequest, invoice, existing.orElse(null), username, 0);
    }

    /**
     * Send invoice with exponential backoff retry.
     */
    private KSeFSendInvoiceResponse sendWithRetry(KSeFInvoiceRequest request,
                                                   Invoice invoice,
                                                   KsefInvoice existingKsef,
                                                   String username,
                                                   int attempt) {
        long start = System.currentTimeMillis();

        try {
            String sessionToken = authService.getActiveSessionToken();
            String jsonPayload = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl() + "/invoice/send"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + sessionToken)
                    .timeout(Duration.ofMillis(config.getReadTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            int duration = (int) (System.currentTimeMillis() - start);
            JsonNode json = objectMapper.readTree(response.body());

            // Save/update KSeF invoice record
            KsefInvoice ksefInvoice = existingKsef != null ? existingKsef : new KsefInvoice();
            ksefInvoice.setInvoiceId(invoice.getId());
            ksefInvoice.setInvoiceNumber(invoice.getNumber());
            ksefInvoice.setSubmissionAttempts(attempt + 1);
            ksefInvoice.setLastSubmittedAt(LocalDateTime.now());
            ksefInvoice.setCreatedBy(username);

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                String referenceNumber = json.has("referenceNumber") ? json.get("referenceNumber").asText() : null;
                String statusCode = json.has("statusCode") ? json.get("statusCode").asText() : null;

                ksefInvoice.setKsefReferenceNumber(referenceNumber);
                ksefInvoice.setKsefStatusCode(statusCode);
                ksefInvoice.setStatus(KSeFStatus.SENT);
                ksefInvoice.setResponseXml(response.body());

                ksefInvoiceRepository.save(ksefInvoice);

                // Update internal invoice with KSeF reference
                invoice.setKsefReferenceNumber(referenceNumber);
                invoice.setKsefStatus(KSeFStatus.SENT.name());
                invoice.setKsefSentAt(LocalDateTime.now());
                invoiceRepository.save(invoice);

                auditService.log(
                        KSeFOperationType.INVOICE_SEND, username, config.getNip(),
                        ksefInvoice.getId(), null,
                        "Invoice " + invoice.getNumber() + " sent, ref=" + referenceNumber,
                        true, null, null, duration);

                log.info("KSeF invoice sent: {} ref={} (attempt {})",
                        invoice.getNumber(), referenceNumber, attempt + 1);

                return new KSeFSendInvoiceResponse(referenceNumber, KSeFStatus.SENT.name(),
                        "Faktura wysłana pomyślnie", ksefInvoice.getId());

            } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                // Auth error — try to refresh session and retry
                ksefInvoice.setStatus(KSeFStatus.ERROR);
                ksefInvoice.setLastErrorMessage("Auth error: " + response.body());
                ksefInvoice.setLastErrorCode(String.valueOf(response.statusCode()));
                ksefInvoiceRepository.save(ksefInvoice);

                auditService.log(
                        KSeFOperationType.AUTH_ERROR, username, config.getNip(),
                        ksefInvoice.getId(), null,
                        "HTTP " + response.statusCode() + " for invoice " + invoice.getNumber(),
                        false, json.has("error") ? json.get("error").asText() : "Auth error",
                        String.valueOf(response.statusCode()), duration);

                // Force new session on auth error
                if (attempt < 1) {
                    log.warn("KSeF auth error for invoice {}, retrying with new session", invoice.getNumber());
                    authService.closeSession(
                            authService.getActiveSession().getId(), username);
                    return sendWithRetry(request, invoice, ksefInvoice, username, attempt + 1);
                }
                throw new KsefCommunicationException(
                        "KSeF auth error: " + response.body(), response.statusCode());

            } else {
                // Other error
                String errorMsg = json.has("error") ? json.get("error").asText() : response.body();
                String errorCode = json.has("errorCode") ? json.get("errorCode").asText() : null;

                ksefInvoice.setStatus(KSeFStatus.ERROR);
                ksefInvoice.setLastErrorMessage(errorMsg);
                ksefInvoice.setLastErrorCode(errorCode);
                ksefInvoice.setResponseXml(response.body());
                ksefInvoiceRepository.save(ksefInvoice);

                auditService.log(
                        KSeFOperationType.API_ERROR, username, config.getNip(),
                        ksefInvoice.getId(), null,
                        "HTTP " + response.statusCode() + " for invoice " + invoice.getNumber(),
                        false, errorMsg, errorCode, duration);

                // Retry with backoff if within limits
                if (attempt < MAX_RETRIES - 1) {
                    long delay = (long) (config.getRetry().getDelay()
                            * Math.pow(config.getRetry().getMultiplier(), attempt));
                    log.warn("KSeF send failed (attempt {}), retrying in {}ms: {}",
                            attempt + 1, delay, errorMsg);
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    auditService.log(
                            KSeFOperationType.RETRY_SUCCESS, username, config.getNip(),
                            ksefInvoice.getId(), null,
                            "Retry attempt " + (attempt + 2) + " for invoice " + invoice.getNumber(),
                            true, null, null, null);
                    return sendWithRetry(request, invoice, ksefInvoice, username, attempt + 1);
                }

                auditService.log(
                        KSeFOperationType.RETRY_FAILURE, username, config.getNip(),
                        ksefInvoice.getId(), null,
                        "All retries exhausted for invoice " + invoice.getNumber(),
                        false, errorMsg, errorCode, duration);

                throw new KsefCommunicationException(
                        "KSeF send failed after " + MAX_RETRIES + " attempts: " + errorMsg,
                        response.statusCode(), errorCode);
            }

        } catch (KsefValidationException | KsefCommunicationException | KsefAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            int duration = (int) (System.currentTimeMillis() - start);
            auditService.log(
                    KSeFOperationType.API_ERROR, username, config.getNip(),
                    existingKsef != null ? existingKsef.getId() : null, null,
                    "Send failed: " + e.getMessage(),
                    false, e.getMessage(), "SEND_ERROR", duration);

            if (attempt < MAX_RETRIES) {
                long delay = (long) (config.getRetry().getDelay()
                        * Math.pow(config.getRetry().getMultiplier(), attempt));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return sendWithRetry(request, invoice, existingKsef, username, attempt + 1);
            }

            throw new KsefCommunicationException(
                    "KSeF send failed after " + MAX_RETRIES + " attempts: " + e.getMessage(), 0);
        }
    }

    /**
     * Get the status of a KSeF invoice.
     */
    @Transactional
    public KSeFStatusResponse getInvoiceStatus(Long ksefInvoiceId) {
        Long tenantId = TenantContext.getTenantId();
        KsefInvoice ksefInvoice = ksefInvoiceRepository.findByIdAndTenantId(ksefInvoiceId, tenantId)
                .orElseThrow(() -> new RuntimeException("KSeF invoice not found: " + ksefInvoiceId));

        // If already accepted/processed, return cached status
        if (ksefInvoice.getStatus() == KSeFStatus.ACCEPTED
                || ksefInvoice.getStatus() == KSeFStatus.REJECTED
                || ksefInvoice.getStatus() == KSeFStatus.PROCESSED) {
            return new KSeFStatusResponse(
                    ksefInvoice.getId(), ksefInvoice.getInvoiceNumber(),
                    ksefInvoice.getStatus(), ksefInvoice.getKsefReferenceNumber(),
                    ksefInvoice.getKsefStatusCode(), ksefInvoice.getKsefStatusMessage()
            );
        }

        // Query KSeF API for status
        try {
            String sessionToken = authService.getActiveSessionToken();
            String url = config.getApiUrl() + "/invoice/status/" + ksefInvoice.getKsefReferenceNumber();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + sessionToken)
                    .timeout(Duration.ofMillis(config.getReadTimeout()))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            JsonNode json = objectMapper.readTree(response.body());

            if (response.statusCode() == 200) {
                String statusStr = json.has("status") ? json.get("status").asText() : "PENDING";
                String statusCode = json.has("statusCode") ? json.get("statusCode").asText() : null;
                String statusMsg = json.has("statusMessage") ? json.get("statusMessage").asText() : null;

                KSeFStatus newStatus = mapApiStatus(statusStr);
                ksefInvoice.setStatus(newStatus);
                ksefInvoice.setKsefStatusCode(statusCode);
                ksefInvoice.setKsefStatusMessage(statusMsg);
                ksefInvoiceRepository.save(ksefInvoice);

                return new KSeFStatusResponse(
                        ksefInvoice.getId(), ksefInvoice.getInvoiceNumber(),
                        newStatus, ksefInvoice.getKsefReferenceNumber(),
                        statusCode, statusMsg
                );
            }

        } catch (Exception e) {
            log.warn("Failed to query KSeF status for invoice {}: {}",
                    ksefInvoice.getInvoiceNumber(), e.getMessage());
        }

        // Return cached status on failure
        return new KSeFStatusResponse(
                ksefInvoice.getId(), ksefInvoice.getInvoiceNumber(),
                ksefInvoice.getStatus(), ksefInvoice.getKsefReferenceNumber(),
                ksefInvoice.getKsefStatusCode(), ksefInvoice.getKsefStatusMessage()
        );
    }

    /**
     * List all KSeF invoices for the current tenant.
     */
    @Transactional(readOnly = true)
    public Page<KSeFInvoiceResponse> getAllKsefInvoices(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        return ksefInvoiceRepository.findByTenantId(tenantId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get a single KSeF invoice.
     */
    @Transactional(readOnly = true)
    public KSeFInvoiceResponse getKsefInvoice(Long id) {
        Long tenantId = TenantContext.getTenantId();
        KsefInvoice ksefInvoice = ksefInvoiceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("KSeF invoice not found: " + id));
        return toResponse(ksefInvoice);
    }

    /**
     * Get dashboard statistics.
     */
    public KSeFDashboardStats getDashboardStats() {
        Long tenantId = TenantContext.getTenantId();
        long total = ksefInvoiceRepository.countByTenantId(tenantId);
        long accepted = ksefInvoiceRepository.countByTenantIdAndStatus(tenantId, KSeFStatus.ACCEPTED);
        long rejected = ksefInvoiceRepository.countByTenantIdAndStatus(tenantId, KSeFStatus.REJECTED);
        long pending = ksefInvoiceRepository.countByTenantIdAndStatus(tenantId, KSeFStatus.PENDING);
        long errors = ksefInvoiceRepository.countByTenantIdAndStatus(tenantId, KSeFStatus.ERROR);
        long corrected = ksefInvoiceRepository.countByTenantIdAndStatus(tenantId, KSeFStatus.CORRECTED);

        boolean sessionActive = false;
        String lastSessionNip = null;
        String lastSessionStatus = null;
        long activeSessions = 0;

        try {
            var activeSession = authService.getActiveSession();
            sessionActive = true;
            lastSessionNip = activeSession.getNip();
            lastSessionStatus = activeSession.getIsActive() ? "ACTIVE" : "INACTIVE";

            activeSessions = sessionRepository.countByTenantIdAndIsActiveTrueAndExpiresAtAfter(
                    tenantId, LocalDateTime.now());
        } catch (Exception e) {
            // No active session
        }

        return new KSeFDashboardStats(
                total, accepted, rejected, pending, errors, corrected,
                activeSessions, sessionActive, lastSessionNip, lastSessionStatus
        );
    }

    /**
     * Convert internal Invoice to KSeF invoice request.
     */
    private KSeFInvoiceRequest convertToKsefRequest(Invoice invoice) {
        List<KSeFInvoiceItemRequest> items = invoice.getItems().stream()
                .map(this::convertItem)
                .toList();

        return new KSeFInvoiceRequest(
                invoice.getNumber(),
                invoice.getIssueDate(),
                invoice.getSaleDate(),
                invoice.getDueDate(),
                invoice.getTotalNet(),
                invoice.getTotalVat(),
                invoice.getTotalGross(),
                "PLN",
                invoice.getBuyerTaxId(),
                invoice.getBuyerName(),
                invoice.getBuyerAddress(),
                invoice.getSellerTaxId(),
                invoice.getSellerName(),
                invoice.getSellerAddress(),
                invoice.getPaymentMethod(),
                invoice.getPaymentAccount(),
                items,
                invoice.getNotes()
        );
    }

    private KSeFInvoiceItemRequest convertItem(InvoiceItem item) {
        return new KSeFInvoiceItemRequest(
                item.getProductName(),
                item.getProductUnit() != null ? item.getProductUnit() : "szt.",
                BigDecimal.valueOf(item.getQuantity()),
                item.getUnitPriceNet(),
                item.getVatRate(),
                item.getTotalNet(),
                item.getVatAmount(),
                item.getTotalGross()
        );
    }

    /**
     * Map KSeF API status string to internal enum.
     */
    private KSeFStatus mapApiStatus(String apiStatus) {
        if (apiStatus == null) return KSeFStatus.PENDING;
        return switch (apiStatus.toUpperCase()) {
            case "SENT" -> KSeFStatus.SENT;
            case "PROCESSED" -> KSeFStatus.PROCESSED;
            case "ACCEPTED" -> KSeFStatus.ACCEPTED;
            case "REJECTED" -> KSeFStatus.REJECTED;
            case "CORRECTED" -> KSeFStatus.CORRECTED;
            case "CANCELLED" -> KSeFStatus.CANCELLED;
            default -> KSeFStatus.PENDING;
        };
    }

    private KSeFInvoiceResponse toResponse(KsefInvoice entity) {
        return new KSeFInvoiceResponse(
                entity.getId(),
                entity.getInvoiceId(),
                entity.getInvoiceNumber(),
                entity.getKsefReferenceNumber(),
                entity.getStatus(),
                entity.getKsefStatusCode(),
                entity.getKsefStatusMessage(),
                entity.getSubmissionAttempts(),
                entity.getLastSubmittedAt(),
                entity.getLastErrorMessage(),
                entity.getLastErrorCode(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
