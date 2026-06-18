package com.example.magazyn.ksef.config;

import com.example.magazyn.ksef.repository.KsefSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * KSeF health indicator for Spring Boot Actuator.
 * Reports:
 * - UP: KSeF is configured and has active sessions
 * - DEGRADED: KSeF is configured but no active session
 * - DOWN: KSeF is not configured (missing NIP)
 */
@Component
public class KsefHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(KsefHealthIndicator.class);

    private final KsefConfig config;
    private final KsefSessionRepository sessionRepository;

    public KsefHealthIndicator(KsefConfig config, KsefSessionRepository sessionRepository) {
        this.config = config;
        this.sessionRepository = sessionRepository;
    }

    @Override
    public Health health() {
        // Check if KSeF is configured
        if (config.getNip() == null || config.getNip().isBlank()) {
            return Health.down()
                    .withDetail("ksefStatus", "NOT_CONFIGURED")
                    .withDetail("message", "KSeF nie jest skonfigurowany — brak NIP")
                    .build();
        }

        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return Health.status("DEGRADED")
                    .withDetail("ksefStatus", "NO_API_KEY")
                    .withDetail("nip", maskNip(config.getNip()))
                    .withDetail("apiUrl", config.getApiUrl())
                    .withDetail("message", "KSeF skonfigurowany, ale brak klucza API")
                    .build();
        }

        // Check active sessions
        try {
            long activeSessions = sessionRepository.countByTenantIdAndIsActiveTrueAndExpiresAtAfter(
                    1L, LocalDateTime.now());

            if (activeSessions > 0) {
                return Health.up()
                        .withDetail("ksefStatus", "CONNECTED")
                        .withDetail("nip", maskNip(config.getNip()))
                        .withDetail("apiUrl", config.getApiUrl())
                        .withDetail("activeSessions", activeSessions)
                        .withDetail("message", "KSeF połączony — aktywna sesja")
                        .build();
            } else {
                return Health.status("DEGRADED")
                        .withDetail("ksefStatus", "NO_SESSION")
                        .withDetail("nip", maskNip(config.getNip()))
                        .withDetail("apiUrl", config.getApiUrl())
                        .withDetail("activeSessions", 0)
                        .withDetail("message", "KSeF skonfigurowany, ale brak aktywnej sesji")
                        .build();
            }
        } catch (Exception e) {
            log.warn("KSeF health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("ksefStatus", "CHECK_FAILED")
                    .withDetail("message", "Błąd sprawdzania stanu KSeF: " + e.getMessage())
                    .build();
        }
    }

    private String maskNip(String nip) {
        if (nip == null || nip.length() < 6) return nip;
        return nip.substring(0, 3) + "****" + nip.substring(nip.length() - 3);
    }
}
