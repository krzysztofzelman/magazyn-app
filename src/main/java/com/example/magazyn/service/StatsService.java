package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.StatsDashboardResponse;
import com.example.magazyn.dto.StatsDashboardResponse.ReorderAlert;
import com.example.magazyn.dto.StatsDashboardResponse.TopSellingProduct;
import com.example.magazyn.entity.MovementType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.BatchRepository;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.StockMovementRepository;
import com.example.magazyn.repository.TopSellingProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final BatchRepository batchRepository;

    @Autowired
    public StatsService(ProductRepository productRepository,
                        StockMovementRepository stockMovementRepository,
                        BatchRepository batchRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.batchRepository = batchRepository;
    }

    public StatsDashboardResponse getDashboard() {
        Long tenantId = TenantContext.getTenantId();
        long totalProducts = productRepository.count();

        BigDecimal totalStockValue = productRepository.getTotalStockValue(tenantId);

        List<TopSellingProduct> topSelling = getTopSellingProducts();

        List<ReorderAlert> reorderAlerts = getReorderAlerts(tenantId);

        // No sales module — return null for revenue
        BigDecimal revenueLast30Days = null;

        // Batch stats
        LocalDate today = LocalDate.now();
        long expiringBatchesCount = batchRepository.countExpiringBatches(today, today.plusDays(30));
        BigDecimal expiredBatchesValue = batchRepository.totalValueOfExpiredBatches(today);

        return new StatsDashboardResponse(
                totalProducts,
                totalStockValue,
                topSelling,
                reorderAlerts,
                revenueLast30Days,
                expiringBatchesCount,
                expiredBatchesValue
        );
    }

    private List<TopSellingProduct> getTopSellingProducts() {
        List<TopSellingProjection> projections = stockMovementRepository
                .findTopSellingProducts(MovementType.WYDANIE);

        return projections.stream()
                .limit(5)
                .map(p -> new TopSellingProduct(p.getProductId(), p.getProductName(), p.getTotalSold()))
                .collect(Collectors.toList());
    }

    private List<ReorderAlert> getReorderAlerts(Long tenantId) {
        List<Product> lowStockProducts = productRepository.findProductsBelowMinStock(tenantId);

        if (lowStockProducts.isEmpty()) {
            return Collections.emptyList();
        }

        return lowStockProducts.stream()
                .map(p -> new ReorderAlert(
                        p.getId(),
                        p.getName(),
                        p.getSku(),
                        p.getQuantity(),
                        p.getMinQuantity(),
                        p.getMinQuantity() - p.getQuantity()
                ))
                .collect(Collectors.toList());
    }
}
