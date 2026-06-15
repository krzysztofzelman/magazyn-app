package com.example.magazyn.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TenantInterceptor tenantInterceptor;
    private final WarehouseInterceptor warehouseInterceptor;

    public WebConfig(TenantInterceptor tenantInterceptor, WarehouseInterceptor warehouseInterceptor) {
        this.tenantInterceptor = tenantInterceptor;
        this.warehouseInterceptor = warehouseInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(warehouseInterceptor).addPathPatterns("/api/**");
    }
}
