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
 * HandlerInterceptor that enables the Hibernate @Filter for the current warehouse.
 * Runs within the DispatcherServlet lifecycle, AFTER OpenEntityManagerInViewFilter
 * has opened the Hibernate session.
 */
@Component
public class WarehouseInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WarehouseInterceptor.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                             jakarta.servlet.http.HttpServletResponse response,
                             Object handler) {
        Long warehouseId = WarehouseContext.getWarehouseId();
        if (warehouseId != null) {
            try {
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("warehouseFilter");
                filter.setParameter("warehouseId", warehouseId);
            } catch (Exception e) {
                log.error("Failed to enable warehouse filter for warehouseId={}: {}", warehouseId, e.getMessage(), e);
            }
        }
        return true;
    }
}
