package com.example.magazyn.repository;

import com.example.magazyn.entity.RefreshToken;
import com.example.magazyn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(UUID token);

    void deleteByUser(User user);
}
