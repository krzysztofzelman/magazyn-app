package com.example.magazyn.auth;

import com.example.magazyn.entity.RefreshToken;
import com.example.magazyn.entity.User;
import com.example.magazyn.repository.UserRepository;
import com.example.magazyn.service.AuditLogService;
import com.example.magazyn.util.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role("ROLE_USER")
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);

        auditLogService.log(currentUsername(), "REGISTER", "User", user.getId(),
                "Registered user: " + user.getUsername());
        return new AuthResponse(token, refreshToken.getToken().toString(), user.getUsername(), user.getRole());
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
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);

            auditLogService.log(user.getUsername(), "LOGIN_SUCCESS", "User", user.getId(),
                    "Login successful for user: " + user.getUsername());
            return new AuthResponse(token, refreshToken.getToken().toString(), user.getUsername(), user.getRole());
        } catch (RuntimeException e) {
            auditLogService.log("anonymous", "LOGIN_FAILURE", "User", null,
                    "Login failed for username=" + request.getUsername() + " reason=" + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenStr) {
        User user = refreshTokenService.validateRefreshToken(refreshTokenStr);
        refreshTokenService.deleteByUser(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
        RefreshToken newRefreshToken = refreshTokenService.generateRefreshToken(user);
        return new AuthResponse(token, newRefreshToken.getToken().toString(), user.getUsername(), user.getRole());
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        try {
            User user = refreshTokenService.validateRefreshToken(refreshTokenStr);
            refreshTokenService.deleteByUser(user);
        } catch (RefreshTokenException e) {
            // If token is invalid/expired, still succeed — nothing to invalidate
        }
    }
}
