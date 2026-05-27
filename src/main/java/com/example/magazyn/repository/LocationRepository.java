package com.example.magazyn.repository;

import com.example.magazyn.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findByParentId(Long parentId);

    List<Location> findByParentIdIsNull();

    boolean existsByParentId(Long parentId);
}
