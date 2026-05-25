package com.example.magazyn.service;

import com.example.magazyn.dto.CreateProductRequest;
import com.example.magazyn.dto.ProductResponse;
import com.example.magazyn.dto.UpdateProductRequest;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable, String search) {
        if (search == null || search.isBlank()) {
            return productRepository.findAll(pageable)
                    .map(this::toResponse);
        }
        return productRepository.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(search, search, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductById(Long id) {
        return productRepository.findById(id)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> getProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(this::toResponse);
    }

    public ProductResponse createProduct(CreateProductRequest request) {
        if (productRepository.findBySku(request.getSku()).isPresent()) {
            throw new RuntimeException("Product with SKU '" + request.getSku() + "' already exists");
        }

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .description(request.getDescription())
                .unit(request.getUnit())
                .quantity(0)
                .build();

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getSku() != null) {
            if (!existing.getSku().equals(request.getSku())
                    && productRepository.findBySku(request.getSku()).isPresent()) {
                throw new RuntimeException("Product with SKU '" + request.getSku() + "' already exists");
            }
            existing.setSku(request.getSku());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getUnit() != null) {
            existing.setUnit(request.getUnit());
        }

        Product saved = productRepository.save(existing);
        return toResponse(saved);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getUnit(),
                product.getQuantity(),
                product.getCreatedAt()
        );
    }
}
