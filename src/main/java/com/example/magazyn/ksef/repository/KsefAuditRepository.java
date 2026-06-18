package com.example.magazyn.ksef.repository;

import com.example.magazyn.ksef.model.entity.KsefAuditLog;
import com.example.magazyn.ksef.model.enums.KSeFOperationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface KsefAuditRepository extends JpaRepository<KsefAuditLog, Long> {

    Page<KsefAuditLog> findByTenantIdOrderByCreatedAtDesc(Long tenantId, Pageable pageable);

    List<KsefAuditLog> findByTenantIdAndOperation(Long tenantId, KSeFOperationType operation);

    List<KsefAuditLog> findByTenantIdAndCreatedAtBetween(Long tenantId, LocalDateTime from, LocalDateTime to);

    long countByTenantIdAndOperationAndSuccess(Long tenantId, KSeFOperationType operation, boolean success);

    long countByTenantIdAndCreatedAtAfterAndSuccess(Long tenantId, LocalDateTime after, boolean success);
}
