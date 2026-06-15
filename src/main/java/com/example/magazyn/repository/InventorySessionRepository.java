package com.example.magazyn.repository;

import com.example.magazyn.entity.InventorySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventorySessionRepository extends JpaRepository<InventorySession, Long> {

    Optional<InventorySession> findByIdAndTenantId(Long id, Long tenantId);

    List<InventorySession> findByStatusAndTenantId(String status, Long tenantId);

    List<InventorySession> findAllByTenantId(Long tenantId);
}
