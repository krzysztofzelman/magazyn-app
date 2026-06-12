package com.example.magazyn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Filter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "batches",
    indexes = {
    @Index(name = "idx_batch_product_expiry", columnList = "product_id, expiry_date"),
    @Index(name = "idx_batch_product_created", columnList = "product_id, created_at")
})
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public class Batch extends TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String lotNumber;

    @Column
    private LocalDate expiryDate;

    @Column
    private LocalDate manufacturingDate;

    @Column(nullable = false)
    private Integer quantity;

    @Column
    private Long locationId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Version
    private Integer version;
}
