package com.example.magazyn.repository;

import com.example.magazyn.entity.LocationStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationStockRepository extends JpaRepository<LocationStock, Long> {

    List<LocationStock> findByLocationId(Long locationId);

    List<LocationStock> findByProductId(Long productId);

    Optional<LocationStock> findByLocationIdAndProductId(Long locationId, Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LocationStock> findByLocationIdAndProductIdForUpdate(Long locationId, Long productId);
}
