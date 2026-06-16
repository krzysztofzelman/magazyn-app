package com.example.magazyn.repository;

import com.example.magazyn.entity.RefreshToken;
import com.example.magazyn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByIdAndTenantId(Long id, Long tenantId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUser(User user);
}
