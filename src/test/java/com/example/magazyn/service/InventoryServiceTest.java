package com.example.magazyn.service;

import com.example.magazyn.dto.InventoryItemResponse;
import com.example.magazyn.dto.InventoryReportResponse;
import com.example.magazyn.dto.InventoryScanRequest;
import com.example.magazyn.dto.InventorySessionRequest;
import com.example.magazyn.dto.InventorySessionResponse;
import com.example.magazyn.entity.InventoryItem;
import com.example.magazyn.entity.InventorySession;
import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationStock;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.InventoryItemRepository;
import com.example.magazyn.repository.InventorySessionRepository;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.repository.LocationStockRepository;
import com.example.magazyn.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventorySessionRepository sessionRepository;

    @Mock
    private InventoryItemRepository itemRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationStockRepository locationStockRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InventoryService inventoryService;

    private static final String USERNAME = "testuser";

    private Location warehouse;
    private Location shelfA;
    private Location shelfB;
    private Product productA;
    private Product productB;
    private LocationStock stockA1;
    private LocationStock stockB1;

    @BeforeEach
    void setUp() {
        warehouse = Location.builder()
                .id(1L).code("MG-01").name("Magazyn G\u0142\u00f3wny")
                .build();

        shelfA = Location.builder()
                .id(10L).code("MG-01-R01").name("Regal 1")
                .parentId(1L)
                .build();

        shelfB = Location.builder()
                .id(11L).code("MG-01-R02").name("Regal 2")
                .parentId(1L)
                .build();

        productA = Product.builder()
                .id(1L).name("Product A").sku("SKU-A").unit("szt.")
                .quantity(100).barcode("PROD-1")
                .build();

        productB = Product.builder()
                .id(2L).name("Product B").sku("SKU-B").unit("szt.")
                .quantity(50).barcode("PROD-2")
                .build();

        stockA1 = LocationStock.builder()
                .id(1L).locationId(10L).productId(1L)
                .quantity(BigDecimal.valueOf(20)).reservedQuantity(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now()).build();

        stockB1 = LocationStock.builder()
                .id(2L).locationId(11L).productId(2L)
                .quantity(BigDecimal.valueOf(15)).reservedQuantity(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now()).build();
    }

    // ──────────────────────────────────────────────
    // createSession
    // ──────────────────────────────────────────────

    @Test
    void createSession_autoPopulatesFromLocationStock() {
        InventorySessionRequest request = new InventorySessionRequest();
        request.setName("Inwentaryzacja Q1");
        request.setWarehouseId(1L);

        InventorySession savedSession = InventorySession.builder()
                .id(100L).name("Inwentaryzacja Q1").createdBy(USERNAME)
                .status("OPEN").warehouseId(1L).createdAt(LocalDateTime.now())
                .build();

        when(sessionRepository.save(any(InventorySession.class))).thenReturn(savedSession);
        when(locationRepository.findByParentIdAndTenantId(1L, anyLong())).thenReturn(Arrays.asList(shelfA, shelfB));
        when(locationRepository.findByParentIdAndTenantId(10L, anyLong())).thenReturn(List.of());
        when(locationRepository.findByParentIdAndTenantId(11L, anyLong())).thenReturn(List.of());
        when(locationStockRepository.findByLocationIdAndTenantId(10L, anyLong())).thenReturn(List.of(stockA1));
        when(locationStockRepository.findByLocationIdAndTenantId(11L, anyLong())).thenReturn(List.of(stockB1));
        when(itemRepository.saveAll(anyList())).thenReturn(List.of(
                InventoryItem.builder().id(1L).build(),
                InventoryItem.builder().id(2L).build()
        ));
        when(locationRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        InventorySessionResponse response = inventoryService.createSession(request, USERNAME);

        assertNotNull(response);
        assertEquals("Inwentaryzacja Q1", response.getName());
        assertEquals("OPEN", response.getStatus());
        assertEquals("Magazyn G\u0142\u00f3wny", response.getWarehouseName());
        assertEquals(Integer.valueOf(2), response.getItemCount());
        verify(sessionRepository).save(any(InventorySession.class));
        verify(itemRepository).saveAll(anyList());
        verify(auditLogService).log(eq(USERNAME), eq("INVENTORY_CREATE"), eq("InventorySession"), eq(100L), anyString());
    }

    @Test
    void createSession_noWarehouse_populatesAllStock() {
        InventorySessionRequest request = new InventorySessionRequest();
        request.setName("Inwentaryzacja globalna");

        InventorySession savedSession = InventorySession.builder()
                .id(101L).name("Inwentaryzacja globalna").createdBy(USERNAME)
                .status("OPEN").createdAt(LocalDateTime.now())
                .build();

        when(sessionRepository.save(any(InventorySession.class))).thenReturn(savedSession);
        when(locationStockRepository.findByTenantId(anyLong())).thenReturn(Arrays.asList(stockA1, stockB1));
        when(itemRepository.saveAll(anyList())).thenReturn(List.of(
                InventoryItem.builder().id(1L).build(),
                InventoryItem.builder().id(2L).build()
        ));

        InventorySessionResponse response = inventoryService.createSession(request, USERNAME);

        assertNotNull(response);
        assertEquals("Inwentaryzacja globalna", response.getName());
        assertEquals(Integer.valueOf(2), response.getItemCount());
        verify(locationStockRepository).findByTenantId(anyLong());
    }

    // ──────────────────────────────────────────────
    // getSession
    // ──────────────────────────────────────────────

    @Test
    void getSession_returnsSessionWithItemCount() {
        InventorySession session = InventorySession.builder()
                .id(100L).name("Test Session").createdBy(USERNAME)
                .status("OPEN").createdAt(LocalDateTime.now())
                .build();

        when(sessionRepository.findByIdAndTenantId(eq(100L), any())).thenReturn(Optional.of(session));
        when(itemRepository.findBySessionIdAndTenantId(100L, anyLong())).thenReturn(List.of(
                InventoryItem.builder().id(1L).build(),
                InventoryItem.builder().id(2L).build()
        ));

        InventorySessionResponse response = inventoryService.getSession(100L);

        assertEquals(Integer.valueOf(2), response.getItemCount());
    }

    @Test
    void getSession_notFound_throwsException() {
        when(sessionRepository.findByIdAndTenantId(eq(99L), any())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> inventoryService.getSession(99L));
    }

    // ──────────────────────────────────────────────
    // scan
    // ──────────────────────────────────────────────

    @Test
    void scan_createsNewInventoryItem() {
        InventorySession session = InventorySession.builder()
                .id(100L).name("Test").status("OPEN").build();
        InventoryScanRequest request = new InventoryScanRequest();
        request.setLocationBarcode("LOC-MG01-R01");
        request.setProductBarcode("PROD-1");
        request.setQuantity(25.0);

        when(sessionRepository.findByIdAndTenantId(eq(100L), any())).thenReturn(Optional.of(session));
        when(locationRepository.findByBarcodeAndTenantId("LOC-MG01-R01", anyLong())).thenReturn(Optional.of(shelfA));
        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(productA));
        when(itemRepository.findBySessionIdAndLocationIdAndProductIdAndTenantId(100L, 10L, 1L, anyLong()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(InventoryItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InventoryItemResponse response = inventoryService.scan(100L, request, USERNAME);

        assertNotNull(response);
        assertEquals(10L, response.getLocationId().longValue());
        assertEquals(1L, response.getProductId().longValue());
        assertEquals("Product A", response.getProductName());
        assertEquals(BigDecimal.valueOf(25.0), response.getCountedQuantity());
        assertNotNull(response.getScannedAt());
        assertEquals(USERNAME, response.getScannedBy());
        verify(itemRepository).save(any(InventoryItem.class));
        verify(auditLogService).log(eq(USERNAME), eq("INVENTORY_SCAN"), eq("InventoryItem"), any(), anyString());
    }

    @Test
    void scan_updatesExistingInventoryItem() {
        InventorySession session = InventorySession.builder()
                .id(100L).name("Test").status("OPEN").build();
        InventoryScanRequest request = new InventoryScanRequest();
        request.setLocationBarcode("LOC-MG01-R01");
        request.setProductBarcode("PROD-1");
        request.setQuantity(30.0);

        InventoryItem existing = InventoryItem.builder()
                .id(1L).sessionId(100L).locationId(10L).productId(1L)
                .expectedQuantity(BigDecimal.valueOf(20)).countedQuantity(BigDecimal.valueOf(22))
                .build();

        when(sessionRepository.findByIdAndTenantId(eq(100L), any())).thenReturn(Optional.of(session));
        when(locationRepository.findByBarcodeAndTenantId("LOC-MG01-R01", anyLong())).thenReturn(Optional.of(shelfA));
        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(productA));
        when(itemRepository.findBySessionIdAndLocationIdAndProductIdAndTenantId(100L, 10L, 1L, anyLong()))
                .thenReturn(Optional.of(existing));
        when(itemRepository.save(any(InventoryItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InventoryItemResponse response = inventoryService.scan(100L, request, USERNAME);

        assertEquals(BigDecimal.valueOf(30.0), response.getCountedQuantity());
        assertEquals(BigDecimal.valueOf(20), response.getExpectedQuantity());
        assertEquals(BigDecimal.valueOf(10.0), response.getDifference());
    }

    @Test
    void scan_findsProductBySku() {
        InventorySession session = InventorySession.builder()
                .id(100L).name("Test").status("OPEN").build();
        InventoryScanRequest request = new InventoryScanRequest();
        request.setLocationBarcode("LOC-MG01-R02");
        request.setProductBarcode("SKU-B");
        request.setQuantity(10.0);

        when(sessionRepository.findByIdAndTenantId(eq(100L), any())).thenReturn(Optional.of(session));
        when(locationRepository.findByBarcodeAndTenantId("LOC-MG01-R02", anyLong())).thenReturn(Optional.of(shelfB));
        when(productRepository.findByBarcodeAndTenantId("SKU-B", anyLong())).thenReturn(Optional.empty());
        when(productRepository.findBySkuAndTenantId("SKU-B", anyLong())).thenReturn(Optional.of(productB));
        when(itemRepository.findBySessionIdAndLocationIdAndProductIdAndTenantId(100L, 11L, 2L, anyLong()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(InventoryItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        InventoryItemResponse response = inventoryService.scan(100L, request, USERNAME);

        assertEquals("Product B", response.getProductName());
        assertEquals(BigDecimal.valueOf(10.0), response.getCountedQuantity());
    }

    @Test
    void scan_closedSession_throwsException() {
        InventorySession session = InventorySession.builder()
                .id(100L).name("Test").status("CLOSED").build();
        when(sessionRepository.findByIdAndTenantId(eq(100L), any())).thenReturn(Optional.of(session));

        InventoryScanRequest request = new InventoryScanRequest();
        assertThrows(RuntimeException.class,
                () -> inventoryService.scan(100L, request, USERNAME));
    }

    // ──────────────────────────────────────────────
    // getReport
    // ──────────────────────────────────────────────

    @Test
    void getReport_returnsItemsWithDifferences() {
        InventorySession session = InventorySession.builder()
                .id(100L).name("Raport test").status("OPEN").build();

        InventoryItem item1 = InventoryItem.builder()
                .id(1L).sessionId(100L).locationId(10L).productId(1L)
                .expectedQuantity(BigDecimal.valueOf(20)).countedQuantity(BigDecimal.valueOf(22))
                .scannedAt(LocalDateTime.now()).scannedBy(USERNAME)
                .build();

        InventoryItem item2 = InventoryItem.builder()
                .id(2L).sessionId(100L).locationId(11L).productId(2L)
                .expectedQuantity(BigDecimal.valueOf(15)).countedQuantity(BigDecimal.valueOf(15))
                .scannedAt(LocalDateTime.now()).scannedBy(USERNAME)
                .build();

        when(sessionRepository.findByIdAndTenantId(eq(100L), any())).thenReturn(Optional.of(session));
        when(itemRepository.findBySessionIdAndTenantId(100L, anyLong())).thenReturn(Arrays.asList(item1, item2));
        when(locationRepository.findAllById(List.of(10L, 11L))).thenReturn(List.of(shelfA, shelfB));
        when(productRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(productA, productB));

        InventoryReportResponse report = inventoryService.getReport(100L);

        assertEquals(2, report.getItems().size());
        assertEquals(BigDecimal.valueOf(2), report.getItems().get(0).getDifference()); // 22-20
        assertEquals(BigDecimal.ZERO, report.getItems().get(1).getDifference());       // 15-15
        assertEquals(BigDecimal.valueOf(35), report.getTotalExpected());                // 20+15
        assertEquals(BigDecimal.valueOf(37), report.getTotalCounted());                 // 22+15
        assertEquals(BigDecimal.valueOf(2), report.getTotalDifference());               // 37-35
    }

    // ──────────────────────────────────────────────
    // closeSession
    // ──────────────────────────────────────────────

    @Test
    void closeSession_updatesLocationStockAndCloses() {
        InventorySession session = InventorySession.builder()
                .id(100L).name("Test").status("OPEN").createdAt(LocalDateTime.now())
                .build();

        InventoryItem item = InventoryItem.builder()
                .id(1L).sessionId(100L).locationId(10L).productId(1L)
                .expectedQuantity(BigDecimal.valueOf(20)).countedQuantity(BigDecimal.valueOf(22))
                .build();

        when(sessionRepository.findByIdAndTenantId(eq(100L), any())).thenReturn(Optional.of(session));
        when(itemRepository.findBySessionIdAndTenantId(100L, anyLong())).thenReturn(List.of(item));
        when(locationStockRepository.findByLocationIdAndProductIdAndTenantId(10L, 1L, anyLong()))
                .thenReturn(Optional.of(stockA1));
        when(locationStockRepository.findByLocationIdAndTenantId(10L, anyLong()))
                .thenReturn(List.of(stockA1));
        when(locationRepository.findById(10L)).thenReturn(Optional.of(shelfA));
        when(sessionRepository.save(any(InventorySession.class))).thenReturn(session);

        InventorySessionResponse response = inventoryService.closeSession(100L, USERNAME);

        assertEquals("CLOSED", response.getStatus());
        assertEquals(BigDecimal.valueOf(22), stockA1.getQuantity());
        verify(locationStockRepository).save(stockA1);
        verify(sessionRepository).save(any(InventorySession.class));
        verify(auditLogService).log(eq(USERNAME), eq("INVENTORY_CLOSE"), eq("InventorySession"), eq(100L), anyString());
    }

    @Test
    void closeSession_alreadyClosed_throwsException() {
        InventorySession session = InventorySession.builder()
                .id(100L).name("Test").status("CLOSED").build();
        when(sessionRepository.findByIdAndTenantId(eq(100L), any())).thenReturn(Optional.of(session));

        assertThrows(RuntimeException.class,
                () -> inventoryService.closeSession(100L, USERNAME));
    }
}
