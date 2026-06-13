package com.example.magazyn.repository;

import com.example.magazyn.entity.Invoice;
import com.example.magazyn.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    Page<Invoice> findByTenantId(Long tenantId, Pageable pageable);

    List<Invoice> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, InvoiceStatus status);

    Optional<Invoice> findByDocumentId(Long documentId);

    @Query("SELECT MAX(i.number) FROM Invoice i WHERE i.number LIKE :prefix%")
    Optional<String> findMaxNumberByPrefix(@Param("prefix") String prefix);

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId AND YEAR(i.issueDate) = :year ORDER BY i.createdAt DESC")
    List<Invoice> findByTenantIdAndYear(@Param("tenantId") Long tenantId, @Param("year") int year);

    @Query("SELECT i FROM Invoice i WHERE i.tenantId = :tenantId AND i.status = :status AND YEAR(i.issueDate) = :year ORDER BY i.createdAt DESC")
    List<Invoice> findByTenantIdAndStatusAndYear(@Param("tenantId") Long tenantId,
                                                  @Param("status") InvoiceStatus status,
                                                  @Param("year") int year);
}
