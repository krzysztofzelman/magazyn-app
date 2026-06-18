package com.example.magazyn.ksef.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.ksef.config.KsefConfig;
import com.example.magazyn.ksef.model.dto.KSeFAuditLogResponse;
import com.example.magazyn.ksef.model.entity.KsefAuditLog;
import com.example.magazyn.ksef.model.enums.KSeFOperationType;
import com.example.magazyn.ksef.repository.KsefAuditRepository;
import com.example.magazyn.util.AuditContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class KsefAuditService {

    private static final Logger log = LoggerFactory.getLogger(KsefAuditService.class);

    private final KsefAuditRepository auditRepository;
    private final KsefConfig config;

    public KsefAuditService(KsefAuditRepository auditRepository, KsefConfig config) {
        this.auditRepository = auditRepository;
        this.config = config;
    }

    /**
     * Log a KSeF operation. Never throws — failures are logged and swallowed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(
            KSeFOperationType operation,
            String performedBy,
            String nip,
            Long ksefInvoiceId,
            Long sessionId,
            String details,
            boolean success,
            String errorMessage,
            String errorCode,
            Integer durationMs) {

        if (!config.getAudit().isEnabled()) return;

        try {
            KsefAuditLog auditLog = KsefAuditLog.builder()
                    .operation(operation)
                    .performedBy(performedBy != null ? performedBy : "system")
                    .nip(nip)
                    .ksefInvoiceId(ksefInvoiceId)
                    .sessionId(sessionId)
                    .details(details)
                    .success(success)
                    .errorMessage(errorMessage)
                    .errorCode(errorCode)
                    .durationMs(durationMs)
                    .ipAddress(AuditContext.getIp())
                    .build();

            if (auditLog.getTenantId() == null) {
                Long contextTenant = TenantContext.getTenantId();
                auditLog.setTenantId(contextTenant != null ? contextTenant : 1L);
            }

            auditRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist KSeF audit log (operation={}): {}", operation, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<KSeFAuditLogResponse> getAuditLogs(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        return auditRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long countRecentErrors(KSeFOperationType operation, java.time.Duration duration) {
        Long tenantId = TenantContext.getTenantId();
        LocalDateTime since = LocalDateTime.now().minus(duration);
        return auditRepository.countByTenantIdAndCreatedAtAfterAndSuccess(tenantId, since, false);
    }

    /**
     * Daily cleanup of old KSeF audit logs (runs at 02:00).
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldLogs() {
        int retentionDays = config.getAudit().getRetentionDays();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        try {
            List<KsefAuditLog> oldLogs = auditRepository.findByTenantIdAndCreatedAtBetween(
                    1L, LocalDateTime.of(2000, 1, 1, 0, 0), cutoff);
            // Note: full cleanup would require a custom query — this is a placeholder
            log.info("KSeF audit cleanup: {} logs older than {} days would be purged",
                    oldLogs.size(), retentionDays);
        } catch (Exception e) {
            log.error("KSeF audit cleanup failed: {}", e.getMessage(), e);
        }
    }

    private KSeFAuditLogResponse toResponse(KsefAuditLog entity) {
        return new KSeFAuditLogResponse(
                entity.getId(),
                entity.getOperation(),
                entity.getKsefInvoiceId(),
                entity.getSessionId(),
                entity.getPerformedBy(),
                entity.getNip(),
                entity.getDetails(),
                entity.getSuccess(),
                entity.getErrorMessage(),
                entity.getErrorCode(),
                entity.getDurationMs(),
                entity.getIpAddress(),
                entity.getCreatedAt()
        );
    }
}
