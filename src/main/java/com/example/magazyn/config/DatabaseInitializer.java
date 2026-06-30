package com.example.magazyn.config;

import com.example.magazyn.entity.Tenant;
import com.example.magazyn.entity.User;
import com.example.magazyn.repository.TenantRepository;
import com.example.magazyn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultUserPassword;

    public DatabaseInitializer(UserRepository userRepository, TenantRepository tenantRepository,
                               PasswordEncoder passwordEncoder, Environment env) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultUserPassword = env.getProperty("DEFAULT_USER_PASSWORD", "change_me");
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
            log.info("Creating default users under tenant '{}'", defaultTenant.getName());

            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(defaultUserPassword))
                    .role("ROLE_ADMIN")
                    .email("admin@magazyn.local")
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("manager")
                    .password(passwordEncoder.encode(defaultUserPassword))
                    .role("ROLE_MANAGER")
                    .email("manager@magazyn.local")
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("warehouse")
                    .password(passwordEncoder.encode(defaultUserPassword))
                    .role("ROLE_WAREHOUSE")
                    .email("warehouse@magazyn.local")
                    .isActive(true)
                    .build());

            userRepository.save(User.builder()
                    .username("viewer")
                    .password(passwordEncoder.encode(defaultUserPassword))
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
