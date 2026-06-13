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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Only rate-limit POST /api/auth/login
        if (!("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path))) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = request.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }
        Bucket bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

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

    private Bucket createBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }
}
