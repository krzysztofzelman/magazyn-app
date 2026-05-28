package com.example.magazyn.service;

import com.example.magazyn.entity.AuditLog;
import com.example.magazyn.repository.AuditLogRepository;
import com.example.magazyn.util.AuditContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Persists an audit log entry. Never throws — failures are logged and swallowed
     * so they never block the calling business operation.
     */
    public void log(String username, String action, String entityType, Long entityId, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .username(username != null ? username : "anonymous")
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(AuditContext.getIp())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log (action={}, entityType={}, entityId={}): {}",
                    action, entityType, entityId, e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(String username, String action, Pageable pageable) {
        boolean hasUsername = username != null && !username.isBlank();
        boolean hasAction = action != null && !action.isBlank();

        if (hasUsername && hasAction) {
            return auditLogRepository.findByUsernameContainingIgnoreCaseAndAction(username, action, pageable);
        } else if (hasUsername) {
            return auditLogRepository.findByUsernameContainingIgnoreCase(username, pageable);
        } else if (hasAction) {
            return auditLogRepository.findByAction(action, pageable);
        } else {
            return auditLogRepository.findAll(pageable);
        }
    }
}
