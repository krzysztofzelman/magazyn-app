package com.example.magazyn.config;

import com.example.magazyn.util.AuditContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts the client IP address from X-Forwarded-For or remoteAddr
 * and stores it in AuditContext (ThreadLocal) for AuditLogService to consume.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        try {
            String ip = resolveClientIp(request);
            AuditContext.setIp(ip);
            filterChain.doFilter(request, response);
        } finally {
            AuditContext.clear();
        }
    }

    static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
            // X-Forwarded-For can contain a comma-separated list — take the first (original client)
            String firstIp = xff.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        return request.getRemoteAddr();
    }
}
