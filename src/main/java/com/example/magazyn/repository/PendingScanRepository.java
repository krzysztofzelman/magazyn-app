package com.example.magazyn.repository;

import com.example.magazyn.entity.PendingScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PendingScanRepository extends JpaRepository<PendingScan, Long> {

    Optional<PendingScan> findByIdAndTenantId(Long id, Long tenantId);

    List<PendingScan> findByModeAndScannedByOrderByCreatedAtAsc(String mode, String scannedBy);

    void deleteByModeAndScannedBy(String mode, String scannedBy);

    void deleteByIdAndScannedBy(Long id, String scannedBy);

    long countByModeAndScannedBy(String mode, String scannedBy);
}
