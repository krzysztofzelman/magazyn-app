package com.example.magazyn.service;

import com.example.magazyn.dto.AssignLocationRequest;
import com.example.magazyn.dto.CreateProductRequest;
import com.example.magazyn.dto.ProductResponse;
import com.example.magazyn.dto.UpdateProductRequest;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.service.BatchService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private ReservationService reservationService;

    @Mock
    private BatchService batchService;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        lenient().when(reservationService.getActiveReservedQuantity(anyLong())).thenReturn(0);
        lenient().when(batchService.getNearestExpiryDateByProduct()).thenReturn(java.util.Map.of());
    }

    private Product createProductEntity(Long id, String name, String sku, int quantity) {
        return Product.builder()
                .id(id)
                .name(name)
                .sku(sku)
                .description("Description of " + name)
                .unit("szt.")
                .quantity(quantity)
                .price(BigDecimal.valueOf(10.00))
                .minQuantity(5)
                .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();
    }

    private CreateProductRequest createRequest(String name, String sku) {
        CreateProductRequest request = new CreateProductRequest();
        request.setName(name);
        request.setSku(sku);
        request.setDescription("Description");
        request.setUnit("szt.");
        request.setPrice(BigDecimal.valueOf(100.00));
        request.setMinQuantity(5);
        return request;
    }

    // ──────────────────────────────────────────────
    // getAllProducts
    // ──────────────────────────────────────────────

    @Test
    void getAllProducts_withoutSearch_returnsAllPaged() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("name"));
        Product p1 = createProductEntity(1L, "Product A", "SKU-A", 10);
        Product p2 = createProductEntity(2L, "Product B", "SKU-B", 5);
        Page<Product> productPage = new PageImpl<>(List.of(p1, p2), pageable, 2);

        when(productRepository.findAll(pageable)).thenReturn(productPage);

        Page<ProductResponse> result = productService.getAllProducts(pageable, null);

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals("Product A", result.getContent().get(0).getName());
        assertEquals("Product B", result.getContent().get(1).getName());
        verify(productRepository).findAll(pageable);
        verify(counter).increment();
    }

    @Test
    void getAllProducts_withSearch_returnsFiltered() {
        Pageable pageable = PageRequest.of(0, 10);
        Product p1 = createProductEntity(1L, "Widget", "WDG-001", 10);
        Page<Product> productPage = new PageImpl<>(List.of(p1), pageable, 1);

        when(productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase("widget", "widget", pageable))
                .thenReturn(productPage);

        Page<ProductResponse> result = productService.getAllProducts(pageable, "widget");

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Widget", result.getContent().get(0).getName());
        verify(productRepository).findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase("widget", "widget", pageable);
    }

    @Test
    void getAllProducts_withBlankSearch_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<ProductResponse> result = productService.getAllProducts(pageable, "   ");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository).findAll(pageable);
    }

    // ──────────────────────────────────────────────
    // createProduct
    // ──────────────────────────────────────────────

    @Test
    void createProduct_success() {
        CreateProductRequest request = createRequest("Test Product", "TST-001");
        Product savedProduct = createProductEntity(1L, "Test Product", "TST-001", 0);

        when(productRepository.findBySku("TST-001")).thenReturn(Optional.empty());
        savedProduct.setPrice(BigDecimal.valueOf(100.00));
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Product", response.getName());
        assertEquals("TST-001", response.getSku());
        assertEquals("szt.", response.getUnit());
        assertNotNull(response.getCreatedAt());
        assertEquals(BigDecimal.valueOf(100.00), response.getPrice());
        assertEquals(Integer.valueOf(5), response.getMinQuantity());

        verify(productRepository).findBySku("TST-001");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_duplicateSku_throws() {
        CreateProductRequest request = createRequest("Test Product", "TST-001");
        Product existing = createProductEntity(1L, "Existing", "TST-001", 0);

        when(productRepository.findBySku("TST-001")).thenReturn(Optional.of(existing));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.createProduct(request));
        assertTrue(exception.getMessage().contains("already exists"));

        verify(productRepository).findBySku("TST-001");
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_defaultsPriceAndMinQuantityWhenNull() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Product");
        request.setSku("PRD-001");
        request.setUnit("szt.");

        Product savedProduct = Product.builder()
                .id(1L)
                .name("Product")
                .sku("PRD-001")
                .unit("szt.")
                .quantity(0)
                .price(BigDecimal.ZERO)
                .minQuantity(0)
                .createdAt(LocalDateTime.now())
                .build();

        when(productRepository.findBySku("PRD-001")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals(BigDecimal.ZERO, response.getPrice());
        assertEquals(Integer.valueOf(0), response.getMinQuantity());
    }

    // ──────────────────────────────────────────────
    // updateProduct
    // ──────────────────────────────────────────────

    @Test
    void updateProduct_updatesAllFields() {
        Product existing = createProductEntity(1L, "Old Name", "OLD-SKU", 10);
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("New Name");
        request.setSku("NEW-SKU");
        request.setDescription("New description");
        request.setUnit("kg");
        request.setPrice(BigDecimal.valueOf(50.00));
        request.setMinQuantity(3);

        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(existing));
        when(productRepository.findBySku("NEW-SKU")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse response = productService.updateProduct(1L, request);

        assertEquals("New Name", response.getName());
        assertEquals("NEW-SKU", response.getSku());
        assertEquals("kg", response.getUnit());
        assertEquals(BigDecimal.valueOf(50.00), response.getPrice());
        assertEquals(Integer.valueOf(3), response.getMinQuantity());
    }

    @Test
    void updateProduct_partialUpdate_onlyChangesProvidedFields() {
        Product existing = createProductEntity(1L, "Product", "PRD-001", 10);
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Name");

        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse response = productService.updateProduct(1L, request);

        assertEquals("Updated Name", response.getName());
        assertEquals("PRD-001", response.getSku()); // unchanged
    }

    @Test
    void updateProduct_duplicateSku_throws() {
        Product existing = createProductEntity(1L, "Product A", "SKU-A", 10);
        Product otherProduct = createProductEntity(2L, "Product B", "SKU-B", 5);
        UpdateProductRequest request = new UpdateProductRequest();
        request.setSku("SKU-B");

        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(existing));
        when(productRepository.findBySku("SKU-B")).thenReturn(Optional.of(otherProduct));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.updateProduct(1L, request));
        assertTrue(exception.getMessage().contains("already exists"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_notFound_throws() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Name");

        when(productRepository.findByIdAndTenantId(eq(999L), any())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.updateProduct(999L, request));
        assertTrue(exception.getMessage().contains("not found"));

        verify(productRepository).findByIdAndTenantId(eq(999L), any());
        verify(productRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // deleteProduct
    // ──────────────────────────────────────────────

    @Test
    void deleteProduct_success() {
        Product product = createProductEntity(1L, "Product", "PRD-001", 10);
        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(product));
        doNothing().when(auditLogService).log(anyString(), anyString(), anyString(), any(), anyString());

        productService.deleteProduct(1L);

        verify(productRepository).findByIdAndTenantId(eq(1L), any());
        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_notFound_throws() {
        when(productRepository.findByIdAndTenantId(eq(999L), any())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.deleteProduct(999L));
        assertTrue(exception.getMessage().contains("not found"));

        verify(productRepository).findByIdAndTenantId(eq(999L), any());
        verify(productRepository, never()).delete(any());
    }

    // ──────────────────────────────────────────────
    // assignLocation
    // ──────────────────────────────────────────────

    @Test
    void assignLocation_success() {
        Product product = createProductEntity(1L, "Product", "PRD-001", 10);
        AssignLocationRequest request = new AssignLocationRequest();
        request.setLocationId(5L);

        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse response = productService.assignLocation(1L, request);

        assertEquals(Long.valueOf(5L), response.getLocationId());
        assertEquals(Long.valueOf(5L), product.getLocationId());
        verify(productRepository).save(product);
    }

    @Test
    void assignLocation_clearLocation() {
        Product product = createProductEntity(1L, "Product", "PRD-001", 10);
        product.setLocationId(3L);
        AssignLocationRequest request = new AssignLocationRequest();
        request.setLocationId(null);

        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse response = productService.assignLocation(1L, request);

        assertNull(response.getLocationId());
    }

    @Test
    void assignLocation_productNotFound_throws() {
        AssignLocationRequest request = new AssignLocationRequest();
        request.setLocationId(5L);

        when(productRepository.findByIdAndTenantId(eq(999L), any())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.assignLocation(999L, request));
        assertTrue(exception.getMessage().contains("not found"));
    }

    // ──────────────────────────────────────────────
    // getProductById / getProductBySku
    // ──────────────────────────────────────────────

    @Test
    void getProductById_found() {
        Product product = createProductEntity(1L, "Product", "PRD-001", 10);
        when(productRepository.findByIdAndTenantId(eq(1L), any())).thenReturn(Optional.of(product));

        Optional<ProductResponse> result = productService.getProductById(1L);

        assertTrue(result.isPresent());
        assertEquals("Product", result.get().getName());
    }

    @Test
    void getProductById_notFound() {
        when(productRepository.findByIdAndTenantId(eq(999L), any())).thenReturn(Optional.empty());

        Optional<ProductResponse> result = productService.getProductById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getProductBySku_found() {
        Product product = createProductEntity(1L, "Product", "PRD-001", 10);
        when(productRepository.findBySku("PRD-001")).thenReturn(Optional.of(product));

        Optional<ProductResponse> result = productService.getProductBySku("PRD-001");

        assertTrue(result.isPresent());
        assertEquals("PRD-001", result.get().getSku());
    }

    // ──────────────────────────────────────────────
    // getProductByBarcode
    // ──────────────────────────────────────────────

    @Test
    void getProductByBarcode_found() {
        Product product = createProductEntity(1L, "Product", "PRD-001", 10);
        product.setBarcode("5901234567890");
        when(productRepository.findByBarcode("5901234567890")).thenReturn(Optional.of(product));

        Optional<ProductResponse> result = productService.getProductByBarcode("5901234567890");

        assertTrue(result.isPresent());
        assertEquals("5901234567890", result.get().getBarcode());
        assertEquals("Product", result.get().getName());
    }

    @Test
    void getProductByBarcode_notFound() {
        when(productRepository.findByBarcode("INVALID-BARCODE")).thenReturn(Optional.empty());

        Optional<ProductResponse> result = productService.getProductByBarcode("INVALID-BARCODE");

        assertTrue(result.isEmpty());
    }

    // ──────────────────────────────────────────────
    // getProductsByLocation
    // ──────────────────────────────────────────────

    @Test
    void getProductsByLocation_returnsList() {
        Product p1 = createProductEntity(1L, "Product A", "A-001", 5);
        Product p2 = createProductEntity(2L, "Product B", "B-001", 10);
        when(productRepository.findByLocationId(10L)).thenReturn(List.of(p1, p2));

        List<ProductResponse> result = productService.getProductsByLocation(10L);

        assertEquals(2, result.size());
        assertEquals("Product A", result.get(0).getName());
        assertEquals("Product B", result.get(1).getName());
    }

    @Test
    void getProductsByLocation_emptyList() {
        when(productRepository.findByLocationId(999L)).thenReturn(List.of());

        List<ProductResponse> result = productService.getProductsByLocation(999L);

        assertTrue(result.isEmpty());
    }
}
