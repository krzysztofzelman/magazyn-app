package com.example.magazyn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "locations")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Filter(name = "warehouseFilter", condition = "warehouse_id = :warehouseId")
public class Location extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LocationType type;

    @Column
    private Long parentId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String rack;

    @Column
    private String shelf;

    @Column(unique = true)
    private String barcode;

    @Column(columnDefinition = "TEXT")
    private String qrData;

    @Column
    private Integer capacity;

    @Column
    @Builder.Default
    private Integer occupied = 0;

    @Column
    private String zone;

    @Column
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "warehouse_id")
    private Long warehouseId;
}
