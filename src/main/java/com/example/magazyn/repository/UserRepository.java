package com.example.magazyn.repository;

import com.example.magazyn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndTenantId(String username, Long tenantId);

    List<User> findAllByTenantId(Long tenantId);

    boolean existsByUsername(String username);

    List<User> findByRole(String role);

    long countByTenantId(Long tenantId);

    List<User> findByRoleAndTenantId(String role, Long tenantId);
}
