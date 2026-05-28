package com.example.magazyn.dto;

import java.math.BigDecimal;
import java.util.List;

public class StatsDashboardResponse {

    private long totalProducts;
    private BigDecimal totalStockValue;
    private List<TopSellingProduct> topSellingProducts;
    private List<ReorderAlert> reorderAlerts;
    private BigDecimal revenueLast30Days;
    private long expiringBatchesCount;
    private BigDecimal expiredBatchesValue;

    public StatsDashboardResponse() {}

    public StatsDashboardResponse(long totalProducts, BigDecimal totalStockValue,
                                  List<TopSellingProduct> topSellingProducts,
                                  List<ReorderAlert> reorderAlerts,
                                  BigDecimal revenueLast30Days) {
        this(totalProducts, totalStockValue, topSellingProducts, reorderAlerts, revenueLast30Days, 0, BigDecimal.ZERO);
    }

    public StatsDashboardResponse(long totalProducts, BigDecimal totalStockValue,
                                  List<TopSellingProduct> topSellingProducts,
                                  List<ReorderAlert> reorderAlerts,
                                  BigDecimal revenueLast30Days,
                                  long expiringBatchesCount,
                                  BigDecimal expiredBatchesValue) {
        this.totalProducts = totalProducts;
        this.totalStockValue = totalStockValue;
        this.topSellingProducts = topSellingProducts;
        this.reorderAlerts = reorderAlerts;
        this.revenueLast30Days = revenueLast30Days;
        this.expiringBatchesCount = expiringBatchesCount;
        this.expiredBatchesValue = expiredBatchesValue;
    }

    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }

    public BigDecimal getTotalStockValue() { return totalStockValue; }
    public void setTotalStockValue(BigDecimal totalStockValue) { this.totalStockValue = totalStockValue; }

    public List<TopSellingProduct> getTopSellingProducts() { return topSellingProducts; }
    public void setTopSellingProducts(List<TopSellingProduct> topSellingProducts) {
        this.topSellingProducts = topSellingProducts;
    }

    public List<ReorderAlert> getReorderAlerts() { return reorderAlerts; }
    public void setReorderAlerts(List<ReorderAlert> reorderAlerts) { this.reorderAlerts = reorderAlerts; }

    public BigDecimal getRevenueLast30Days() { return revenueLast30Days; }
    public void setRevenueLast30Days(BigDecimal revenueLast30Days) {
        this.revenueLast30Days = revenueLast30Days;
    }

    public long getExpiringBatchesCount() { return expiringBatchesCount; }
    public void setExpiringBatchesCount(long expiringBatchesCount) {
        this.expiringBatchesCount = expiringBatchesCount;
    }

    public BigDecimal getExpiredBatchesValue() { return expiredBatchesValue; }
    public void setExpiredBatchesValue(BigDecimal expiredBatchesValue) {
        this.expiredBatchesValue = expiredBatchesValue;
    }

    // --- Inner classes ---

    public static class TopSellingProduct {
        private Long productId;
        private String productName;
        private Long totalSold;

        public TopSellingProduct() {}

        public TopSellingProduct(Long productId, String productName, Long totalSold) {
            this.productId = productId;
            this.productName = productName;
            this.totalSold = totalSold;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public Long getTotalSold() { return totalSold; }
        public void setTotalSold(Long totalSold) { this.totalSold = totalSold; }
    }

    public static class ReorderAlert {
        private Long productId;
        private String productName;
        private String sku;
        private Integer currentQuantity;
        private Integer minQuantity;
        private Integer deficit;

        public ReorderAlert() {}

        public ReorderAlert(Long productId, String productName, String sku,
                            Integer currentQuantity, Integer minQuantity, Integer deficit) {
            this.productId = productId;
            this.productName = productName;
            this.sku = sku;
            this.currentQuantity = currentQuantity;
            this.minQuantity = minQuantity;
            this.deficit = deficit;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }

        public Integer getCurrentQuantity() { return currentQuantity; }
        public void setCurrentQuantity(Integer currentQuantity) { this.currentQuantity = currentQuantity; }

        public Integer getMinQuantity() { return minQuantity; }
        public void setMinQuantity(Integer minQuantity) { this.minQuantity = minQuantity; }

        public Integer getDeficit() { return deficit; }
        public void setDeficit(Integer deficit) { this.deficit = deficit; }
    }
}
