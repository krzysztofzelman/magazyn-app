package com.example.magazyn.ksef.repository;

import com.example.magazyn.ksef.model.entity.KsefSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface KsefSessionRepository extends JpaRepository<KsefSession, Long> {

    List<KsefSession> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @Query("SELECT s FROM KsefSession s WHERE s.tenantId = :tenantId AND s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<KsefSession> findActiveSessions(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM KsefSession s WHERE s.tenantId = :tenantId AND s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    Optional<KsefSession> findLatestActiveSession(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    long countByTenantIdAndIsActiveTrueAndExpiresAtAfter(Long tenantId, LocalDateTime now);

    @Query("UPDATE KsefSession s SET s.isActive = false WHERE s.expiresAt < :now")
    void expireSessions(@Param("now") LocalDateTime now);
}
