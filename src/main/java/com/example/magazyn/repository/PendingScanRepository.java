package com.example.magazyn.repository;

import com.example.magazyn.entity.PendingScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingScanRepository extends JpaRepository<PendingScan, Long> {

    Optional<PendingScan> findByIdAndTenantId(Long id, Long tenantId);

    List<PendingScan> findByModeAndScannedByAndTenantIdOrderByCreatedAtAsc(String mode, String scannedBy, Long tenantId);

    void deleteByModeAndScannedByAndTenantId(String mode, String scannedBy, Long tenantId);

    void deleteByIdAndScannedByAndTenantId(Long id, String scannedBy, Long tenantId);

    long countByModeAndScannedByAndTenantId(String mode, String scannedBy, Long tenantId);
}
