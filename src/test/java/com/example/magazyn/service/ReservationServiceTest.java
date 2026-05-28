package com.example.magazyn.service;

import com.example.magazyn.dto.CreateReservationRequest;
import com.example.magazyn.dto.ProductAvailabilityResponse;
import com.example.magazyn.dto.ReservationResponse;
import com.example.magazyn.entity.Product;
import com.example.magazyn.entity.ReservationReferenceType;
import com.example.magazyn.entity.ReservationStatus;
import com.example.magazyn.entity.StockReservation;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.StockReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private StockReservationRepository reservationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReservationService reservationService;

    private static final String USERNAME = "testuser";

    private Product createProduct(Long id, String name, int quantity) {
        return Product.builder()
                .id(id)
                .name(name)
                .sku("SKU-" + id)
                .unit("szt.")
                .quantity(quantity)
                .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();
    }

    private CreateReservationRequest createRequest(Long productId, int quantity, ReservationReferenceType refType) {
        CreateReservationRequest request = new CreateReservationRequest();
        request.setProductId(productId);
        request.setQuantity(quantity);
        request.setReferenceType(refType);
        request.setReferenceId("REF-001");
        return request;
    }

    private StockReservation createReservation(Long id, Product product, int quantity, ReservationStatus status) {
        return StockReservation.builder()
                .id(id)
                .product(product)
                .quantity(quantity)
                .referenceType(ReservationReferenceType.ORDER)
                .referenceId("REF-001")
                .status(status)
                .createdBy(USERNAME)
                .createdAt(LocalDateTime.of(2025, 6, 1, 12, 0))
                .expiresAt(LocalDateTime.of(2025, 6, 2, 12, 0))
                .notes("Test reservation")
                .build();
    }

    // ──────────────────────────────────────────────
    // reserve — success cases
    // ──────────────────────────────────────────────

    @Test
    void reserve_withSufficientStock_createsReservation() {
        Product product = createProduct(1L, "Product A", 50);
        CreateReservationRequest request = createRequest(1L, 10, ReservationReferenceType.ORDER);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(reservationRepository.sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE)).thenReturn(0);
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(i -> {
            StockReservation r = i.getArgument(0);
            return StockReservation.builder()
                    .id(100L)
                    .product(r.getProduct())
                    .quantity(r.getQuantity())
                    .referenceType(r.getReferenceType())
                    .referenceId(r.getReferenceId())
                    .status(ReservationStatus.ACTIVE)
                    .createdBy(r.getCreatedBy())
                    .createdAt(LocalDateTime.now())
                    .expiresAt(r.getExpiresAt())
                    .notes(r.getNotes())
                    .build();
        });

        ReservationResponse response = reservationService.reserve(request, USERNAME);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(10, response.getQuantity());
        assertEquals(ReservationStatus.ACTIVE, response.getStatus());
        assertEquals(ReservationReferenceType.ORDER, response.getReferenceType());
        assertEquals("REF-001", response.getReferenceId());
        assertEquals(USERNAME, response.getCreatedBy());
        assertEquals(1L, response.getProductId());

        verify(productRepository).findByIdForUpdate(1L);
        verify(reservationRepository).sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE);
        verify(reservationRepository).save(any(StockReservation.class));
        verify(auditLogService).log(eq(USERNAME), eq("RESERVATION_CREATE"), eq("StockReservation"), eq(100L), anyString());
    }

    @Test
    void reserve_withPartialStock_createsReservation() {
        // 20 total, 5 reserved = 15 available, requesting 10 should succeed
        Product product = createProduct(1L, "Product A", 20);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(reservationRepository.sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE)).thenReturn(5);
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(i -> {
            StockReservation r = i.getArgument(0);
            return StockReservation.builder()
                    .id(101L)
                    .product(r.getProduct())
                    .quantity(r.getQuantity())
                    .referenceType(r.getReferenceType())
                    .referenceId(r.getReferenceId())
                    .status(ReservationStatus.ACTIVE)
                    .createdBy(r.getCreatedBy())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        ReservationResponse response = reservationService.reserve(createRequest(1L, 10, ReservationReferenceType.ORDER), USERNAME);

        assertNotNull(response);
        assertEquals(101L, response.getId());
        assertEquals(10, response.getQuantity());

        verify(productRepository).findByIdForUpdate(1L);
        verify(reservationRepository).sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE);
        verify(reservationRepository).save(any(StockReservation.class));
    }

    // ──────────────────────────────────────────────
    // reserve — insufficient stock
    // ──────────────────────────────────────────────

    @Test
    void reserve_withInsufficientStock_throws() {
        Product product = createProduct(1L, "Product A", 10);
        CreateReservationRequest request = createRequest(1L, 20, ReservationReferenceType.ORDER);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(reservationRepository.sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE)).thenReturn(0);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservationService.reserve(request, USERNAME));

        assertTrue(ex.getMessage().contains("Insufficient"));
        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex.getMessage().contains("20"));

        verify(productRepository).findByIdForUpdate(1L);
        verify(reservationRepository).sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_withPartlyReservedStock_insufficient_throws() {
        // 10 total, 8 already reserved = 2 available, requesting 5 should fail
        Product product = createProduct(1L, "Product A", 10);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(reservationRepository.sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE)).thenReturn(8);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservationService.reserve(createRequest(1L, 5, ReservationReferenceType.ORDER), USERNAME));

        assertTrue(ex.getMessage().contains("Insufficient"));
        assertTrue(ex.getMessage().contains("2"));
        assertTrue(ex.getMessage().contains("8"));
        assertTrue(ex.getMessage().contains("10"));

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_productNotFound_throws() {
        when(productRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservationService.reserve(createRequest(999L, 5, ReservationReferenceType.ORDER), USERNAME));

        assertTrue(ex.getMessage().contains("not found"));

        verify(productRepository).findByIdForUpdate(999L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void reserve_quantityNonPositive_throws() {
        Product product = createProduct(1L, "Product A", 50);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        CreateReservationRequest badRequest = createRequest(1L, 0, ReservationReferenceType.ORDER);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservationService.reserve(badRequest, USERNAME));

        assertTrue(ex.getMessage().contains("positive"));

        verify(reservationRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // release
    // ──────────────────────────────────────────────

    @Test
    void release_activeReservation_setsReleased() {
        Product product = createProduct(1L, "Product A", 50);
        StockReservation reservation = createReservation(1L, product, 10, ReservationStatus.ACTIVE);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.release(1L, USERNAME);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(ReservationStatus.RELEASED, response.getStatus());

        verify(reservationRepository).findByIdForUpdate(1L);
        verify(reservationRepository).save(reservation);
        verify(auditLogService).log(eq(USERNAME), eq("RESERVATION_RELEASE"), eq("StockReservation"), eq(1L), anyString());
    }

    @Test
    void release_nonActiveReservation_throws() {
        StockReservation reservation = createReservation(1L, createProduct(1L, "Product A", 50), 10, ReservationStatus.FULFILLED);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservationService.release(1L, USERNAME));

        assertTrue(ex.getMessage().contains("ACTIVE"));

        verify(reservationRepository).findByIdForUpdate(1L);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void release_notFound_throws() {
        when(reservationRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservationService.release(999L, USERNAME));

        assertTrue(ex.getMessage().contains("not found"));
    }

    // ──────────────────────────────────────────────
    // fulfill
    // ──────────────────────────────────────────────

    @Test
    void fulfill_activeReservation_setsFulfilled() {
        Product product = createProduct(1L, "Product A", 50);
        StockReservation reservation = createReservation(1L, product, 10, ReservationStatus.ACTIVE);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(i -> i.getArgument(0));

        ReservationResponse response = reservationService.fulfill(1L, USERNAME);

        assertNotNull(response);
        assertEquals(ReservationStatus.FULFILLED, response.getStatus());

        verify(reservationRepository).findByIdForUpdate(1L);
        verify(reservationRepository).save(reservation);
        verify(auditLogService).log(eq(USERNAME), eq("RESERVATION_FULFILL"), eq("StockReservation"), eq(1L), anyString());
    }

    @Test
    void fulfill_nonActiveReservation_throws() {
        StockReservation reservation = createReservation(1L, createProduct(1L, "Product A", 50), 10, ReservationStatus.RELEASED);

        when(reservationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reservation));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservationService.fulfill(1L, USERNAME));

        assertTrue(ex.getMessage().contains("ACTIVE"));
        verify(reservationRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // fulfillActiveReservations (bulk FIFO fulfill)
    // ──────────────────────────────────────────────

    @Test
    void fulfillActiveReservations_fulfillsFifo() {
        Product product = createProduct(1L, "Product A", 100);
        StockReservation r1 = createReservation(1L, product, 5, ReservationStatus.ACTIVE);
        StockReservation r2 = createReservation(2L, product, 10, ReservationStatus.ACTIVE);
        StockReservation r3 = createReservation(3L, product, 15, ReservationStatus.ACTIVE);

        when(reservationRepository.findByProductIdAndStatus(1L, ReservationStatus.ACTIVE))
                .thenReturn(List.of(r1, r2, r3));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(i -> i.getArgument(0));

        int fulfilled = reservationService.fulfillActiveReservations(1L, 18, USERNAME);

        assertEquals(18, fulfilled);
        // r1: 5 fulfilled -> status FULFILLED
        assertEquals(ReservationStatus.FULFILLED, r1.getStatus());
        assertEquals(0, r1.getQuantity());
        // r2: 10 fulfilled -> status FULFILLED
        assertEquals(ReservationStatus.FULFILLED, r2.getStatus());
        assertEquals(0, r2.getQuantity());
        // r3: 3 fulfilled (18 - 5 - 10 = 3) -> still ACTIVE with 12 remaining
        assertEquals(ReservationStatus.ACTIVE, r3.getStatus());
        assertEquals(12, r3.getQuantity());

        verify(reservationRepository).findByProductIdAndStatus(1L, ReservationStatus.ACTIVE);
        verify(reservationRepository, times(3)).save(any(StockReservation.class));
    }

    @Test
    void fulfillActiveReservations_noReservations_returnsZero() {
        when(reservationRepository.findByProductIdAndStatus(1L, ReservationStatus.ACTIVE))
                .thenReturn(List.of());

        int fulfilled = reservationService.fulfillActiveReservations(1L, 10, USERNAME);

        assertEquals(0, fulfilled);
        verify(reservationRepository).findByProductIdAndStatus(1L, ReservationStatus.ACTIVE);
        verify(reservationRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // releaseExpired — @Scheduled
    // ──────────────────────────────────────────────

    @Test
    void releaseExpired_releasesExpiredReservations() {
        Product product = createProduct(1L, "Product A", 50);
        StockReservation expired1 = createReservation(1L, product, 5, ReservationStatus.ACTIVE);
        expired1.setExpiresAt(LocalDateTime.of(2025, 1, 1, 12, 0));

        when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(expired1));
        when(reservationRepository.save(any(StockReservation.class))).thenAnswer(i -> i.getArgument(0));

        reservationService.releaseExpired();

        assertEquals(ReservationStatus.RELEASED, expired1.getStatus());
        assertTrue(expired1.getNotes().contains("auto-released"));

        verify(reservationRepository).findByStatusAndExpiresAtBefore(eq(ReservationStatus.ACTIVE), any(LocalDateTime.class));
        verify(reservationRepository).save(expired1);
    }

    @Test
    void releaseExpired_noExpiredReservations_doesNothing() {
        when(reservationRepository.findByStatusAndExpiresAtBefore(eq(ReservationStatus.ACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of());

        reservationService.releaseExpired();

        verify(reservationRepository).findByStatusAndExpiresAtBefore(eq(ReservationStatus.ACTIVE), any(LocalDateTime.class));
        verify(reservationRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // getActiveReservedQuantity
    // ──────────────────────────────────────────────

    @Test
    void getActiveReservedQuantity_returnsSum() {
        when(reservationRepository.sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE)).thenReturn(25);

        int result = reservationService.getActiveReservedQuantity(1L);

        assertEquals(25, result);
    }

    @Test
    void getActiveReservedQuantity_noReservations_returnsZero() {
        when(reservationRepository.sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE)).thenReturn(null);

        int result = reservationService.getActiveReservedQuantity(1L);

        assertEquals(0, result);
    }

    // ──────────────────────────────────────────────
    // getProductAvailability
    // ──────────────────────────────────────────────

    @Test
    void getProductAvailability_returnsCorrectValues() {
        Product product = createProduct(1L, "Product A", 100);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(reservationRepository.sumQuantityByProductIdAndStatus(1L, ReservationStatus.ACTIVE)).thenReturn(30);

        ProductAvailabilityResponse response = reservationService.getProductAvailability(1L);

        assertNotNull(response);
        assertEquals(1L, response.getProductId());
        assertEquals("Product A", response.getProductName());
        assertEquals(100, response.getQuantity());
        assertEquals(30, response.getReservedQuantity());
        assertEquals(70, response.getAvailableQuantity());
    }

    @Test
    void getProductAvailability_productNotFound_throws() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> reservationService.getProductAvailability(999L));

        assertTrue(ex.getMessage().contains("not found"));
    }

    // ──────────────────────────────────────────────
    // getReservations (listing with filters)
    // ──────────────────────────────────────────────

    @Test
    void getReservations_withProductAndStatus_returnsFiltered() {
        Product product = createProduct(1L, "Product A", 50);
        StockReservation r1 = createReservation(1L, product, 10, ReservationStatus.ACTIVE);

        when(reservationRepository.findByProductIdAndStatus(1L, ReservationStatus.ACTIVE))
                .thenReturn(List.of(r1));

        List<ReservationResponse> result = reservationService.getReservations(1L, ReservationStatus.ACTIVE);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getProductId());
        assertEquals(ReservationStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void getReservations_withoutFilters_returnsAll() {
        Product product = createProduct(1L, "Product A", 50);
        StockReservation r1 = createReservation(1L, product, 10, ReservationStatus.ACTIVE);
        StockReservation r2 = createReservation(2L, product, 5, ReservationStatus.FULFILLED);

        when(reservationRepository.findAll()).thenReturn(List.of(r1, r2));

        List<ReservationResponse> result = reservationService.getReservations(null, null);

        assertEquals(2, result.size());
    }
}
