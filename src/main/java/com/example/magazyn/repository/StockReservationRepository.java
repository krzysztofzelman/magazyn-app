package com.example.magazyn.repository;

import com.example.magazyn.entity.ReservationStatus;
import com.example.magazyn.entity.StockReservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReservation r WHERE r.id = :id")
    Optional<StockReservation> findByIdForUpdate(@Param("id") Long id);

    List<StockReservation> findByProductIdAndStatus(Long productId, ReservationStatus status);

    List<StockReservation> findByProductId(Long productId);

    List<StockReservation> findByStatus(ReservationStatus status);

    @Query("SELECT r FROM StockReservation r WHERE r.status = :status AND r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    List<StockReservation> findByStatusAndExpiresAtBefore(
            @Param("status") ReservationStatus status,
            @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM StockReservation r WHERE r.product.id = :productId AND r.status = :status")
    Integer sumQuantityByProductIdAndStatus(
            @Param("productId") Long productId,
            @Param("status") ReservationStatus status);
}
