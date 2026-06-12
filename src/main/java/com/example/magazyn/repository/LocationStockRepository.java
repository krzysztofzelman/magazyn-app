package com.example.magazyn.repository;

import com.example.magazyn.entity.LocationStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationStockRepository extends JpaRepository<LocationStock, Long> {

    Optional<LocationStock> findByIdAndTenantId(Long id, Long tenantId);

    List<LocationStock> findByLocationId(Long locationId);

    List<LocationStock> findByProductId(Long productId);

    Optional<LocationStock> findByLocationIdAndProductId(Long locationId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ls FROM LocationStock ls WHERE ls.locationId = :locationId AND ls.productId = :productId")
    Optional<LocationStock> findByLocationIdAndProductIdWithLock(Long locationId, Long productId);
}
