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
 * Cleans up TenantContext after the request completes.
 * Must be the outermost filter to prevent ThreadLocal leaks.
 */
@Component
@Order(Integer.MAX_VALUE)
public class TenantCleanupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            WarehouseContext.clear();
        }
    }
}
