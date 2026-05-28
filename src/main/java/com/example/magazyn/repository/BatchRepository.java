package com.example.magazyn.repository;

import com.example.magazyn.entity.Batch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {

    List<Batch> findByProductIdOrderByExpiryDateAsc(Long productId);

    List<Batch> findByProductIdOrderByCreatedAtAsc(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Batch b WHERE b.id = :id")
    Optional<Batch> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT b FROM Batch b WHERE b.expiryDate IS NOT NULL AND b.expiryDate <= :date AND b.quantity > 0")
    List<Batch> findExpiredBatches(@Param("date") LocalDate date);

    @Query("SELECT b FROM Batch b WHERE b.expiryDate IS NOT NULL AND b.expiryDate > :today AND b.expiryDate <= :threshold AND b.quantity > 0")
    List<Batch> findExpiringBatches(@Param("today") LocalDate today, @Param("threshold") LocalDate threshold);

    @Query("SELECT COUNT(b) FROM Batch b WHERE b.expiryDate IS NOT NULL AND b.expiryDate > :today AND b.expiryDate <= :threshold AND b.quantity > 0")
    long countExpiringBatches(@Param("today") LocalDate today, @Param("threshold") LocalDate threshold);

    @Query("SELECT COALESCE(SUM(p.price * b.quantity), 0) FROM Batch b JOIN b.product p WHERE b.expiryDate IS NOT NULL AND b.expiryDate <= :date AND b.quantity > 0")
    java.math.BigDecimal totalValueOfExpiredBatches(@Param("date") LocalDate date);

    @Query("SELECT b.product.id, MIN(b.expiryDate) FROM Batch b WHERE b.expiryDate IS NOT NULL AND b.quantity > 0 GROUP BY b.product.id")
    List<Object[]> findNearestExpiryDateByProduct();
}
