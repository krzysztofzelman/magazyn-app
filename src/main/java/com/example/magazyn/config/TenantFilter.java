package com.example.magazyn.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves tenant from request and sets TenantContext.
 * For subdomain-based routing (e.g. customer.magazyn.kzelman.pl),
 * the tenant is determined by the X-Tenant-Id header (set by nginx).
 * For JWT-authenticated requests, the JWT filter overrides this.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        // Resolve tenant from header (set by nginx based on subdomain)
        // For JWT-authenticated requests, JwtAuthenticationFilter will override this
        String tenantHeader = request.getHeader("X-Tenant-Id");
        if (tenantHeader != null && !tenantHeader.isEmpty()) {
            TenantContext.setTenantId(Long.valueOf(tenantHeader));
        }

        filterChain.doFilter(request, response);
    }
}
