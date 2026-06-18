package com.example.magazyn.ksef.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "ksef")
@Validated
public class KsefConfig {

    @NotBlank
    private String apiUrl = "https://ksef-test.mf.gov.pl/api/v1";

    private String apiKey;

    private String nip;

    @Positive
    private int connectionTimeout = 30000;

    @Positive
    private int readTimeout = 30000;

    private RetryConfig retry = new RetryConfig();

    private EncryptionConfig encryption = new EncryptionConfig();

    private AuditConfig audit = new AuditConfig();

    // Getters and setters

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getNip() { return nip; }
    public void setNip(String nip) { this.nip = nip; }

    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    public int getReadTimeout() { return readTimeout; }
    public void setReadTimeout(int readTimeout) { this.readTimeout = readTimeout; }

    public RetryConfig getRetry() { return retry; }
    public void setRetry(RetryConfig retry) { this.retry = retry; }

    public EncryptionConfig getEncryption() { return encryption; }
    public void setEncryption(EncryptionConfig encryption) { this.encryption = encryption; }

    public AuditConfig getAudit() { return audit; }
    public void setAudit(AuditConfig audit) { this.audit = audit; }

    public static class RetryConfig {
        @Positive
        private int maxAttempts = 3;

        @Positive
        private long delay = 1000;

        private double multiplier = 2.0;

        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

        public long getDelay() { return delay; }
        public void setDelay(long delay) { this.delay = delay; }

        public double getMultiplier() { return multiplier; }
        public void setMultiplier(double multiplier) { this.multiplier = multiplier; }
    }

    public static class EncryptionConfig {
        private String key;
        private String algorithm = "AES/GCM/NoPadding";

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
    }

    public static class AuditConfig {
        private boolean enabled = true;
        private int retentionDays = 90;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public int getRetentionDays() { return retentionDays; }
        public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    }
}
