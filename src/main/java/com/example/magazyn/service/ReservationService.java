package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.CreateReservationRequest;
import com.example.magazyn.dto.ProductAvailabilityResponse;
import com.example.magazyn.dto.ReservationResponse;
import com.example.magazyn.entity.Product;
import com.example.magazyn.entity.ReservationReferenceType;
import com.example.magazyn.entity.ReservationStatus;
import com.example.magazyn.entity.StockReservation;
import com.example.magazyn.entity.Tenant;
import com.example.magazyn.exception.InsufficientStockException;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.StockReservationRepository;
import com.example.magazyn.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final StockReservationRepository reservationRepository;
    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;
    private final TenantRepository tenantRepository;

    public ReservationService(StockReservationRepository reservationRepository,
                              ProductRepository productRepository,
                              AuditLogService auditLogService,
                              TenantRepository tenantRepository) {
        this.reservationRepository = reservationRepository;
        this.productRepository = productRepository;
        this.auditLogService = auditLogService;
        this.tenantRepository = tenantRepository;
    }

    /**
     * Creates a reservation after validating available stock.
     * Uses pessimistic lock on Product to prevent concurrent over-reservation.
     */
    public ReservationResponse reserve(CreateReservationRequest request, String username) {
        Long tenantId = TenantContext.getTenantId();
        Product product = productRepository.findByIdForUpdate(request.getProductId(), tenantId)
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
        Long tenantId = TenantContext.getTenantId();
        StockReservation reservation = reservationRepository.findByIdForUpdate(reservationId, tenantId)
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
        Long tenantId = TenantContext.getTenantId();
        StockReservation reservation = reservationRepository.findByIdForUpdate(reservationId, tenantId)
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
     * Uses PESSIMISTIC_WRITE on the reservation list to prevent concurrent modification.
     * Returns the total quantity of reservations fulfilled.
     */
    public int fulfillActiveReservations(Long productId, int quantity, String username) {
        Long tenantId = TenantContext.getTenantId();
        List<StockReservation> active = reservationRepository.findByProductIdAndStatusForUpdate(productId, ReservationStatus.ACTIVE, tenantId);
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
        Long tenantId = TenantContext.getTenantId();
        Integer sum = reservationRepository.sumQuantityByProductIdAndStatus(productId, ReservationStatus.ACTIVE, tenantId);
        return sum != null ? sum : 0;
    }

    /**
     * Returns the availability info for a product.
     */
    @Transactional(readOnly = true)
    public ProductAvailabilityResponse getProductAvailability(Long productId) {
        Product product = productRepository.findByIdAndTenantId(productId, TenantContext.getTenantId())
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
     * Iterates over all active tenants since there is no HTTP request context in @Scheduled methods.
     * Uses PESSIMISTIC_WRITE to lock each expired reservation against concurrent fulfill/release.
     */
    @Scheduled(fixedRateString = "3600000") // 1 hour in ms
    public void releaseExpired() {
        List<Tenant> activeTenants = tenantRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .collect(Collectors.toList());

        if (activeTenants.isEmpty()) {
            log.info("No active tenants found — skipping expired reservation release");
            return;
        }

        int totalReleased = 0;

        for (Tenant tenant : activeTenants) {
            TenantContext.setTenantId(tenant.getId());
            try {
                List<StockReservation> expired = reservationRepository.findByStatusAndExpiresAtBeforeForUpdate(
                        ReservationStatus.ACTIVE, LocalDateTime.now(), tenant.getId());

                for (StockReservation reservation : expired) {
                    reservation.setStatus(ReservationStatus.RELEASED);
                    reservation.setNotes(reservation.getNotes() != null
                            ? reservation.getNotes() + " [auto-released at expiry]"
                            : "[auto-released at expiry]");

                    log.info("Auto-released expired reservation id={} for productId={} qty={}",
                            reservation.getId(),
                            reservation.getProduct().getId(),
                            reservation.getQuantity());
                }

                totalReleased += expired.size();
            } finally {
                TenantContext.clear();
            }
        }

        if (totalReleased > 0) {
            auditLogService.log("SYSTEM", "RESERVATION_EXPIRE_RELEASE", "StockReservation", null,
                    "Auto-released " + totalReleased + " expired reservations across "
                            + activeTenants.size() + " tenants");
        }
    }

    /**
     * Lists reservations with optional filtering by productId and status.
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservations(Long productId, ReservationStatus status) {
        Long tenantId = TenantContext.getTenantId();
        List<StockReservation> reservations;

        if (productId != null && status != null) {
            reservations = reservationRepository.findByProductIdAndStatusAndTenantId(productId, status, tenantId);
        } else if (productId != null) {
            reservations = reservationRepository.findByProductIdAndTenantId(productId, tenantId);
        } else if (status != null) {
            reservations = reservationRepository.findByStatusAndTenantId(status, tenantId);
        } else {
            reservations = reservationRepository.findAllByTenantId(tenantId);
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
