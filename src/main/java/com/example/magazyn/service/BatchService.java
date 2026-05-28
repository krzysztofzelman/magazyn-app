package com.example.magazyn.service;

import com.example.magazyn.dto.BatchResponse;
import com.example.magazyn.entity.Batch;
import com.example.magazyn.repository.BatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BatchService {

    private final BatchRepository batchRepository;

    public BatchService(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public List<BatchResponse> getBatchesByProduct(Long productId) {
        return batchRepository.findByProductIdOrderByExpiryDateAsc(productId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BatchResponse> getExpiringBatches(int days) {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(days);
        return batchRepository.findExpiringBatches(today, threshold)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<BatchResponse> getExpiredBatches() {
        return batchRepository.findExpiredBatches(LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BatchResponse toResponse(Batch batch) {
        return new BatchResponse(
                batch.getId(),
                batch.getProduct().getId(),
                batch.getProduct().getName(),
                batch.getProduct().getSku(),
                batch.getLotNumber(),
                batch.getExpiryDate(),
                batch.getManufacturingDate(),
                batch.getQuantity(),
                batch.getLocationId(),
                batch.getCreatedAt()
        );
    }
}
