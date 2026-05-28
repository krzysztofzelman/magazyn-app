package com.example.magazyn.repository;

import com.example.magazyn.entity.Contractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractorRepository extends JpaRepository<Contractor, Long> {

    List<Contractor> findByNameContainingIgnoreCase(String name);

    List<Contractor> findByTaxIdContaining(String taxId);
}
