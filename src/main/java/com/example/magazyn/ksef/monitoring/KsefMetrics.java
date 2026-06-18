package com.example.magazyn.ksef.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Prometheus metrics for KSeF operations.
 * Tracks invoices sent, errors, response times, and active sessions.
 */
@Component
public class KsefMetrics {

    private final MeterRegistry meterRegistry;

    private Counter ksefInvoicesSent;
    private Counter ksefErrors;
    private Timer ksefRequestDuration;
    private Counter ksefSessionInitiated;
    private Counter ksefSessionRefreshed;
    private Counter ksefRetries;

    private volatile Supplier<Long> activeSessionsSupplier = () -> 0L;

    public KsefMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    void init() {
        this.ksefInvoicesSent = Counter.builder("ksef.invoices.sent")
                .description("Number of invoices sent to KSeF")
                .tag("status", "sent")
                .register(meterRegistry);

        this.ksefErrors = Counter.builder("ksef.errors.total")
                .description("Total number of KSeF errors")
                .tag("type", "all")
                .register(meterRegistry);

        this.ksefRequestDuration = Timer.builder("ksef.request.duration")
                .description("KSeF API request duration")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .sla(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofSeconds(1),
                        java.time.Duration.ofSeconds(2),
                        java.time.Duration.ofSeconds(5)
                )
                .register(meterRegistry);

        this.ksefSessionInitiated = Counter.builder("ksef.session.initiated")
                .description("Number of KSeF sessions initiated")
                .register(meterRegistry);

        this.ksefSessionRefreshed = Counter.builder("ksef.session.refreshed")
                .description("Number of KSeF sessions refreshed")
                .register(meterRegistry);

        this.ksefRetries = Counter.builder("ksef.retries.total")
                .description("Number of KSeF retry attempts")
                .register(meterRegistry);

        Gauge.builder("ksef.active.sessions", this::getActiveSessions)
                .description("Number of active KSeF sessions")
                .register(meterRegistry);
    }

    // ── Record methods ──

    /**
     * Record a successfully sent invoice.
     */
    public void recordInvoiceSent(String status) {
        ksefInvoicesSent.increment();
    }

    /**
     * Record a KSeF error.
     * @param type error type (e.g. "AUTH_ERROR", "API_ERROR", "VALIDATION_ERROR")
     */
    public void recordError(String type) {
        ksefErrors.increment();
        Counter.builder("ksef.errors")
                .description("KSeF errors by type")
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record a KSeF API request duration.
     */
    public void recordRequestDuration(long millis) {
        ksefRequestDuration.record(millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Record a session initiation.
     */
    public void recordSessionInitiated() {
        ksefSessionInitiated.increment();
    }

    /**
     * Record a session refresh.
     */
    public void recordSessionRefreshed() {
        ksefSessionRefreshed.increment();
    }

    /**
     * Record a retry attempt.
     */
    public void recordRetry() {
        ksefRetries.increment();
    }

    // ── Active sessions supplier ──

    public void setActiveSessionsSupplier(Supplier<Long> supplier) {
        this.activeSessionsSupplier = supplier;
    }

    private long getActiveSessions() {
        try {
            return activeSessionsSupplier.get();
        } catch (Exception e) {
            return 0L;
        }
    }
}
