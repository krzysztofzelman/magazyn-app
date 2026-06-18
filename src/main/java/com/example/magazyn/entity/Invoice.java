package com.example.magazyn.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "invoices")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Filter(name = "warehouseFilter", condition = "warehouse_id = :warehouseId")
public class Invoice extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    @Column(name = "document_id")
    private Long documentId;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    // Seller info (snapshot at time of issuance)
    @Column(nullable = false)
    private String sellerName;

    private String sellerTaxId;

    @Column(columnDefinition = "TEXT")
    private String sellerAddress;

    private String sellerBankAccount;

    // Buyer info (snapshot at time of issuance)
    @Column(nullable = false)
    private String buyerName;

    private String buyerTaxId;

    @Column(columnDefinition = "TEXT")
    private String buyerAddress;

    // Dates
    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false)
    private LocalDate saleDate;

    private LocalDate dueDate;

    // Payment
    @Column(length = 20)
    @Builder.Default
    private String paymentMethod = "PRZELEW";

    private String paymentAccount;

    // Totals
    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalNet = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalVat = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false)
    private String createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("id ASC")
    private List<InvoiceItem> items = new ArrayList<>();

    // KSeF fields
    @Column(name = "ksef_status", length = 30)
    private String ksefStatus;

    @Column(name = "ksef_reference_number", length = 100)
    private String ksefReferenceNumber;

    @Column(name = "ksef_sent_at")
    private LocalDateTime ksefSentAt;

    @Version
    private Integer version;
}
