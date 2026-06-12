package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.CompanySettingsRequest;
import com.example.magazyn.dto.CompanySettingsResponse;
import com.example.magazyn.entity.CompanySettings;
import com.example.magazyn.repository.CompanySettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CompanySettingsService {

    private final CompanySettingsRepository repository;
    private final AuditLogService auditLogService;

    public CompanySettingsService(CompanySettingsRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public CompanySettingsResponse getSettings() {
        return repository.findByTenantId(TenantContext.getTenantId())
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional
    public CompanySettingsResponse upsertSettings(CompanySettingsRequest request, String username) {
        CompanySettings settings = repository.findByTenantId(TenantContext.getTenantId())
                .orElseGet(() -> {
                    CompanySettings s = new CompanySettings();
                    s.setTenantId(TenantContext.getTenantId());
                    return s;
                });

        settings.setName(request.name());
        settings.setTaxId(request.taxId());
        settings.setAddress(request.address());
        settings.setBankName(request.bankName());
        settings.setBankAccount(request.bankAccount());
        settings.setPhone(request.phone());
        settings.setEmail(request.email());

        CompanySettings saved = repository.save(settings);
        auditLogService.log(username, "UPSERT_COMPANY_SETTINGS", "CompanySettings", saved.getId(),
                "Updated company settings: " + saved.getName());
        return toResponse(saved);
    }

    private CompanySettingsResponse toResponse(CompanySettings s) {
        return new CompanySettingsResponse(
                s.getId(), s.getName(), s.getTaxId(), s.getAddress(),
                s.getBankName(), s.getBankAccount(), s.getPhone(), s.getEmail(), s.getUpdatedAt()
        );
    }
}
