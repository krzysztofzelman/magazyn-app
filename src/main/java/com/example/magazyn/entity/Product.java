package com.example.magazyn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Filter(name = "warehouseFilter", condition = "warehouse_id = :warehouseId")
public class Product extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String sku;

    private String description;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal defaultVatRate = new BigDecimal("23.00");

    @Column
    @Builder.Default
    private Integer minQuantity = 0;

    @Column
    private Long categoryId;

    @Column
    private Long defaultLocationId;

    @Column
    private Long locationId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean trackExpiry = false;

    @Column(unique = true)
    private String barcode;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Version
    private Integer version;
}
