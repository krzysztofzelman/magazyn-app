package com.example.magazyn.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.persistence.EntityManager;
import java.io.IOException;

/**
 * Spring Security filter that enables the Hibernate @Filter for the current tenant.
 * Runs after the EntityManager is available (within OpenSessionInViewFilter scope).
 * Must be added to the SecurityFilterChain.
 */
public class TenantSessionFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

    public TenantSessionFilter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        Long tenantId = TenantContext.getTenantId();

        if (tenantId != null) {
            try {
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenantId);
            } catch (Exception e) {
                logger.warn("Could not enable tenant filter: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
