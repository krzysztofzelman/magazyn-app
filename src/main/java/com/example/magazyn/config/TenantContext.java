package com.example.magazyn.config;

/**
 * ThreadLocal holder for the current tenant ID throughout a request.
 * Set by JwtAuthenticationFilter from JWT claims at the start of each request,
 * cleared at the end by TenantCleanupFilter.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
