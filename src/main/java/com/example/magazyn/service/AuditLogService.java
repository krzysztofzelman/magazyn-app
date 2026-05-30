package com.example.magazyn.service;

import com.example.magazyn.entity.AuditLog;
import com.example.magazyn.repository.AuditLogRepository;
import com.example.magazyn.util.AuditContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    public byte[] exportAuditLogsCsv(String username, String action) {
        Pageable all = PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> logs = getAuditLogs(username, action, all);

        StringBuilder sb = new StringBuilder();
        sb.append("ID,Username,Action,EntityType,EntityId,Details,IP,Timestamp\n");
        for (AuditLog log : logs.getContent()) {
            sb.append(log.getId()).append(",");
            sb.append(escapeCsv(log.getUsername())).append(",");
            sb.append(escapeCsv(log.getAction())).append(",");
            sb.append(escapeCsv(log.getEntityType())).append(",");
            sb.append(log.getEntityId() != null ? log.getEntityId() : "").append(",");
            sb.append(escapeCsv(log.getDetails())).append(",");
            sb.append(escapeCsv(log.getIpAddress())).append(",");
            sb.append(log.getTimestamp() != null ? log.getTimestamp().toString() : "").append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
