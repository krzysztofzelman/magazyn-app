package com.example.magazyn.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final long refreshTokenDurationMs;

    public AuthController(AuthService authService,
                          @Value("${jwt.refresh-expiration:604800000}") long refreshTokenDurationMs) {
        this.authService = authService;
        this.refreshTokenDurationMs = refreshTokenDurationMs;
    }

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                  HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        setRefreshTokenCookie(response, authResponse.getRefreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.login(request);
            setRefreshTokenCookie(response, authResponse.getRefreshToken());
            return ResponseEntity.ok(authResponse);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Nieprawidłowa nazwa użytkownika lub hasło"));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody(required = false) RefreshTokenRequest request,
                                     HttpServletRequest servletRequest,
                                     HttpServletResponse servletResponse) {
        // Read refresh token from cookie first (httpOnly), fall back to request body
        String refreshToken = resolveRefreshToken(servletRequest, request);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Brak tokena odświeżania"));
        }

        try {
            AuthResponse authResponse = authService.refresh(refreshToken);
            setRefreshTokenCookie(servletResponse, authResponse.getRefreshToken());
            return ResponseEntity.ok(authResponse);
        } catch (RefreshTokenException e) {
            clearRefreshTokenCookie(servletResponse);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshTokenRequest request,
                                    HttpServletRequest servletRequest,
                                    HttpServletResponse servletResponse) {
        String refreshToken = resolveRefreshToken(servletRequest, request);
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(refreshToken);
        }
        clearRefreshTokenCookie(servletResponse);
        return ResponseEntity.ok(new LogoutResponse("Wylogowano pomyślnie"));
    }

    private String resolveRefreshToken(HttpServletRequest request, RefreshTokenRequest body) {
        // Prefer cookie (httpOnly, more secure)
        if (request.getCookies() != null) {
            java.util.Optional<Cookie> cookie = Arrays.stream(request.getCookies())
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .findFirst();
            if (cookie.isPresent() && cookie.get().getValue() != null && !cookie.get().getValue().isBlank()) {
                return cookie.get().getValue();
            }
        }
        // Fall back to request body for backward compatibility
        if (body != null && body.getRefreshToken() != null && !body.getRefreshToken().isBlank()) {
            return body.getRefreshToken();
        }
        return null;
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) (refreshTokenDurationMs / 1000));
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private record ErrorResponse(String error) {}
    private record LogoutResponse(String message) {}
}
