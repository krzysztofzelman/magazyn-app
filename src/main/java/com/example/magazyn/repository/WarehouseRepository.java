package com.example.magazyn.repository;

import com.example.magazyn.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByTenantIdOrderByName(Long tenantId);

    Optional<Warehouse> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByCodeAndTenantId(String code, Long tenantId);
}
