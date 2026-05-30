package com.example.magazyn.repository;

import com.example.magazyn.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findBySessionId(Long sessionId);

    List<InventoryItem> findBySessionIdAndLocationId(Long sessionId, Long locationId);

    Optional<InventoryItem> findBySessionIdAndLocationIdAndProductId(
            Long sessionId, Long locationId, Long productId);
}
