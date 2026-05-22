package com.example.magazyn.service;

import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public Optional<Product> getProductBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    public Product createProduct(Product product) {
        if (productRepository.findBySku(product.getSku()).isPresent()) {
            throw new RuntimeException("Product with SKU '" + product.getSku() + "' already exists");
        }
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, Product productDetails) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (productDetails.getName() != null) {
            existing.setName(productDetails.getName());
        }
        if (productDetails.getSku() != null) {
            if (!existing.getSku().equals(productDetails.getSku())
                    && productRepository.findBySku(productDetails.getSku()).isPresent()) {
                throw new RuntimeException("Product with SKU '" + productDetails.getSku() + "' already exists");
            }
            existing.setSku(productDetails.getSku());
        }
        if (productDetails.getDescription() != null) {
            existing.setDescription(productDetails.getDescription());
        }
        if (productDetails.getUnit() != null) {
            existing.setUnit(productDetails.getUnit());
        }

        return productRepository.save(existing);
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}
