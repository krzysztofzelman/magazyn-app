package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.ApiKeyResponse;
import com.example.magazyn.dto.RegisterTenantRequest;
import com.example.magazyn.dto.TenantRegistrationResponse;
import com.example.magazyn.dto.TenantResponse;
import com.example.magazyn.entity.Tenant;
import com.example.magazyn.entity.User;
import com.example.magazyn.exception.DuplicateResourceException;
import com.example.magazyn.repository.TenantRepository;
import com.example.magazyn.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TenantService(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public TenantRegistrationResponse register(RegisterTenantRequest request) {
        if (tenantRepository.existsBySubdomain(request.getSubdomain())) {
            throw new DuplicateResourceException(
                    "Subdomena '" + request.getSubdomain() + "' jest ju\u017C zaj\u0119ta");
        }
        if (userRepository.existsByUsername(request.getAdminUsername())) {
            throw new DuplicateResourceException(
                    "Nazwa u\u017Cytkownika '" + request.getAdminUsername() + "' jest ju\u017C zaj\u0119ta");
        }

        // Create tenant
        Tenant tenant = Tenant.builder()
                .name(request.getCompanyName())
                .subdomain(request.getSubdomain().toLowerCase().trim())
                .plan("free")
                .maxUsers(3)
                .isActive(true)
                .build();
        tenant = tenantRepository.save(tenant);

        // Create admin user for this tenant
        // Set tenant context so @PrePersist on User sets tenant_id
        TenantContext.setTenantId(tenant.getId());
        try {
            User admin = User.builder()
                    .username(request.getAdminUsername())
                    .password(passwordEncoder.encode(request.getAdminPassword()))
                    .email(request.getAdminEmail())
                    .role("ROLE_ADMIN")
                    .isActive(true)
                    .build();
            userRepository.save(admin);

            log.info("Registered new tenant: {} (subdomain={}, id={}), admin user: {}",
                    tenant.getName(), tenant.getSubdomain(), tenant.getId(), admin.getUsername());

            return new TenantRegistrationResponse(
                    "Konto firmy utworzone. Mo\u017Cesz si\u0119 zalogowa\u0107.",
                    admin.getUsername(), tenant.getName(), tenant.getSubdomain());
        } finally {
            TenantContext.clear();
        }
    }

    public TenantResponse getCurrentTenantInfo() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Brak kontekstu tenanta");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant nie znaleziony"));

        long userCount = userRepository.countByTenantId(tenantId);

        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getPlan(),
                tenant.getMaxUsers(),
                userCount,
                tenant.getIsActive(),
                tenant.getCreatedAt()
        );
    }

    public boolean canAddUser() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) return true; // fallback — allow

        Tenant tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) return true;

        long currentUsers = userRepository.countByTenantId(tenantId);
        return currentUsers < tenant.getMaxUsers();
    }

    public ApiKeyResponse getApiKey() {
        Long tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant nie znaleziony"));
        return new ApiKeyResponse(tenant.getApiKey());
    }

    @Transactional
    public ApiKeyResponse regenerateApiKey() {
        Long tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant nie znaleziony"));
        tenant.setApiKey(java.util.UUID.randomUUID().toString());
        tenantRepository.save(tenant);
        log.info("API key regenerated for tenant id={}", tenantId);
        return new ApiKeyResponse(tenant.getApiKey());
    }
}
