package com.example.magazyn.ksef.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.ksef.config.KsefConfig;
import com.example.magazyn.ksef.exception.KsefAuthenticationException;
import com.example.magazyn.ksef.model.dto.KSeFSessionResponse;
import com.example.magazyn.ksef.model.entity.KsefSession;
import com.example.magazyn.ksef.model.enums.KSeFOperationType;
import com.example.magazyn.ksef.repository.KsefSessionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages KSeF API authentication sessions.
 * Handles token lifecycle: init, refresh, close, and cache.
 * Token is valid for 8 hours per MF specification.
 */
@Service
@Transactional
public class KsefAuthService {

    private static final Logger log = LoggerFactory.getLogger(KsefAuthService.class);
    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final Duration REFRESH_THRESHOLD = Duration.ofMinutes(30);

    private final KsefConfig config;
    private final KsefSessionRepository sessionRepository;
    private final KsefAuditService auditService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KsefAuthService(KsefConfig config,
                           KsefSessionRepository sessionRepository,
                           KsefAuditService auditService) {
        this.config = config;
        this.sessionRepository = sessionRepository;
        this.auditService = auditService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getConnectionTimeout()))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Get an active KSeF session for the current tenant.
     * Reuses existing valid session or initiates a new one.
     */
    public KSeFSessionResponse getOrCreateSession(String username) {
        Long tenantId = TenantContext.getTenantId();

        // Try to find an existing active session
        var activeSession = sessionRepository.findLatestActiveSession(tenantId, LocalDateTime.now());
        if (activeSession.isPresent()) {
            KsefSession session = activeSession.get();
            // Refresh if close to expiry
            if (session.getExpiresAt().minus(REFRESH_THRESHOLD).isBefore(LocalDateTime.now())) {
                return refreshSession(session, username);
            }
            touchSession(session);
            return toResponse(session);
        }

        // Initiate new session with KSeF API
        return initiateSession(username);
    }

    /**
     * Initiate a new KSeF session via the API.
     */
    private KSeFSessionResponse initiateSession(String username) {
        Long tenantId = TenantContext.getTenantId();
        long start = System.currentTimeMillis();

        try {
            // Build KSeF API auth request
            String authPayload = buildAuthPayload();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl() + "/auth/init"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(config.getReadTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(authPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            int duration = (int) (System.currentTimeMillis() - start);
            JsonNode json = objectMapper.readTree(response.body());

            if (response.statusCode() != 200) {
                String errorMsg = json.has("error") ? json.get("error").asText() : "Unknown error";
                auditService.log(
                        KSeFOperationType.SESSION_INIT, username, config.getNip(),
                        null, null, "HTTP " + response.statusCode() + ": " + errorMsg,
                        false, errorMsg, String.valueOf(response.statusCode()), duration);
                throw new KsefAuthenticationException("KSeF session init failed: " + errorMsg);
            }

            String sessionToken = json.get("sessionToken").asText();
            String referenceNumber = json.has("referenceNumber")
                    ? json.get("referenceNumber").asText() : null;
            LocalDateTime expiresAt = LocalDateTime.now().plus(SESSION_TTL);

            String nip = config.getNip();
            if (nip == null || nip.isBlank()) {
                throw new KsefAuthenticationException(
                        "KSeF not configured: COMPANY_NIP must be set in environment");
            }

            KsefSession session = KsefSession.builder()
                    .sessionToken(sessionToken)
                    .referenceNumber(referenceNumber)
                    .initiatedBy(username)
                    .expiresAt(expiresAt)
                    .isActive(true)
                    .nip(nip)
                    .build();

            session = sessionRepository.save(session);

            auditService.log(
                    KSeFOperationType.SESSION_INIT, username, config.getNip(),
                    null, session.getId(),
                    "Session initiated, expires: " + expiresAt,
                    true, null, null, duration);

            log.info("KSeF session initiated: ref={}, expires={}", referenceNumber, expiresAt);
            return toResponse(session);

        } catch (KsefAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            int duration = (int) (System.currentTimeMillis() - start);
            auditService.log(
                    KSeFOperationType.AUTH_ERROR, username, config.getNip(),
                    null, null, e.getMessage(),
                    false, e.getMessage(), "CONNECTION_ERROR", duration);
            throw new KsefAuthenticationException("Failed to initiate KSeF session: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh an existing KSeF session.
     */
    private KSeFSessionResponse refreshSession(KsefSession session, String username) {
        long start = System.currentTimeMillis();

        try {
            String refreshPayload = "{\"sessionToken\":\"" + session.getSessionToken() + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl() + "/auth/refresh"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofMillis(config.getReadTimeout()))
                    .POST(HttpRequest.BodyPublishers.ofString(refreshPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            int duration = (int) (System.currentTimeMillis() - start);

            if (response.statusCode() != 200) {
                JsonNode json = objectMapper.readTree(response.body());
                String errorMsg = json.has("error") ? json.get("error").asText() : "Unknown error";
                auditService.log(
                        KSeFOperationType.SESSION_REFRESH, username, config.getNip(),
                        null, session.getId(), "HTTP " + response.statusCode() + ": " + errorMsg,
                        false, errorMsg, String.valueOf(response.statusCode()), duration);

                // If refresh fails, mark session inactive and initiate new one
                session.setIsActive(false);
                session.setErrorMessage(errorMsg);
                sessionRepository.save(session);
                return initiateSession(username);
            }

            String newSessionToken = objectMapper.readTree(response.body())
                    .get("sessionToken").asText();
            LocalDateTime newExpiry = LocalDateTime.now().plus(SESSION_TTL);

            session.setSessionToken(newSessionToken);
            session.setExpiresAt(newExpiry);
            session.setRefreshedAt(LocalDateTime.now());
            session.setErrorMessage(null);
            session = sessionRepository.save(session);

            auditService.log(
                    KSeFOperationType.SESSION_REFRESH, username, config.getNip(),
                    null, session.getId(), "Session refreshed, expires: " + newExpiry,
                    true, null, null, duration);

            return toResponse(session);

        } catch (Exception e) {
            int duration = (int) (System.currentTimeMillis() - start);
            auditService.log(
                    KSeFOperationType.AUTH_ERROR, username, config.getNip(),
                    null, session.getId(), e.getMessage(),
                    false, e.getMessage(), "REFRESH_ERROR", duration);

            // Mark session as potentially stale but try to continue
            log.warn("KSeF session refresh failed (session={}): {}", session.getId(), e.getMessage());
            return toResponse(session);
        }
    }

    /**
     * Close an active KSeF session.
     */
    public void closeSession(Long sessionId, String username) {
        Long tenantId = TenantContext.getTenantId();
        long start = System.currentTimeMillis();

        sessionRepository.findById(sessionId).ifPresent(session -> {
            if (!session.getTenantId().equals(tenantId)) return;

            try {
                String closePayload = "{\"sessionToken\":\"" + session.getSessionToken() + "\"}";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(config.getApiUrl() + "/auth/close"))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .timeout(Duration.ofMillis(config.getReadTimeout()))
                        .POST(HttpRequest.BodyPublishers.ofString(closePayload))
                        .build();

                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                log.warn("Failed to close KSeF session {}: {}", sessionId, e.getMessage());
            }

            session.setIsActive(false);
            sessionRepository.save(session);

            int duration = (int) (System.currentTimeMillis() - start);
            auditService.log(
                    KSeFOperationType.SESSION_CLOSE, username, config.getNip(),
                    null, sessionId, "Session closed",
                    true, null, null, duration);
        });
    }

    /**
     * Get active session token for API calls. Throws if no active session.
     */
    public String getActiveSessionToken() {
        Long tenantId = TenantContext.getTenantId();
        var session = sessionRepository.findLatestActiveSession(tenantId, LocalDateTime.now());
        return session.map(KsefSession::getSessionToken)
                .orElseThrow(() -> new KsefAuthenticationException("Brak aktywnej sesji KSeF. Zainicjuj sesję."));
    }

    /**
     * Get active session for the current tenant.
     */
    public KsefSession getActiveSession() {
        Long tenantId = TenantContext.getTenantId();
        return sessionRepository.findLatestActiveSession(tenantId, LocalDateTime.now())
                .orElseThrow(() -> new KsefAuthenticationException("Brak aktywnej sesji KSeF"));
    }

    /**
     * List all sessions for the current tenant.
     */
    @Transactional(readOnly = true)
    public List<KSeFSessionResponse> getSessions() {
        Long tenantId = TenantContext.getTenantId();
        return sessionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void touchSession(KsefSession session) {
        session.setLastUsedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    private String buildAuthPayload() {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "apiKey", config.getApiKey() != null ? config.getApiKey() : "",
                    "nip", config.getNip()
            ));
        } catch (Exception e) {
            throw new KsefAuthenticationException("Failed to build auth payload", e);
        }
    }

    private KSeFSessionResponse toResponse(KsefSession session) {
        return new KSeFSessionResponse(
                session.getId(),
                session.getReferenceNumber(),
                session.getIsActive(),
                session.getInitiatedAt(),
                session.getExpiresAt(),
                session.getRefreshedAt(),
                session.getLastUsedAt(),
                session.getNip(),
                session.getApiVersion(),
                session.getErrorMessage()
        );
    }
}
