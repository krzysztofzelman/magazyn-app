package com.example.magazyn.config;

import com.example.magazyn.entity.Tenant;
import com.example.magazyn.entity.User;
import com.example.magazyn.repository.TenantRepository;
import com.example.magazyn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist in database — skipping default user creation");
            return;
        }

        // Find the default tenant (created by Flyway V9 migration)
        Tenant defaultTenant = tenantRepository.findBySubdomain("default")
                .orElseThrow(() -> new RuntimeException("Default tenant not found — ensure Flyway migrations have run"));

        // Set tenant context so TenantAware @PrePersist picks it up
        TenantContext.setTenantId(defaultTenant.getId());
        try {
            log.warn("Creating default users under tenant '{}'", defaultTenant.getName());
            log.warn("DEFAULT PASSWORDS: admin=admin123, manager=manager123, warehouse=warehouse123, viewer=viewer123");
            log.warn("CHANGE DEFAULT PASSWORDS AFTER FIRST LOGIN for production use");

            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .role("ROLE_ADMIN")
                    .email("admin@magazyn.local")
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("manager")
                    .password(passwordEncoder.encode("manager123"))
                    .role("ROLE_MANAGER")
                    .email("manager@magazyn.local")
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("warehouse")
                    .password(passwordEncoder.encode("warehouse123"))
                    .role("ROLE_WAREHOUSE")
                    .email("warehouse@magazyn.local")
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("viewer")
                    .password(passwordEncoder.encode("viewer123"))
                    .role("ROLE_VIEWER")
                    .email("viewer@magazyn.local")
                    .isActive(true)
                    .build());

            log.info("Created default users: admin, manager, warehouse, viewer");
        } finally {
            TenantContext.clear();
        }
    }
}
