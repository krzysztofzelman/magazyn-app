package com.example.magazyn.repository;

import com.example.magazyn.entity.WarehouseDocumentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseDocumentItemRepository extends JpaRepository<WarehouseDocumentItem, Long> {

    Optional<WarehouseDocumentItem> findByIdAndTenantId(Long id, Long tenantId);
}
