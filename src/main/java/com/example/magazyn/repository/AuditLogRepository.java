package com.example.magazyn.repository;

import com.example.magazyn.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Optional<AuditLog> findByIdAndTenantId(Long id, Long tenantId);

    Page<AuditLog> findAllByTenantId(Long tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndUsernameContainingIgnoreCase(Long tenantId, String username, Pageable pageable);

    Page<AuditLog> findByTenantIdAndAction(Long tenantId, String action, Pageable pageable);

    Page<AuditLog> findByTenantIdAndUsernameContainingIgnoreCaseAndAction(Long tenantId, String username, String action, Pageable pageable);

    Page<AuditLog> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByUsernameContainingIgnoreCaseAndAction(
            String username, String action, Pageable pageable);
}
