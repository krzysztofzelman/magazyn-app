package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.ChangePasswordRequest;
import com.example.magazyn.dto.UserRequest;
import com.example.magazyn.dto.UserResponse;
import com.example.magazyn.entity.User;
import com.example.magazyn.exception.DuplicateResourceException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       TenantService tenantService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
    }

    public UserResponse register(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        // Enforce tenant plan user limit
        if (!tenantService.canAddUser()) {
            throw new IllegalStateException(
                "Osi\u0105gni\u0119to limit u\u017Cytkownik\u00F3w dla tego planu. "
                + "Aby doda\u0107 wi\u0119cej u\u017Cytkownik\u00F3w, zaktualizuj plan.");
        }

        String role = normalizeRole(request.getRole());

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(role)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        return toResponse(user);
    }

    public List<UserResponse> getAllUsers() {
        Long tenantId = TenantContext.getTenantId();
        return userRepository.findAllByTenantId(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse getUser(Long id) {
        User user = userRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toResponse(user);
    }

    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsernameAndTenantId(username, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User", username));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User", id));

        if (request.getUsername() != null && !request.getUsername().isBlank()
                && !user.getUsername().equals(request.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException("Username already exists: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getRole() != null && !request.getRole().isBlank()) {
            user.setRole(normalizeRole(request.getRole()));
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setIsActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setIsActive(true);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndTenantId(userId, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Nieprawidłowe bieżące hasło");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private String normalizeRole(String role) {
        if (role == null) return null;
        String upper = role.toUpperCase().trim();
        if (upper.startsWith("ROLE_")) {
            return upper;
        }
        return "ROLE_" + upper;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getIsActive(),
                user.getCreatedAt()
        );
    }
}
