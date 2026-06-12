package com.example.magazyn.repository;

import com.example.magazyn.entity.Invoice;
import com.example.magazyn.entity.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Optional<Invoice> findByIdAndTenantId(Long id, Long tenantId);

    List<Invoice> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<Invoice> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, InvoiceStatus status);

    Optional<Invoice> findByDocumentId(Long documentId);

    @Query("SELECT MAX(i.number) FROM Invoice i WHERE i.number LIKE :prefix%")
    Optional<String> findMaxNumberByPrefix(@Param("prefix") String prefix);
}
