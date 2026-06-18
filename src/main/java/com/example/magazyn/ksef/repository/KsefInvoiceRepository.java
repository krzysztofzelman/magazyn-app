package com.example.magazyn.ksef.repository;

import com.example.magazyn.ksef.model.entity.KsefInvoice;
import com.example.magazyn.ksef.model.enums.KSeFStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KsefInvoiceRepository extends JpaRepository<KsefInvoice, Long> {

    Optional<KsefInvoice> findByIdAndTenantId(Long id, Long tenantId);

    Optional<KsefInvoice> findByInvoiceIdAndTenantId(Long invoiceId, Long tenantId);

    Optional<KsefInvoice> findByKsefReferenceNumber(String referenceNumber);

    Page<KsefInvoice> findByTenantId(Long tenantId, Pageable pageable);

    List<KsefInvoice> findByTenantIdAndStatus(Long tenantId, KSeFStatus status);

    @Query("SELECT k FROM KsefInvoice k WHERE k.tenantId = :tenantId AND k.status IN :statuses")
    List<KsefInvoice> findByTenantIdAndStatusIn(
        @Param("tenantId") Long tenantId,
        @Param("statuses") List<KSeFStatus> statuses
    );

    long countByTenantId(Long tenantId);

    long countByTenantIdAndStatus(Long tenantId, KSeFStatus status);

    @Query("SELECT k FROM KsefInvoice k WHERE k.tenantId = :tenantId AND k.status = 'PENDING' AND k.submissionAttempts < :maxAttempts")
    List<KsefInvoice> findPendingForRetry(
        @Param("tenantId") Long tenantId,
        @Param("maxAttempts") int maxAttempts
    );
}
