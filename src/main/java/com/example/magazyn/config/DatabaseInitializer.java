package com.example.magazyn.config;

import com.example.magazyn.entity.User;
import com.example.magazyn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Users already exist in database — skipping default user creation");
            return;
        }

        log.info("No users found — creating default users");

        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("REMOVED"))
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
    }
}
