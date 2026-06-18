package com.example.magazyn.ksef.model.entity;

import com.example.magazyn.entity.TenantAware;
import com.example.magazyn.ksef.model.enums.KSeFStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "ksef_invoices", indexes = {
    @Index(name = "idx_ksef_invoices_tenant", columnList = "tenant_id"),
    @Index(name = "idx_ksef_invoices_status", columnList = "tenant_id, status"),
    @Index(name = "idx_ksef_invoices_ksef_ref", columnList = "ksef_reference_number")
})
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class KsefInvoice extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "invoice_number", nullable = false, length = 100)
    private String invoiceNumber;

    @Column(name = "ksef_reference_number", length = 100)
    private String ksefReferenceNumber;

    @Column(name = "ksef_timestamp")
    private LocalDateTime ksefTimestamp;

    @Column(name = "ksef_status_code", length = 20)
    private String ksefStatusCode;

    @Column(name = "ksef_status_message", columnDefinition = "TEXT")
    private String ksefStatusMessage;

    @Column(name = "invoice_xml", columnDefinition = "TEXT")
    private String invoiceXml;

    @Column(name = "invoice_hash", length = 128)
    private String invoiceHash;

    @Column(name = "response_xml", columnDefinition = "TEXT")
    private String responseXml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private KSeFStatus status = KSeFStatus.PENDING;

    @Column(name = "submission_attempts")
    @Builder.Default
    private Integer submissionAttempts = 0;

    @Column(name = "last_submitted_at")
    private LocalDateTime lastSubmittedAt;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;

    @Column(name = "corrected_by_invoice_id")
    private Long correctedByInvoiceId;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Builder.Default
    private Integer version = 0;
}
