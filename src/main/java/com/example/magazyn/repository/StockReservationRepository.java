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

    Optional<StockReservation> findByIdAndTenantId(Long id, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReservation r WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<StockReservation> findByIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReservation r WHERE r.product.id = :productId AND r.status = :status AND r.tenantId = :tenantId")
    List<StockReservation> findByProductIdAndStatusForUpdate(@Param("productId") Long productId, @Param("status") ReservationStatus status, @Param("tenantId") Long tenantId);

    List<StockReservation> findByProductIdAndStatusAndTenantId(Long productId, ReservationStatus status, Long tenantId);

    List<StockReservation> findByProductIdAndTenantId(Long productId, Long tenantId);

    List<StockReservation> findByStatusAndTenantId(ReservationStatus status, Long tenantId);

    List<StockReservation> findAllByTenantId(Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM StockReservation r WHERE r.status = :status AND r.expiresAt IS NOT NULL AND r.expiresAt < :now AND r.tenantId = :tenantId")
    List<StockReservation> findByStatusAndExpiresAtBeforeForUpdate(
            @Param("status") ReservationStatus status,
            @Param("now") LocalDateTime now,
            @Param("tenantId") Long tenantId);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM StockReservation r WHERE r.product.id = :productId AND r.status = :status AND r.tenantId = :tenantId")
    Integer sumQuantityByProductIdAndStatus(
            @Param("productId") Long productId,
            @Param("status") ReservationStatus status,
            @Param("tenantId") Long tenantId);
}
