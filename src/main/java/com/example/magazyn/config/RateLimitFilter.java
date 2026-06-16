package com.example.magazyn.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Per-endpoint rate limit configuration: {method:path:rps/burst}
    // Login: 20 requests per minute per IP
    // Tenant registration: 5 requests per hour per IP (prevents mass registration)
    // AI Assistant: 30 requests per minute per IP (controls API costs)
    private static final RateLimitConfig[] RATE_LIMITS = {
        new RateLimitConfig("POST", "/api/auth/login", 20, Duration.ofMinutes(1)),
        new RateLimitConfig("POST", "/api/tenants/register", 5, Duration.ofHours(1)),
        new RateLimitConfig("POST", "/api/assistant/chat", 30, Duration.ofMinutes(1)),
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Check if this path matches any rate-limited endpoint
        RateLimitConfig matched = findMatchingConfig(method, path);
        if (matched == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Use the LAST address in X-Forwarded-For (closest to nginx, most trustworthy)
        // to prevent IP spoofing via the client-controlled first address.
        String clientIp = resolveClientIp(request);
        String bucketKey = matched.path + ":" + clientIp;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(matched.limit, matched.duration));

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            String body = "{\"status\":429,\"message\":\"Za du\u017co \u017c\u0105da\u0144 — spr\u00f3buj ponownie za chwil\u0119\",\"timestamp\":\""
                    + java.time.LocalDateTime.now() + "\"}";
            response.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            response.getOutputStream().flush();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            // Take the LAST address in the chain — closest to nginx/proxy,
            // not the first which is attacker-controlled.
            String[] parts = forwardedFor.split(",");
            String lastIp = parts[parts.length - 1].trim();
            if (!lastIp.isEmpty()) {
                return lastIp;
            }
        }
        return request.getRemoteAddr();
    }

    private static RateLimitConfig findMatchingConfig(String method, String path) {
        for (RateLimitConfig config : RATE_LIMITS) {
            if (config.method.equalsIgnoreCase(method) && config.path.equals(path)) {
                return config;
            }
        }
        return null;
    }

    private static Bucket createBucket(long limit, Duration duration) {
        Bandwidth bandwidth = Bandwidth.classic(limit, Refill.greedy(limit, duration));
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private record RateLimitConfig(String method, String path, long limit, Duration duration) {}
}
