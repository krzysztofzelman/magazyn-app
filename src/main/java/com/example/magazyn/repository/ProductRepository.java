package com.example.magazyn.repository;

import com.example.magazyn.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    Optional<Product> findBySkuAndTenantId(String sku, Long tenantId);

    Optional<Product> findByBarcodeAndTenantId(String barcode, Long tenantId);

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String name, String sku, Pageable pageable);

    Optional<Product> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByIdAndTenantId(Long id, Long tenantId);

    List<Product> findByTenantId(Long tenantId);

    Page<Product> findAllByTenantId(Long tenantId, Pageable pageable);

    long countByTenantId(Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.tenantId = :tenantId")
    Optional<Product> findByIdForUpdate(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.quantity < p.minQuantity AND p.tenantId = :tenantId ORDER BY (p.minQuantity - p.quantity) DESC")
    List<Product> findProductsBelowMinStock(@Param("tenantId") Long tenantId);

    List<Product> findByLocationIdAndTenantId(Long locationId, Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchByNameOrSku(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.quantity * p.price), 0) FROM Product p WHERE p.tenantId = :tenantId")
    BigDecimal getTotalStockValue(@Param("tenantId") Long tenantId);
}
