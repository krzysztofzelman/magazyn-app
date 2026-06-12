package com.example.magazyn.config;

public final class WarehouseContext {
    private static final ThreadLocal<Long> CURRENT_WAREHOUSE = new ThreadLocal<>();

    private WarehouseContext() {}

    public static void setWarehouseId(Long warehouseId) {
        CURRENT_WAREHOUSE.set(warehouseId);
    }

    public static Long getWarehouseId() {
        return CURRENT_WAREHOUSE.get();
    }

    public static void clear() {
        CURRENT_WAREHOUSE.remove();
    }
}
