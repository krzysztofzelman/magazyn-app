package com.example.magazyn.repository;

import com.example.magazyn.entity.MovementType;
import com.example.magazyn.entity.StockMovement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @EntityGraph(attributePaths = "product")
    List<StockMovement> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("SELECT sm.product.id as productId, sm.product.name as productName, " +
           "SUM(sm.quantity) as totalSold " +
           "FROM StockMovement sm " +
           "WHERE sm.type = :type " +
           "GROUP BY sm.product.id, sm.product.name " +
           "ORDER BY totalSold DESC")
    List<TopSellingProjection> findTopSellingProducts(@Param("type") MovementType type);
}
