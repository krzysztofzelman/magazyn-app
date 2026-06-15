package com.example.magazyn.repository;

import com.example.magazyn.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    Optional<InventoryItem> findByIdAndTenantId(Long id, Long tenantId);

    List<InventoryItem> findBySessionIdAndTenantId(Long sessionId, Long tenantId);

    List<InventoryItem> findBySessionIdAndLocationIdAndTenantId(Long sessionId, Long locationId, Long tenantId);

    Optional<InventoryItem> findBySessionIdAndLocationIdAndProductIdAndTenantId(
            Long sessionId, Long locationId, Long productId, Long tenantId);
}
