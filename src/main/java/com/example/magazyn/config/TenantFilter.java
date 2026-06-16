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
 * Deprecated: tenant ID is now extracted exclusively from the JWT token
 * by JwtAuthenticationFilter. This filter is kept as a no-op pass-through
 * to maintain filter ordering without changes.
 * <p>
 * The X-Tenant-Id header is STRIPPED by nginx (proxy_set_header X-Tenant-Id "")
 * and must NOT be trusted from client requests to prevent tenant spoofing.
 */
@Component
@Order(1)
@Deprecated
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        // Tenant ID is now set exclusively by JwtAuthenticationFilter from JWT claims.
        // X-Tenant-Id header from clients is stripped by nginx and never trusted.
        filterChain.doFilter(request, response);
    }
}
