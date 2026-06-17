package com.example.magazyn.auth;

import com.example.magazyn.entity.User;
import com.example.magazyn.exception.DuplicateResourceException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.UserRepository;
import com.example.magazyn.config.TenantContext;
import com.example.magazyn.service.AuditLogService;
import com.example.magazyn.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       RefreshTokenService refreshTokenService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.auditLogService = auditLogService;
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists");
        }

        String role = request.getRole() != null && !request.getRole().isBlank()
                ? normalizeRole(request.getRole())
                : "ROLE_WAREHOUSE";

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(role)
                .isActive(true)
                .build();

        userRepository.save(user);

        // Set tenant context from the saved user so TenantAware @PrePersist works
        TenantContext.setTenantId(user.getTenantId());
        try {
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getTenantId());
            RefreshTokenService.RefreshTokenResult refreshResult = refreshTokenService.generateRefreshToken(user);

            auditLogService.log(currentUsername(), "REGISTER", "User", user.getId(),
                    "Registered user: " + user.getUsername());
            return new AuthResponse(token, refreshResult.rawToken(), user.getUsername(), user.getRole());
        } finally {
            TenantContext.clear();
        }
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getUsername()));

            TenantContext.setTenantId(user.getTenantId());
            try {
                String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getTenantId());
                RefreshTokenService.RefreshTokenResult refreshResult = refreshTokenService.generateRefreshToken(user);

                auditLogService.log(user.getUsername(), "LOGIN_SUCCESS", "User", user.getId(),
                        "Login successful for user: " + user.getUsername());
                return new AuthResponse(token, refreshResult.rawToken(), user.getUsername(), user.getRole());
            } finally {
                TenantContext.clear();
            }
        } catch (AuthenticationException e) {
            auditLogService.log("anonymous", "LOGIN_FAILURE", "User", null,
                    "Login failed for username=" + request.getUsername() + " reason=" + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenStr) {
        RefreshTokenService.RotateResult rotateResult = refreshTokenService.rotate(refreshTokenStr);
        User user = rotateResult.entity().getUser();

        TenantContext.setTenantId(user.getTenantId());
        try {
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole(), user.getTenantId());
            return new AuthResponse(token, rotateResult.rawToken(), user.getUsername(), user.getRole());
        } finally {
            TenantContext.clear();
        }
    }

    public void logout(String refreshTokenStr) {
        try {
            refreshTokenService.logout(refreshTokenStr);
        } catch (RefreshTokenException e) {
            // If token is invalid/expired, still succeed — nothing to invalidate
        }
    }

    private String normalizeRole(String role) {
        if (role == null) return null;
        String upper = role.toUpperCase().trim();
        if (upper.startsWith("ROLE_")) {
            return upper;
        }
        return "ROLE_" + upper;
    }
}
