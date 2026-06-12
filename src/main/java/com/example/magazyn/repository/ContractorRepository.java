package com.example.magazyn.repository;

import com.example.magazyn.entity.Contractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractorRepository extends JpaRepository<Contractor, Long> {

    Optional<Contractor> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByIdAndTenantId(Long id, Long tenantId);

    List<Contractor> findByNameContainingIgnoreCase(String name);

    List<Contractor> findByTaxIdContaining(String taxId);
}
