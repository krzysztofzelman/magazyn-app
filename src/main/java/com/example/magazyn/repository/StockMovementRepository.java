package com.example.magazyn.repository;

import com.example.magazyn.entity.MovementType;
import com.example.magazyn.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Optional<StockMovement> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = "product")
    List<StockMovement> findByProductIdAndTenantIdOrderByCreatedAtDesc(Long productId, Long tenantId);

    @EntityGraph(attributePaths = "product")
    Page<StockMovement> findByProductIdAndTenantIdOrderByCreatedAtDesc(Long productId, Long tenantId, Pageable pageable);

    @Query("SELECT sm.product.id as productId, sm.product.name as productName, " +
           "SUM(sm.quantity) as totalSold " +
           "FROM StockMovement sm " +
           "WHERE sm.type = :type AND sm.tenantId = :tenantId " +
           "GROUP BY sm.product.id, sm.product.name " +
           "ORDER BY totalSold DESC")
    List<TopSellingProjection> findTopSellingProducts(@Param("type") MovementType type, @Param("tenantId") Long tenantId);
}
