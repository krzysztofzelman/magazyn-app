package com.example.magazyn.service;

import com.example.magazyn.dto.CreateProductRequest;
import com.example.magazyn.dto.ProductResponse;
import com.example.magazyn.dto.UpdateProductRequest;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_success() {
        // given
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Test Product");
        request.setSku("TST-001");
        request.setDescription("Description");
        request.setUnit("szt.");

        Product savedProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .sku("TST-001")
                .description("Description")
                .unit("szt.")
                .createdAt(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();

        when(productRepository.findBySku("TST-001")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        // when
        ProductResponse response = productService.createProduct(request);

        // then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Product", response.getName());
        assertEquals("TST-001", response.getSku());
        assertEquals("Description", response.getDescription());
        assertEquals("szt.", response.getUnit());
        assertNotNull(response.getCreatedAt());

        verify(productRepository).findBySku("TST-001");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_duplicateSku() {
        // given
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Test Product");
        request.setSku("TST-001");
        request.setUnit("szt.");

        Product existing = Product.builder()
                .id(1L)
                .name("Existing")
                .sku("TST-001")
                .unit("szt.")
                .build();

        when(productRepository.findBySku("TST-001")).thenReturn(Optional.of(existing));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.createProduct(request));
        assertTrue(exception.getMessage().contains("already exists"));

        verify(productRepository).findBySku("TST-001");
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateProduct_notFound() {
        // given
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("Updated Name");

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> productService.updateProduct(999L, request));
        assertTrue(exception.getMessage().contains("not found"));

        verify(productRepository).findById(999L);
        verify(productRepository, never()).save(any());
    }

    @Test
    void deleteProduct_success() {
        // given
        when(productRepository.existsById(1L)).thenReturn(true);

        // when
        productService.deleteProduct(1L);

        // then
        verify(productRepository).existsById(1L);
        verify(productRepository).deleteById(1L);
    }

    @Test
    void getAllProducts_returnsEmptyList() {
        // given
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // when
        List<ProductResponse> result = productService.getAllProducts();

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(productRepository).findAll();
    }
}
