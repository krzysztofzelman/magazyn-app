package com.example.magazyn.controller;

import com.example.magazyn.dto.ContractorRequest;
import com.example.magazyn.dto.ContractorResponse;
import com.example.magazyn.service.ContractorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contractors")
@Tag(name = "Contractors", description = "Contractor (supplier/customer) management")
@SecurityRequirement(name = "bearerAuth")
public class ContractorController {

    private final ContractorService contractorService;

    public ContractorController(ContractorService contractorService) {
        this.contractorService = contractorService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all contractors", description = "Returns a list of all contractors")
    public ResponseEntity<List<ContractorResponse>> getAllContractors() {
        return ResponseEntity.ok(contractorService.getAllContractors());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get contractor by ID")
    public ResponseEntity<ContractorResponse> getContractorById(@PathVariable Long id) {
        return ResponseEntity.ok(contractorService.getContractorById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new contractor")
    public ResponseEntity<ContractorResponse> createContractor(@Valid @RequestBody ContractorRequest request) {
        ContractorResponse created = contractorService.createContractor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing contractor")
    public ResponseEntity<ContractorResponse> updateContractor(@PathVariable Long id,
                                                               @Valid @RequestBody ContractorRequest request) {
        return ResponseEntity.ok(contractorService.updateContractor(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a contractor")
    public ResponseEntity<Void> deleteContractor(@PathVariable Long id) {
        contractorService.deleteContractor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Search contractors by name or taxId")
    public ResponseEntity<List<ContractorResponse>> searchContractors(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String taxId) {
        if (name != null && !name.isBlank()) {
            return ResponseEntity.ok(contractorService.searchByName(name));
        }
        if (taxId != null && !taxId.isBlank()) {
            return ResponseEntity.ok(contractorService.searchByTaxId(taxId));
        }
        return ResponseEntity.ok(contractorService.getAllContractors());
    }
}
