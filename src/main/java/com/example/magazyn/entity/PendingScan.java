package com.example.magazyn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "pending_scans")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Filter(name = "warehouseFilter", condition = "warehouse_id = :warehouseId")
public class PendingScan extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String mode;

    @Column(nullable = false)
    private String barcode;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "product_sku", nullable = false)
    private String productSku;

    @Column(name = "product_unit", nullable = false, length = 50)
    private String productUnit;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "scanned_by", nullable = false, length = 100)
    private String scannedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "warehouse_id")
    private Long warehouseId;
}
