package com.example.magazyn.repository;

import com.example.magazyn.entity.Batch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.EntityGraph;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    Optional<Batch> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"product"})
    List<Batch> findByProductIdAndTenantIdOrderByExpiryDateAsc(Long productId, Long tenantId);

    @EntityGraph(attributePaths = {"product"})
    List<Batch> findByProductIdAndTenantIdOrderByCreatedAtAsc(Long productId, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Batch b WHERE b.id = :id AND b.tenantId = :tenantId")
    Optional<Batch> findByIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Batch b WHERE b.product.id = :productId AND b.lotNumber = :lotNumber AND b.tenantId = :tenantId")
    Optional<Batch> findByProductIdAndLotNumberForUpdate(@Param("productId") Long productId, @Param("lotNumber") String lotNumber, @Param("tenantId") Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Batch b WHERE b.product.id = :productId AND b.tenantId = :tenantId ORDER BY b.createdAt ASC")
    List<Batch> findByProductIdOrderByCreatedAtAscForUpdate(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT b FROM Batch b WHERE b.expiryDate IS NOT NULL AND b.expiryDate <= :date AND b.quantity > 0 AND b.tenantId = :tenantId")
    List<Batch> findExpiredBatches(@Param("date") LocalDate date, @Param("tenantId") Long tenantId);

    @EntityGraph(attributePaths = {"product"})
    @Query("SELECT b FROM Batch b WHERE b.expiryDate IS NOT NULL AND b.expiryDate > :today AND b.expiryDate <= :threshold AND b.quantity > 0 AND b.tenantId = :tenantId")
    List<Batch> findExpiringBatches(@Param("today") LocalDate today, @Param("threshold") LocalDate threshold, @Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(b) FROM Batch b WHERE b.expiryDate IS NOT NULL AND b.expiryDate > :today AND b.expiryDate <= :threshold AND b.quantity > 0 AND b.tenantId = :tenantId")
    long countExpiringBatches(@Param("today") LocalDate today, @Param("threshold") LocalDate threshold, @Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(p.price * b.quantity), 0) FROM Batch b JOIN b.product p WHERE b.expiryDate IS NOT NULL AND b.expiryDate <= :date AND b.quantity > 0 AND b.tenantId = :tenantId")
    java.math.BigDecimal totalValueOfExpiredBatches(@Param("date") LocalDate date, @Param("tenantId") Long tenantId);

    @Query("SELECT b.product.id AS productId, MIN(b.expiryDate) AS nearestExpiryDate FROM Batch b WHERE b.expiryDate IS NOT NULL AND b.quantity > 0 AND b.tenantId = :tenantId GROUP BY b.product.id")
    List<NearestExpiryProjection> findNearestExpiryDateByProduct(@Param("tenantId") Long tenantId);

    interface NearestExpiryProjection {
        Long getProductId();
        LocalDate getNearestExpiryDate();
    }
}
