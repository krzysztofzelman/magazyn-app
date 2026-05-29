package com.example.magazyn.service;

import com.example.magazyn.dto.CreateReservationRequest;
import com.example.magazyn.dto.ProductAvailabilityResponse;
import com.example.magazyn.dto.ReservationResponse;
import com.example.magazyn.entity.Product;
import com.example.magazyn.entity.ReservationReferenceType;
import com.example.magazyn.entity.ReservationStatus;
import com.example.magazyn.entity.StockReservation;
import com.example.magazyn.exception.InsufficientStockException;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.StockReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final StockReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    public ReservationService(StockReservationRepository reservationRepository,
                              ProductRepository productRepository,
                              AuditLogService auditLogService) {
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Creates a reservation after validating available stock.
     * Uses pessimistic lock on Product to prevent concurrent over-reservation.
     */
    public ReservationResponse reserve(CreateReservationRequest request, String username) {
        Product product = productRepository.findByIdForUpdate(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new InvalidOperationException("Quantity must be positive");
        }

        int reservedQuantity = getActiveReservedQuantity(product.getId());
        int availableQuantity = product.getQuantity() - reservedQuantity;

        if (availableQuantity < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient available stock — available: " + availableQuantity
                            + " (total: " + product.getQuantity()
                            + ", reserved: " + reservedQuantity
                            + "), requested: " + request.getQuantity());
        }

        StockReservation reservation = StockReservation.builder()
                .product(product)
                .quantity(request.getQuantity())
                .referenceType(request.getReferenceType())
                .referenceId(request.getReferenceId())
                .status(ReservationStatus.ACTIVE)
                .createdBy(username)
                .expiresAt(request.getExpiresAt())
                .notes(request.getNotes())
                .build();

        StockReservation saved = reservationRepository.save(reservation);
        auditLogService.log(username, "RESERVATION_CREATE", "StockReservation", saved.getId(),
                "Reserved qty=" + request.getQuantity() + " for product=" + product.getId()
                        + " (" + product.getName() + ")"
                        + " refType=" + request.getReferenceType()
                        + (request.getReferenceId() != null ? " refId=" + request.getReferenceId() : ""));

        return toResponse(saved);
    }

    /**
     * Releases (cancels) an active reservation.
     */
    public ReservationResponse release(Long reservationId, String username) {
        StockReservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidOperationException("Only ACTIVE reservations can be released. Current status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.RELEASED);
        StockReservation saved = reservationRepository.save(reservation);

        auditLogService.log(username, "RESERVATION_RELEASE", "StockReservation", saved.getId(),
                "Released reservation for product=" + saved.getProduct().getId()
                        + " qty=" + saved.getQuantity());

        return toResponse(saved);
    }

    /**
     * Fulfills an active reservation (e.g., when a WZ is confirmed).
     */
    public ReservationResponse fulfill(Long reservationId, String username) {
        StockReservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", reservationId));

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new InvalidOperationException("Only ACTIVE reservations can be fulfilled. Current status: " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.FULFILLED);
        StockReservation saved = reservationRepository.save(reservation);

        auditLogService.log(username, "RESERVATION_FULFILL", "StockReservation", saved.getId(),
                "Fulfilled reservation for product=" + saved.getProduct().getId()
                        + " qty=" + saved.getQuantity());

        return toResponse(saved);
    }

    /**
     * Fulfills active reservations for a product up to the given quantity (FIFO by createdAt).
     * Returns the total quantity of reservations fulfilled.
     */
    public int fulfillActiveReservations(Long productId, int quantity, String username) {
        List<StockReservation> active = reservationRepository.findByProductIdAndStatus(productId, ReservationStatus.ACTIVE);
        int remaining = quantity;
        int fulfilled = 0;

        for (StockReservation reservation : active) {
            if (remaining <= 0) break;

            int fulfillQty = Math.min(remaining, reservation.getQuantity());
            reservation.setQuantity(reservation.getQuantity() - fulfillQty);
            remaining -= fulfillQty;
            fulfilled += fulfillQty;

            if (reservation.getQuantity() == 0) {
                reservation.setStatus(ReservationStatus.FULFILLED);
            }
            reservationRepository.save(reservation);
        }

        if (fulfilled > 0) {
            auditLogService.log(username, "RESERVATION_FULFILL_BULK", "StockReservation", productId,
                    "Fulfilled " + fulfilled + " units from reservations for productId=" + productId);
        }

        return fulfilled;
    }

    /**
     * Returns the sum of quantities for all ACTIVE reservations on a product.
     */
    @Transactional(readOnly = true)
    public int getActiveReservedQuantity(Long productId) {
        Integer sum = reservationRepository.sumQuantityByProductIdAndStatus(productId, ReservationStatus.ACTIVE);
        return sum != null ? sum : 0;
    }

    /**
     * Returns the availability info for a product.
     */
    @Transactional(readOnly = true)
    public ProductAvailabilityResponse getProductAvailability(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        int reservedQuantity = getActiveReservedQuantity(productId);
        int availableQuantity = product.getQuantity() - reservedQuantity;

        return new ProductAvailabilityResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getQuantity(),
                reservedQuantity,
                Math.max(availableQuantity, 0)
        );
    }

    /**
     * Scheduled task: releases all expired ACTIVE reservations every hour.
     */
    @Scheduled(fixedRateString = "3600000") // 1 hour in ms
    public void releaseExpired() {
        List<StockReservation> expired = reservationRepository.findByStatusAndExpiresAtBefore(
                ReservationStatus.ACTIVE, LocalDateTime.now());

        for (StockReservation reservation : expired) {
            reservation.setStatus(ReservationStatus.RELEASED);
            reservation.setNotes(reservation.getNotes() != null
                    ? reservation.getNotes() + " [auto-released at expiry]"
                    : "[auto-released at expiry]");
            reservationRepository.save(reservation);

            log.info("Auto-released expired reservation id={} for productId={} qty={}",
                    reservation.getId(),
                    reservation.getProduct().getId(),
                    reservation.getQuantity());
        }

        if (!expired.isEmpty()) {
            auditLogService.log("SYSTEM", "RESERVATION_EXPIRE_RELEASE", "StockReservation", null,
                    "Auto-released " + expired.size() + " expired reservations");
        }
    }

    /**
     * Lists reservations with optional filtering by productId and status.
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservations(Long productId, ReservationStatus status) {
        List<StockReservation> reservations;

        if (productId != null && status != null) {
            reservations = reservationRepository.findByProductIdAndStatus(productId, status);
        } else if (productId != null) {
            reservations = reservationRepository.findByProductId(productId);
        } else if (status != null) {
            reservations = reservationRepository.findByStatus(status);
        } else {
            reservations = reservationRepository.findAll();
        }

        return reservations.stream()
                .map(this::toResponse)
                .toList();
    }

    private ReservationResponse toResponse(StockReservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getProduct().getId(),
                r.getProduct().getName(),
                r.getProduct().getSku(),
                r.getQuantity(),
                r.getReferenceType(),
                r.getReferenceId(),
                r.getStatus(),
                r.getCreatedBy(),
                r.getCreatedAt(),
                r.getExpiresAt(),
                r.getNotes()
        );
    }
}
