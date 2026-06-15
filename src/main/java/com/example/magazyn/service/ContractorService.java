package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.ContractorRequest;
import com.example.magazyn.dto.ContractorResponse;
import com.example.magazyn.entity.Contractor;
import com.example.magazyn.exception.DuplicateResourceException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.ContractorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ContractorService {

    private final ContractorRepository contractorRepository;

    public ContractorService(ContractorRepository contractorRepository) {
        this.contractorRepository = contractorRepository;
    }

    @Transactional(readOnly = true)
    public List<ContractorResponse> getAllContractors() {
        Long tenantId = TenantContext.getTenantId();
        return contractorRepository.findAllByTenantId(tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractorResponse getContractorById(Long id) {
        Contractor contractor = contractorRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Contractor", id));
        return toResponse(contractor);
    }

    public ContractorResponse createContractor(ContractorRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (request.getTaxId() != null && !request.getTaxId().isBlank()) {
            contractorRepository.findByTaxIdContainingAndTenantId(request.getTaxId(), tenantId).stream()
                    .filter(c -> c.getTaxId().equals(request.getTaxId()))
                    .findAny()
                    .ifPresent(c -> {
                        throw new DuplicateResourceException("Contractor with taxId " + request.getTaxId() + " already exists");
                    });
        }

        Contractor contractor = Contractor.builder()
                .name(request.getName())
                .taxId(request.getTaxId())
                .address(request.getAddress())
                .email(request.getEmail())
                .phone(request.getPhone())
                .type(request.getType())
                .active(request.getActive() != null ? request.getActive() : true)
                .bankAccount(request.getBankAccount())
                .paymentDays(request.getPaymentDays())
                .paymentMethod(request.getPaymentMethod())
                .build();

        Contractor saved = contractorRepository.save(contractor);
        return toResponse(saved);
    }

    public ContractorResponse updateContractor(Long id, ContractorRequest request) {
        Contractor contractor = contractorRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Contractor", id));

        if (request.getName() != null) contractor.setName(request.getName());
        if (request.getTaxId() != null) contractor.setTaxId(request.getTaxId());
        if (request.getAddress() != null) contractor.setAddress(request.getAddress());
        if (request.getEmail() != null) contractor.setEmail(request.getEmail());
        if (request.getPhone() != null) contractor.setPhone(request.getPhone());
        if (request.getType() != null) contractor.setType(request.getType());
        if (request.getActive() != null) contractor.setActive(request.getActive());
        if (request.getBankAccount() != null) contractor.setBankAccount(request.getBankAccount());
        if (request.getPaymentDays() != null) contractor.setPaymentDays(request.getPaymentDays());
        if (request.getPaymentMethod() != null) contractor.setPaymentMethod(request.getPaymentMethod());

        Contractor saved = contractorRepository.save(contractor);
        return toResponse(saved);
    }

    public void deleteContractor(Long id) {
        Contractor contractor = contractorRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Contractor", id));
        contractorRepository.delete(contractor);
    }

    @Transactional(readOnly = true)
    public List<ContractorResponse> searchByName(String name) {
        Long tenantId = TenantContext.getTenantId();
        return contractorRepository.findByNameContainingIgnoreCaseAndTenantId(name, tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractorResponse> searchByTaxId(String taxId) {
        Long tenantId = TenantContext.getTenantId();
        return contractorRepository.findByTaxIdContainingAndTenantId(taxId, tenantId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ContractorResponse toResponse(Contractor c) {
        return new ContractorResponse(
                c.getId(), c.getName(), c.getTaxId(), c.getAddress(),
                c.getEmail(), c.getPhone(), c.getType(), c.getActive(), c.getCreatedAt(),
                c.getBankAccount(), c.getPaymentDays(), c.getPaymentMethod()
        );
    }
}
