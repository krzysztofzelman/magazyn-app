package com.example.magazyn.entity;

import com.example.magazyn.config.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Base class for all tenant-aware entities.
 * Automatically sets tenant_id from TenantContext on persist.
 * Each entity must also add @Filter(name="tenantFilter", condition="tenant_id = :tenantId")
 * for query-level isolation (Hibernate limitation — @Filter on @MappedSuperclass
 * does not propagate to subclasses).
 */
@Data
@MappedSuperclass
@FilterDef(
    name = "tenantFilter",
    parameters = @ParamDef(name = "tenantId", type = Long.class)
)
public abstract class TenantAware {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @PrePersist
    protected void onTenantPersist() {
        if (tenantId == null) {
            Long current = TenantContext.getTenantId();
            if (current != null) {
                this.tenantId = current;
            }
        }
    }
}
