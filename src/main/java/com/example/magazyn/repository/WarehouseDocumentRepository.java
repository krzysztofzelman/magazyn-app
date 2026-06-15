package com.example.magazyn.repository;

import com.example.magazyn.entity.DocumentStatus;
import com.example.magazyn.entity.DocumentType;
import com.example.magazyn.entity.WarehouseDocument;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WarehouseDocumentRepository extends JpaRepository<WarehouseDocument, Long>,
        JpaSpecificationExecutor<WarehouseDocument> {

    Optional<WarehouseDocument> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM WarehouseDocument d LEFT JOIN FETCH d.items i LEFT JOIN FETCH i.product WHERE d.id = :id AND d.tenantId = :tenantId")
    Optional<WarehouseDocument> findByIdWithItemsLocked(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT d FROM WarehouseDocument d LEFT JOIN FETCH d.items i LEFT JOIN FETCH i.product WHERE d.id = :id AND d.tenantId = :tenantId")
    Optional<WarehouseDocument> findByIdWithItems(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT MAX(d.number) FROM WarehouseDocument d WHERE d.type = :type AND d.number LIKE :prefix% AND d.tenantId = :tenantId")
    Optional<String> findMaxNumberByTypeAndYearAndTenantId(@Param("type") DocumentType type, @Param("prefix") String prefix, @Param("tenantId") Long tenantId);

    @EntityGraph(attributePaths = {"contractor", "items"})
    Page<WarehouseDocument> findByTypeAndTenantId(DocumentType type, Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"contractor", "items"})
    Page<WarehouseDocument> findByStatusAndTenantId(DocumentStatus status, Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"contractor", "items"})
    Page<WarehouseDocument> findByTypeAndStatusAndTenantId(DocumentType type, DocumentStatus status, Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"contractor", "items"})
    Page<WarehouseDocument> findAllByTenantId(Long tenantId, Pageable pageable);
}
