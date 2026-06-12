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

import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "inventory_items")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Filter(name = "warehouseFilter", condition = "warehouse_id = :warehouseId")
public class InventoryItem extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "expected_quantity", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal expectedQuantity = BigDecimal.ZERO;

    @Column(name = "counted_quantity", precision = 15, scale = 2)
    private BigDecimal countedQuantity;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Column(name = "scanned_by")
    private String scannedBy;

    @Column(name = "warehouse_id")
    private Long warehouseId;
}
