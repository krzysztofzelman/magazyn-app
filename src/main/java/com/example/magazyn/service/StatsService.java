package com.example.magazyn.service;

import com.example.magazyn.dto.StatsDashboardResponse;
import com.example.magazyn.dto.StatsDashboardResponse.ReorderAlert;
import com.example.magazyn.dto.StatsDashboardResponse.TopSellingProduct;
import com.example.magazyn.entity.MovementType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.StockMovementRepository;
import com.example.magazyn.repository.TopSellingProjection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Autowired
    public StatsService(ProductRepository productRepository,
                        StockMovementRepository stockMovementRepository) {
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public StatsDashboardResponse getDashboard() {
        long totalProducts = productRepository.count();

        BigDecimal totalStockValue = productRepository.getTotalStockValue();

        List<TopSellingProduct> topSelling = getTopSellingProducts();

        List<ReorderAlert> reorderAlerts = getReorderAlerts();

        // No sales module — return null for revenue
        BigDecimal revenueLast30Days = null;

        return new StatsDashboardResponse(
                totalProducts,
                totalStockValue,
                topSelling,
                reorderAlerts,
                revenueLast30Days
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

    private List<ReorderAlert> getReorderAlerts() {
        List<Product> lowStockProducts = productRepository.findProductsBelowMinStock();

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
