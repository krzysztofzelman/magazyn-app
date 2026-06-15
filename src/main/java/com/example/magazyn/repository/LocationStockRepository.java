package com.example.magazyn.repository;

import com.example.magazyn.entity.LocationStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationStockRepository extends JpaRepository<LocationStock, Long> {

    Optional<LocationStock> findByIdAndTenantId(Long id, Long tenantId);

    List<LocationStock> findByTenantId(Long tenantId);

    List<LocationStock> findByLocationIdAndTenantId(Long locationId, Long tenantId);

    List<LocationStock> findByProductIdAndTenantId(Long productId, Long tenantId);

    Optional<LocationStock> findByLocationIdAndProductIdAndTenantId(Long locationId, Long productId, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ls FROM LocationStock ls WHERE ls.locationId = :locationId AND ls.productId = :productId AND ls.tenantId = :tenantId")
    Optional<LocationStock> findByLocationIdAndProductIdWithLock(@Param("locationId") Long locationId, @Param("productId") Long productId, @Param("tenantId") Long tenantId);
}
