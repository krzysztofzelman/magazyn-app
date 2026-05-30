package com.example.magazyn.repository;

import com.example.magazyn.entity.InventorySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventorySessionRepository extends JpaRepository<InventorySession, Long> {

    List<InventorySession> findByStatus(String status);
}
