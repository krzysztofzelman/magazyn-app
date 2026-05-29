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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM WarehouseDocument d LEFT JOIN FETCH d.items i LEFT JOIN FETCH i.product WHERE d.id = :id")
    Optional<WarehouseDocument> findByIdWithItemsLocked(@Param("id") Long id);

    @Query("SELECT d FROM WarehouseDocument d LEFT JOIN FETCH d.items i LEFT JOIN FETCH i.product WHERE d.id = :id")
    Optional<WarehouseDocument> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT MAX(d.number) FROM WarehouseDocument d WHERE d.type = :type AND d.number LIKE :prefix%")
    Optional<String> findMaxNumberByTypeAndYear(@Param("type") DocumentType type, @Param("prefix") String prefix);

    @EntityGraph(attributePaths = {"contractor", "items"})
    Page<WarehouseDocument> findByType(DocumentType type, Pageable pageable);

    @EntityGraph(attributePaths = {"contractor", "items"})
    Page<WarehouseDocument> findByStatus(DocumentStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"contractor", "items"})
    Page<WarehouseDocument> findByTypeAndStatus(DocumentType type, DocumentStatus status, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"contractor", "items"})
    Page<WarehouseDocument> findAll(Pageable pageable);
}
