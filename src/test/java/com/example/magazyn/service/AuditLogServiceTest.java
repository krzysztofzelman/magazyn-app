package com.example.magazyn.service;

import com.example.magazyn.entity.AuditLog;
import com.example.magazyn.repository.AuditLogRepository;
import com.example.magazyn.util.AuditContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Captor
    private ArgumentCaptor<AuditLog> auditLogCaptor;

    @BeforeEach
    void setUp() {
        AuditContext.setIp("192.168.1.1");
    }

    @AfterEach
    void tearDown() {
        AuditContext.clear();
    }

    private AuditLog createAuditLog(Long id, String username, String action, String entityType,
                                     Long entityId, String details, String ip) {
        return AuditLog.builder()
                .id(id)
                .username(username)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ip)
                .timestamp(LocalDateTime.of(2025, 6, 1, 10, 0))
                .build();
    }

    // ──────────────────────────────────────────────
    // log
    // ──────────────────────────────────────────────

    @Test
    void log_success_persistsAuditLog() {
        AuditLog saved = createAuditLog(1L, "admin", "CREATE_PRODUCT", "Product", 42L,
                "Created product: Test", "192.168.1.1");
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(saved);

        auditLogService.log("admin", "CREATE_PRODUCT", "Product", 42L, "Created product: Test");

        verify(auditLogRepository).save(auditLogCaptor.capture());
        AuditLog captured = auditLogCaptor.getValue();
        assertEquals("admin", captured.getUsername());
        assertEquals("CREATE_PRODUCT", captured.getAction());
        assertEquals("Product", captured.getEntityType());
        assertEquals(42L, captured.getEntityId());
        assertEquals("Created product: Test", captured.getDetails());
        assertEquals("192.168.1.1", captured.getIpAddress());
    }

    @Test
    void log_nullUsername_usesAnonymous() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(createAuditLog(
                1L, "anonymous", "LOGIN_FAILURE", "User", null, "Login failed", null));

        auditLogService.log(null, "LOGIN_FAILURE", "User", null, "Login failed");

        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertEquals("anonymous", auditLogCaptor.getValue().getUsername());
    }

    @Test
    void log_repositoryThrows_doesNotPropagate() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("DB connection failed"));

        // Must not throw
        assertDoesNotThrow(() ->
                auditLogService.log("admin", "CREATE_PRODUCT", "Product", 1L, "test"));
    }

    @Test
    void log_noIpInContext_usesNull() {
        AuditContext.clear();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(createAuditLog(
                1L, "admin", "CREATE_PRODUCT", "Product", 1L, "test", null));

        auditLogService.log("admin", "CREATE_PRODUCT", "Product", 1L, "test");

        verify(auditLogRepository).save(auditLogCaptor.capture());
        assertNull(auditLogCaptor.getValue().getIpAddress());
    }

    // ──────────────────────────────────────────────
    // getAuditLogs
    // ──────────────────────────────────────────────

    @Test
    void getAuditLogs_noFilters_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log1 = createAuditLog(1L, "admin", "CREATE_PRODUCT", "Product", 1L, "test", "10.0.0.1");
        AuditLog log2 = createAuditLog(2L, "user1", "LOGIN_SUCCESS", "User", 2L, "login", "10.0.0.2");
        Page<AuditLog> page = new PageImpl<>(List.of(log1, log2), pageable, 2);

        when(auditLogRepository.findAllByTenantId(anyLong(), eq(pageable))).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs(null, null, pageable);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        verify(auditLogRepository).findAllByTenantId(anyLong(), eq(pageable));
    }

    @Test
    void getAuditLogs_filterByUsername() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = createAuditLog(1L, "admin", "CREATE_PRODUCT", "Product", 1L, "test", "10.0.0.1");
        Page<AuditLog> page = new PageImpl<>(List.of(log), pageable, 1);

        when(auditLogRepository.findByTenantIdAndUsernameContainingIgnoreCase(anyLong(), eq("admin"), eq(pageable))).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs("admin", null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findByTenantIdAndUsernameContainingIgnoreCase(anyLong(), eq("admin"), eq(pageable));
    }

    @Test
    void getAuditLogs_filterByAction() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = createAuditLog(1L, "admin", "DELETE_PRODUCT", "Product", 1L, "deleted", "10.0.0.1");
        Page<AuditLog> page = new PageImpl<>(List.of(log), pageable, 1);

        when(auditLogRepository.findByTenantIdAndAction(anyLong(), eq("DELETE_PRODUCT"), eq(pageable))).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs(null, "DELETE_PRODUCT", pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("DELETE_PRODUCT", result.getContent().get(0).getAction());
        verify(auditLogRepository).findByTenantIdAndAction(anyLong(), eq("DELETE_PRODUCT"), eq(pageable));
    }

    @Test
    void getAuditLogs_filterByBothUsernameAndAction() {
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log = createAuditLog(1L, "admin", "CREATE_PRODUCT", "Product", 1L, "test", "10.0.0.1");
        Page<AuditLog> page = new PageImpl<>(List.of(log), pageable, 1);

        when(auditLogRepository.findByTenantIdAndUsernameContainingIgnoreCaseAndAction(anyLong(), eq("admin"), eq("CREATE_PRODUCT"), eq(pageable)))
                .thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs("admin", "CREATE_PRODUCT", pageable);

        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findByTenantIdAndUsernameContainingIgnoreCaseAndAction(anyLong(), eq("admin"), eq("CREATE_PRODUCT"), eq(pageable));
    }

    @Test
    void getAuditLogs_blankUsername_ignoresFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findAllByTenantId(anyLong(), eq(pageable))).thenReturn(Page.empty());

        Page<AuditLog> result = auditLogService.getAuditLogs("   ", null, pageable);

        assertNotNull(result);
        verify(auditLogRepository).findAllByTenantId(anyLong(), eq(pageable));
    }

    @Test
    void getAuditLogs_blankAction_ignoresFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(auditLogRepository.findAllByTenantId(anyLong(), eq(pageable))).thenReturn(Page.empty());

        Page<AuditLog> result = auditLogService.getAuditLogs(null, "   ", pageable);

        assertNotNull(result);
        verify(auditLogRepository).findAllByTenantId(anyLong(), eq(pageable));
    }
}
