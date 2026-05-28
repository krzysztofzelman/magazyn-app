package com.example.magazyn.service;

import com.example.magazyn.dto.*;
import com.example.magazyn.entity.*;
import com.example.magazyn.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class WarehouseDocumentService {

    private final WarehouseDocumentRepository documentRepository;
    private final WarehouseDocumentItemRepository itemRepository;
    private final ContractorRepository contractorRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;
    private final StockService stockService;
    private final BatchRepository batchRepository;
    private final AuditLogService auditLogService;

    public WarehouseDocumentService(WarehouseDocumentRepository documentRepository,
                                    WarehouseDocumentItemRepository itemRepository,
                                    ContractorRepository contractorRepository,
                                    ProductRepository productRepository,
                                    StockMovementRepository stockMovementRepository,
                                    StockService stockService,
                                    BatchRepository batchRepository,
                                    AuditLogService auditLogService) {
        this.documentRepository = documentRepository;
        this.itemRepository = itemRepository;
        this.contractorRepository = contractorRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockService = stockService;
        this.batchRepository = batchRepository;
        this.auditLogService = auditLogService;
    }

    public WarehouseDocumentResponse createDocument(WarehouseDocumentRequest request, String username) {
        Contractor contractor = contractorRepository.findById(request.getContractorId())
                .orElseThrow(() -> new RuntimeException("Contractor not found with id: " + request.getContractorId()));

        String number = generateDocumentNumber(request.getType());

        WarehouseDocument document = WarehouseDocument.builder()
                .number(number)
                .type(request.getType())
                .contractor(contractor)
                .status(DocumentStatus.DRAFT)
                .createdBy(username)
                .notes(request.getNotes())
                .items(new ArrayList<>())
                .build();

        List<WarehouseDocumentItem> items = new ArrayList<>();
        for (WarehouseDocumentItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + itemReq.getProductId()));

            WarehouseDocumentItem item = WarehouseDocumentItem.builder()
                    .document(document)
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice() != null ? itemReq.getUnitPrice() : BigDecimal.ZERO)
                    .lotNumber(itemReq.getLotNumber())
                    .expiryDate(itemReq.getExpiryDate())
                    .manufacturingDate(itemReq.getManufacturingDate())
                    .build();
            items.add(item);
        }
        document.setItems(items);

        WarehouseDocument saved = documentRepository.save(document);
        auditLogService.log(username, "DOCUMENT_CREATE", "WarehouseDocument", saved.getId(),
                "type=" + request.getType() + " number=" + number);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<WarehouseDocumentResponse> getDocuments(DocumentType type, DocumentStatus status, Pageable pageable) {
        Page<WarehouseDocument> page;
        if (type != null && status != null) {
            page = documentRepository.findByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            page = documentRepository.findByType(type, pageable);
        } else if (status != null) {
            page = documentRepository.findByStatus(status, pageable);
        } else {
            page = documentRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WarehouseDocumentResponse getDocumentById(Long id) {
        WarehouseDocument document = documentRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Warehouse document not found with id: " + id));
        return toResponse(document);
    }

    public WarehouseDocumentResponse confirmDocument(Long id, String username) {
        WarehouseDocument document = documentRepository.findByIdWithItemsLocked(id)
                .orElseThrow(() -> new RuntimeException("Warehouse document not found with id: " + id));

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT documents can be confirmed. Current status: " + document.getStatus());
        }

        if (document.getType() == DocumentType.PZ) {
            confirmPZ(document, username);
        } else {
            confirmWZ(document, username);
        }

        document.setStatus(DocumentStatus.CONFIRMED);
        document.setConfirmedAt(LocalDateTime.now());
        documentRepository.save(document);

        auditLogService.log(username, "DOCUMENT_CONFIRM", "WarehouseDocument", id,
                "type=" + document.getType() + " number=" + document.getNumber());

        return toResponse(document);
    }

    public WarehouseDocumentResponse cancelDocument(Long id, String username) {
        WarehouseDocument document = documentRepository.findByIdWithItems(id)
                .orElseThrow(() -> new RuntimeException("Warehouse document not found with id: " + id));

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new RuntimeException("Only DRAFT documents can be cancelled. Current status: " + document.getStatus());
        }

        document.setStatus(DocumentStatus.CANCELLED);
        documentRepository.save(document);

        auditLogService.log(username, "DOCUMENT_CANCEL", "WarehouseDocument", id,
                "type=" + document.getType() + " number=" + document.getNumber());

        return toResponse(document);
    }

    private void confirmPZ(WarehouseDocument document, String username) {
        for (WarehouseDocumentItem item : document.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + item.getProduct().getId()));

            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);

            Long batchId = null;

            // If lot number is provided, create or update a batch
            if (item.getLotNumber() != null && !item.getLotNumber().isBlank()) {
                List<Batch> existingBatches = batchRepository.findByProductIdOrderByCreatedAtAsc(product.getId());
                Batch batch = existingBatches.stream()
                        .filter(b -> b.getLotNumber().equals(item.getLotNumber()))
                        .findFirst()
                        .orElse(null);

                if (batch != null) {
                    batch.setQuantity(batch.getQuantity() + item.getQuantity());
                    if (item.getExpiryDate() != null) batch.setExpiryDate(item.getExpiryDate());
                    if (item.getManufacturingDate() != null) batch.setManufacturingDate(item.getManufacturingDate());
                } else {
                    batch = Batch.builder()
                            .product(product)
                            .lotNumber(item.getLotNumber())
                            .expiryDate(item.getExpiryDate())
                            .manufacturingDate(item.getManufacturingDate())
                            .quantity(item.getQuantity())
                            .locationId(product.getLocationId())
                            .build();
                }
                Batch savedBatch = batchRepository.save(batch);
                batchId = savedBatch.getId();
            }

            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .type(MovementType.PRZYJECIE)
                    .quantity(item.getQuantity())
                    .note("PZ " + document.getNumber())
                    .createdBy(username)
                    .batchId(batchId)
                    .build();
            stockMovementRepository.save(movement);
        }
    }

    private void confirmWZ(WarehouseDocument document, String username) {
        List<String> insufficientProducts = new ArrayList<>();

        // Phase 1: check all products first
        for (WarehouseDocumentItem item : document.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + item.getProduct().getId()));

            if (product.getQuantity() < item.getQuantity()) {
                insufficientProducts.add(product.getName() + " (available: " + product.getQuantity()
                        + ", requested: " + item.getQuantity() + ")");
            }
        }

        if (!insufficientProducts.isEmpty()) {
            throw new RuntimeException("Insufficient stock for products: " + String.join(", ", insufficientProducts));
        }

        // Phase 2: execute movements with FIFO batch deduction
        for (WarehouseDocumentItem item : document.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + item.getProduct().getId()));

            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);

            // FIFO batch deduction: deduct from oldest batches first
            List<Batch> batches = batchRepository.findByProductIdOrderByCreatedAtAsc(product.getId());
            int remaining = item.getQuantity();

            for (Batch batch : batches) {
                if (remaining <= 0 || batch.getQuantity() <= 0) continue;

                Batch lockedBatch = batchRepository.findByIdForUpdate(batch.getId())
                        .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batch.getId()));

                int deductFromThis = Math.min(remaining, lockedBatch.getQuantity());
                lockedBatch.setQuantity(lockedBatch.getQuantity() - deductFromThis);
                batchRepository.save(lockedBatch);
                remaining -= deductFromThis;
            }

            StockMovement movement = StockMovement.builder()
                    .product(product)
                    .type(MovementType.WYDANIE)
                    .quantity(item.getQuantity())
                    .note("WZ " + document.getNumber())
                    .createdBy(username)
                    .build();
            stockMovementRepository.save(movement);
        }
    }

    private String generateDocumentNumber(DocumentType type) {
        String prefix = type.name() + "/" + Year.now().getValue() + "/";
        String maxNumber = documentRepository.findMaxNumberByTypeAndYear(type, prefix).orElse(null);

        int nextSeq = 1;
        if (maxNumber != null) {
            String seqPart = maxNumber.substring(prefix.length());
            try {
                nextSeq = Integer.parseInt(seqPart) + 1;
            } catch (NumberFormatException ignored) {
                // fallback to 1 if format is unexpected
            }
        }

        return prefix + String.format("%03d", nextSeq);
    }

    private WarehouseDocumentResponse toResponse(WarehouseDocument doc) {
        List<WarehouseDocumentItemResponse> itemResponses = doc.getItems().stream()
                .map(this::toItemResponse)
                .collect(Collectors.toList());

        return new WarehouseDocumentResponse(
                doc.getId(),
                doc.getNumber(),
                doc.getType(),
                doc.getStatus(),
                doc.getContractor().getId(),
                doc.getContractor().getName(),
                doc.getContractor().getTaxId(),
                doc.getCreatedAt(),
                doc.getConfirmedAt(),
                doc.getCreatedBy(),
                doc.getNotes(),
                itemResponses
        );
    }

    private WarehouseDocumentItemResponse toItemResponse(WarehouseDocumentItem item) {
        BigDecimal totalPrice = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new WarehouseDocumentItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSku(),
                item.getProduct().getUnit(),
                item.getQuantity(),
                item.getUnitPrice(),
                totalPrice,
                item.getLotNumber(),
                item.getExpiryDate(),
                item.getManufacturingDate()
        );
    }
}
