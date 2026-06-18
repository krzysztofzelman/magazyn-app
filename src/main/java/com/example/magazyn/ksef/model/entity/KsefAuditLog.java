package com.example.magazyn.ksef.model.entity;

import com.example.magazyn.entity.TenantAware;
import com.example.magazyn.ksef.model.enums.KSeFOperationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ksef_audit_log", indexes = {
    @Index(name = "idx_ksef_audit_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ksef_audit_operation", columnList = "tenant_id, operation"),
    @Index(name = "idx_ksef_audit_created", columnList = "tenant_id, created_at")
})
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class KsefAuditLog extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private KSeFOperationType operation;

    @Column(name = "ksef_invoice_id")
    private Long ksefInvoiceId;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(length = 20)
    private String nip;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(nullable = false)
    @Builder.Default
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
