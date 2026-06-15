package com.example.magazyn.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HandlerInterceptor that enables the Hibernate @Filter for the current tenant.
 * Runs within the DispatcherServlet lifecycle, AFTER OpenEntityManagerInViewFilter
 * has opened the Hibernate session, guaranteeing entityManager.unwrap(Session.class)
 * will succeed.
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                             jakarta.servlet.http.HttpServletResponse response,
                             Object handler) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            try {
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenantId);
            } catch (Exception e) {
                log.error("Failed to enable tenant filter for tenantId={}: {}", tenantId, e.getMessage(), e);
            }
        }
        return true;
    }
}
