package com.example.magazyn.service;

import com.example.magazyn.dto.ContractorRequest;
import com.example.magazyn.dto.ContractorResponse;
import com.example.magazyn.entity.Contractor;
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
        return contractorRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContractorResponse getContractorById(Long id) {
        Contractor contractor = contractorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contractor not found with id: " + id));
        return toResponse(contractor);
    }

    public ContractorResponse createContractor(ContractorRequest request) {
        if (request.getTaxId() != null && !request.getTaxId().isBlank()) {
            contractorRepository.findByTaxIdContaining(request.getTaxId()).stream()
                    .filter(c -> c.getTaxId().equals(request.getTaxId()))
                    .findAny()
                    .ifPresent(c -> {
                        throw new RuntimeException("Contractor with taxId " + request.getTaxId() + " already exists");
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
                .build();

        Contractor saved = contractorRepository.save(contractor);
        return toResponse(saved);
    }

    public ContractorResponse updateContractor(Long id, ContractorRequest request) {
        Contractor contractor = contractorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contractor not found with id: " + id));

        if (request.getName() != null) contractor.setName(request.getName());
        if (request.getTaxId() != null) contractor.setTaxId(request.getTaxId());
        if (request.getAddress() != null) contractor.setAddress(request.getAddress());
        if (request.getEmail() != null) contractor.setEmail(request.getEmail());
        if (request.getPhone() != null) contractor.setPhone(request.getPhone());
        if (request.getType() != null) contractor.setType(request.getType());
        if (request.getActive() != null) contractor.setActive(request.getActive());

        Contractor saved = contractorRepository.save(contractor);
        return toResponse(saved);
    }

    public void deleteContractor(Long id) {
        if (!contractorRepository.existsById(id)) {
            throw new RuntimeException("Contractor not found with id: " + id);
        }
        contractorRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ContractorResponse> searchByName(String name) {
        return contractorRepository.findByNameContainingIgnoreCase(name).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ContractorResponse> searchByTaxId(String taxId) {
        return contractorRepository.findByTaxIdContaining(taxId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private ContractorResponse toResponse(Contractor c) {
        return new ContractorResponse(
                c.getId(), c.getName(), c.getTaxId(), c.getAddress(),
                c.getEmail(), c.getPhone(), c.getType(), c.getActive(), c.getCreatedAt()
        );
    }
}
