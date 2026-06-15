package com.example.magazyn.repository;

import com.example.magazyn.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByIdAndTenantId(Long id, Long tenantId);

    List<Location> findByParentIdAndTenantId(Long parentId, Long tenantId);

    List<Location> findByParentIdIsNullAndTenantId(Long tenantId);

    boolean existsByParentIdAndTenantId(Long parentId, Long tenantId);

    Optional<Location> findByBarcodeAndTenantId(String barcode, Long tenantId);

    List<Location> findAllByTenantId(Long tenantId);
}
