package com.example.magazyn.config;

import com.example.magazyn.repository.UserRepository;
import com.example.magazyn.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        String path = request.getRequestURI();

        // Public endpoints — always let through without token
        if (path.equals("/api/auth/login") || path.equals("/api/auth/refresh")
                || path.equals("/api/tenants/register")
                || path.equals("/actuator/health") || path.equals("/actuator/info")
                || path.equals("/") || path.equals("/index.html") || path.equals("/favicon.svg")
                || path.equals("/icons.svg") || path.startsWith("/assets/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);
                Long tenantId = jwtUtil.extractTenantId(token);

                // Set tenant context from JWT
                TenantContext.setTenantId(tenantId);

                // Verify user is still active
                try {
                    boolean active = userRepository.findByUsernameAndTenantId(username, tenantId)
                            .map(User -> User.getIsActive())
                            .orElse(false);
                    if (!active) {
                        log.warn("Rejecting request for inactive/missing user {} in tenant {}", username, tenantId);
                        TenantContext.clear();
                        WarehouseContext.clear();
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Konto u\u017Cytkownika jest nieaktywne");
                        return;
                    }
                } catch (Exception e) {
                    log.error("Error checking user active status for {}", username, e);
                    TenantContext.clear();
                    WarehouseContext.clear();
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "User verification failed");
                    return;
                }

                // Set warehouse context from header (optional)
                String warehouseHeader = request.getHeader("X-Warehouse-Id");
                if (warehouseHeader != null && !warehouseHeader.isBlank()) {
                    try {
                        WarehouseContext.setWarehouseId(Long.parseLong(warehouseHeader));
                    } catch (NumberFormatException ignored) {}
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                List.of(new SimpleGrantedAuthority(role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
