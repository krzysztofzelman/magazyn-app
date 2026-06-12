package com.example.magazyn.config;

import jakarta.persistence.EntityManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class WarehouseSessionFilter extends OncePerRequestFilter {

    private final EntityManager entityManager;

    public WarehouseSessionFilter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Long warehouseId = WarehouseContext.getWarehouseId();
        if (warehouseId != null) {
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.enableFilter("warehouseFilter");
            filter.setParameter("warehouseId", warehouseId);
        }
        filterChain.doFilter(request, response);
    }
}
