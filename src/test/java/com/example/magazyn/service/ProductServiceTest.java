package com.example.magazyn.service;

import com.example.magazyn.dto.AssignLocationRequest;
import com.example.magazyn.dto.CreateProductRequest;
import com.example.magazyn.dto.ProductResponse;
import com.example.magazyn.dto.UpdateProductRequest;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.ProductRepository;
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
    private com.example.magazyn.repository.LocationRepository locationRepository;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
    }

    private Product createProductEntity(Long id, String name, String sku, int quantity) {
        return Product.builder()
                .id(id)
                .name(name)
                .sku(sku)
                .description("Description of " + name)
                .unit("szt.")
                .quantity(quantity)
                .build();
    }

    private ProductResponse createProductResponse(Long id, String name, String sku) {
        return ProductResponse.builder()
                .id(id)
                .name(name)
                .sku(sku)
                .description("Description of " + name)
                .unit("szt.")
                .build();
    }

    // --- createProduct ---

    @Test
    void createProduct_success() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Test Product");
        request.setSku("TST-001");
        request.setDescription("A test product");
        request.setUnit("szt.");
        request.setQuantity(10);

        Product savedEntity = Product.builder()
                .id(1L)
                .name("Test Product")
                .sku("TST-001")
                .description("A test product")
                .unit("szt.")
                .quantity(10)
                .build();

        when(productRepository.existsBySku("TST-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(savedEntity);

        ProductResponse result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Product", result.getName());
        assertEquals("TST-001", result.getSku());
        assertEquals("szt.", result.getUnit());
        assertEquals(10, result.getQuantity());
    }

    @Test
    void createProduct_duplicateSku_throws() {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Test Product");
        request.setSku("TST-001");
        request.setDescription("A test product");
        request.setUnit("szt.");
        request.setQuantity(10);

        when(productRepository.existsBySku("TST-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> productService.createProduct(request));
    }

    // --- updateProduct ---

    @Test
    void updateProduct_updatesAllFields() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Name");
        request.setSku("UPD-001");
        request.setDescription("Updated description");
        request.setUnit("kg");
        request.setQuantity(20);

        Product existing = Product.builder()
                .id(1L)
                .name("Old Name")
                .sku("OLD-001")
                .description("Old description")
                .unit("szt.")
                .quantity(10)
                .build();

        Product updatedEntity = Product.builder()
                .id(1L)
                .name("Updated Name")
                .sku("UPD-001")
                .description("Updated description")
                .unit("kg")
                .quantity(20)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySku("UPD-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(updatedEntity);

        ProductResponse result = productService.updateProduct(1L, request);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals("UPD-001", result.getSku());
        assertEquals("kg", result.getUnit());
        assertEquals(20, result.getQuantity());
    }

    @Test
    void updateProduct_partialUpdate_onlyChangesProvidedFields() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Only Name Change");

        Product existing = Product.builder()
                .id(1L)
                .name("Old Name")
                .sku("OLD-001")
                .description("Old description")
                .unit("szt.")
                .quantity(10)
                .build();

        Product updatedEntity = Product.builder()
                .id(1L)
                .name("Only Name Change")
                .sku("OLD-001")
                .description("Old description")
                .unit("szt.")
                .quantity(10)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class))).thenReturn(updatedEntity);

        ProductResponse result = productService.updateProduct(1L, request);

        assertNotNull(result);
        assertEquals("Only Name Change", result.getName());
        assertEquals("OLD-001", result.getSku());
        assertEquals("szt.", result.getUnit());
        assertEquals(10, result.getQuantity());
    }

    @Test
    void updateProduct_notFound_throws() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.updateProduct(999L, new UpdateProductRequest()));
    }

    @Test
    void updateProduct_duplicateSku_throws() {
        UpdateProductRequest request = new UpdateProductRequest();
        request.setSku("TAKEN-001");

        Product existing = Product.builder()
                .id(1L)
                .name("Old Name")
                .sku("OLD-001")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySku("TAKEN-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct(1L, request));
    }

    // --- deleteProduct ---

    @Test
    void deleteProduct_success() {
        Product product = Product.builder()
                .id(1L)
                .name("To Delete")
                .sku("DEL-001")
                .quantity(5)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doNothing().when(productRepository).delete(product);

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProduct_notFound_throws() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.deleteProduct(999L));
    }

    // --- getProductById ---

    @Test
    void getProductById_found() {
        Product product = createProductEntity(1L, "Test Product", "TST-001", 10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductById(1L);

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        assertEquals("TST-001", result.getSku());
    }

    @Test
    void getProductById_notFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.getProductById(999L));
    }

    // --- getProductBySku ---

    @Test
    void getProductBySku_found() {
        Product product = createProductEntity(1L, "Sku Product", "SKU-001", 5);
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));

        ProductResponse result = productService.getProductBySku("SKU-001");

        assertNotNull(result);
        assertEquals("Sku Product", result.getName());
        assertEquals("SKU-001", result.getSku());
    }

    @Test
    void getProductBySku_notFound() {
        when(productRepository.findBySku("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.getProductBySku("NONEXISTENT"));
    }

    // --- getAllProducts (paged) ---

    @Test
    void getAllProducts_returnsPage() {
        List<Product> products = List.of(
                createProductEntity(1L, "Product A", "A-001", 5),
                createProductEntity(2L, "Product B", "B-001", 10)
        );
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 20), 2);

        when(productRepository.findAll(any(Pageable.class))).thenReturn(productPage);

        Page<ProductResponse> result = productService.getAllProducts(PageRequest.of(0, 20));

        assertNotNull(result);
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
    }

    // --- getProductsByLocation ---

    @Test
    void getProductsByLocation_returnsList() {
        Product product = createProductEntity(1L, "Located Product", "LOC-001", 5);
        when(productRepository.findByLocationId(1L)).thenReturn(List.of(product));

        List<ProductResponse> result = productService.getProductsByLocation(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Located Product", result.get(0).getName());
    }

    @Test
    void getProductsByLocation_emptyList() {
        when(productRepository.findByLocationId(1L)).thenReturn(List.of());

        List<ProductResponse> result = productService.getProductsByLocation(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- assignLocation ---

    @Test
    void assignLocation_success() {
        AssignLocationRequest request = new AssignLocationRequest();
        request.setLocationId(5L);

        Product product = Product.builder()
                .id(1L)
                .name("Product")
                .sku("P-001")
                .quantity(10)
                .build();

        com.example.magazyn.entity.Location location = com.example.magazyn.entity.Location.builder()
                .id(5L)
                .name("Shelf A1")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(5L)).thenReturn(Optional.of(location));
        when(productRepository.save(any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse result = productService.assignLocation(1L, request);

        assertNotNull(result);
        assertEquals(5L, result.getLocationId());
    }

    @Test
    void assignLocation_productNotFound_throws() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        AssignLocationRequest request = new AssignLocationRequest();
        request.setLocationId(1L);

        assertThrows(RuntimeException.class, () -> productService.assignLocation(999L, request));
    }

    @Test
    void assignLocation_locationNotFound_throws() {
        AssignLocationRequest request = new AssignLocationRequest();
        request.setLocationId(999L);

        Product product = Product.builder()
                .id(1L)
                .name("Product")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productService.assignLocation(1L, request));
    }

    // --- countProducts ---

    @Test
    void countProducts_returnsCount() {
        when(productRepository.count()).thenReturn(42L);

        long count = productService.countProducts();

        assertEquals(42L, count);
    }

    @Test
    void countProducts_zero() {
        when(productRepository.count()).thenReturn(0L);

        long count = productService.countProducts();

        assertEquals(0L, count);
    }

    // --- getRecentProducts ---

    @Test
    void getRecentProducts_returnsList() {
        List<Product> products = List.of(
                createProductEntity(1L, "Recent 1", "R-001", 5),
                createProductEntity(2L, "Recent 2", "R-002", 10)
        );

        when(productRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(products);

        List<ProductResponse> result = productService.getRecentProducts();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getRecentProducts_empty() {
        when(productRepository.findTop10ByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<ProductResponse> result = productService.getRecentProducts();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // --- getLowStockProducts ---

    @Test
    void getLowStockProducts_returnsList() {
        List<Product> products = List.of(
                createProductEntity(1L, "Low Stock", "LOW-001", 3)
        );

        when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(products);

        List<ProductResponse> result = productService.getLowStockProducts(5);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Low Stock", result.get(0).getName());
    }

    @Test
    void getLowStockProducts_empty() {
        when(productRepository.findByQuantityLessThanEqual(5)).thenReturn(List.of());

        List<ProductResponse> result = productService.getLowStockProducts(5);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
