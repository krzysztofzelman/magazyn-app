package com.example.magazyn.service;

import com.example.magazyn.dto.*;
import com.example.magazyn.entity.*;
import com.example.magazyn.exception.InsufficientStockException;
import com.example.magazyn.exception.InvalidOperationException;
import com.example.magazyn.exception.ResourceNotFoundException;
import com.example.magazyn.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final ReservationService reservationService;
    private final LocationRepository locationRepository;
    private final LocationStockRepository locationStockRepository;

    public WarehouseDocumentService(WarehouseDocumentRepository documentRepository,
                                    WarehouseDocumentItemRepository itemRepository,
                                    ContractorRepository contractorRepository,
                                    ProductRepository productRepository,
                                    StockMovementRepository stockMovementRepository,
                                    StockService stockService,
                                    BatchRepository batchRepository,
                                    AuditLogService auditLogService,
                                    ReservationService reservationService,
                                    LocationRepository locationRepository,
                                    LocationStockRepository locationStockRepository) {
        this.documentRepository = documentRepository;
        this.itemRepository = itemRepository;
        this.contractorRepository = contractorRepository;
        this.productRepository = productRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.stockService = stockService;
        this.batchRepository = batchRepository;
        this.auditLogService = auditLogService;
        this.reservationService = reservationService;
        this.locationRepository = locationRepository;
        this.locationStockRepository = locationStockRepository;
    }

    public WarehouseDocumentResponse createDocument(WarehouseDocumentRequest request, String username) {
        Contractor contractor = contractorRepository.findById(request.getContractorId())
                .orElseThrow(() -> new ResourceNotFoundException("Contractor", request.getContractorId()));

        // Retry up to 3 times in case of concurrent document number collision,
        // with exponential backoff to reduce contention on retries.
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return doCreateDocument(request, username, contractor);
            } catch (DataIntegrityViolationException e) {
                if (attempt == 2) {
                    throw e;
                }
                // Exponential backoff: 100ms, 200ms
                try {
                    Thread.sleep(100L * (1L << attempt));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        // Should never reach here
        throw new RuntimeException("Failed to create document after retries");
    }

    private WarehouseDocumentResponse doCreateDocument(WarehouseDocumentRequest request, String username,
                                                        Contractor contractor) {
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
                    .orElseThrow(() -> new ResourceNotFoundException("Product", itemReq.getProductId()));

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
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocument", id));
        return toResponse(document);
    }

    public LocationResponse scanLocationForDocument(Long docId, String barcode) {
        WarehouseDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocument", docId));

        if (document.getType() != DocumentType.PZ) {
            throw new InvalidOperationException("Scan-location is only supported for PZ documents");
        }

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new InvalidOperationException("Only DRAFT documents can be updated");
        }

        Location location = locationRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Location with barcode " + barcode));

        // Assign this location to all items that don't have a location yet
        for (WarehouseDocumentItem item : document.getItems()) {
            if (item.getLocationId() == null) {
                item.setLocationId(location.getId());
            }
        }
        documentRepository.save(document);

        return toLocationResponse(location);
    }

    public WarehouseDocumentItemResponse scanLocationForItem(Long docId, Long itemId, String barcode, String username) {
        WarehouseDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocument", docId));

        if (document.getType() != DocumentType.PZ) {
            throw new InvalidOperationException("Scan-location is only supported for PZ documents");
        }

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new InvalidOperationException("Only DRAFT documents can be updated");
        }

        WarehouseDocumentItem item = document.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocumentItem", itemId));

        Location location = locationRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Location with barcode " + barcode));

        item.setLocationId(location.getId());
        itemRepository.save(item);

        auditLogService.log(username, "LOCATION_SCAN_ITEM", "WarehouseDocumentItem", itemId,
                "locationId=" + location.getId() + " barcode=" + barcode + " doc=" + document.getNumber());

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
                item.getManufacturingDate(),
                location.getId(),
                location.getCode()
        );
    }

    private void updateLocationStock(Long locationId, Long productId, Integer quantity) {
        LocationStock stock = locationStockRepository.findByLocationIdAndProductId(locationId, productId)
                .orElse(null);

        if (stock != null) {
            stock.setQuantity(stock.getQuantity().add(BigDecimal.valueOf(quantity)));
            stock.setUpdatedAt(LocalDateTime.now());
        } else {
            stock = LocationStock.builder()
                    .locationId(locationId)
                    .productId(productId)
                    .quantity(BigDecimal.valueOf(quantity))
                    .reservedQuantity(BigDecimal.ZERO)
                    .updatedAt(LocalDateTime.now())
                    .build();
        }
        locationStockRepository.save(stock);
    }

    private LocationResponse toLocationResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getType().name(),
                location.getParentId(),
                location.getDescription(),
                location.getBarcode(),
                location.getQrData(),
                location.getCapacity(),
                location.getOccupied(),
                location.getZone(),
                location.getIsActive()
        );
    }

    public WarehouseDocumentResponse confirmDocument(Long id, String username) {
        WarehouseDocument document = documentRepository.findByIdWithItemsLocked(id)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocument", id));

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new InvalidOperationException("Only DRAFT documents can be confirmed. Current status: " + document.getStatus());
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
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocument", id));

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new InvalidOperationException("Only DRAFT documents can be cancelled. Current status: " + document.getStatus());
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
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProduct().getId()));

            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepository.save(product);

            Long batchId = null;

            // If lot number is provided and product tracks expiry, create or update a batch.
            // Uses PESSIMISTIC_WRITE on lotNumber lookup to prevent concurrent batch duplication.
            if (item.getLotNumber() != null && !item.getLotNumber().isBlank()
                    && Boolean.TRUE.equals(product.getTrackExpiry())) {
                Batch batch = batchRepository.findByProductIdAndLotNumberForUpdate(product.getId(), item.getLotNumber())
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

            // Update location_stock and location.occupied if item has a location assigned
            if (item.getLocationId() != null) {
                LocationStock stock = locationStockRepository
                        .findByLocationIdAndProductIdWithLock(item.getLocationId(), item.getProduct().getId())
                        .orElse(null);

                if (stock != null) {
                    stock.setQuantity(stock.getQuantity().add(BigDecimal.valueOf(item.getQuantity())));
                    stock.setUpdatedAt(LocalDateTime.now());
                } else {
                    stock = LocationStock.builder()
                            .locationId(item.getLocationId())
                            .productId(item.getProduct().getId())
                            .quantity(BigDecimal.valueOf(item.getQuantity()))
                            .reservedQuantity(BigDecimal.ZERO)
                            .updatedAt(LocalDateTime.now())
                            .build();
                }
                locationStockRepository.save(stock);

                // Update location.occupied
                updateLocationOccupied(item.getLocationId());
            }
        }
    }

    public WzScanResponse scanLocationForWzItem(Long docId, Long itemId, String barcode, String username) {
        WarehouseDocument document = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocument", docId));

        if (document.getType() != DocumentType.WZ) {
            throw new InvalidOperationException("Scan-location is only supported for WZ documents");
        }

        if (document.getStatus() != DocumentStatus.DRAFT) {
            throw new InvalidOperationException("Only DRAFT documents can be updated");
        }

        WarehouseDocumentItem item = document.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocumentItem", itemId));

        Location location = locationRepository.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("Location with barcode " + barcode));

        LocationStock stock = locationStockRepository
                .findByLocationIdAndProductId(location.getId(), item.getProduct().getId())
                .orElse(null);

        BigDecimal available = (stock != null)
                ? stock.getQuantity().subtract(stock.getReservedQuantity())
                : BigDecimal.ZERO;

        item.setLocationId(location.getId());
        itemRepository.save(item);

        auditLogService.log(username, "LOCATION_SCAN_WZ_ITEM", "WarehouseDocumentItem", itemId,
                "locationId=" + location.getId() + " barcode=" + barcode
                + " product=" + item.getProduct().getName() + " available=" + available);

        return new WzScanResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getSku(),
                item.getProduct().getUnit(),
                item.getQuantity(),
                location.getId(),
                location.getCode(),
                available,
                available.compareTo(BigDecimal.valueOf(item.getQuantity())) >= 0
        );
    }

    private void updateLocationOccupied(Long locationId) {
        List<LocationStock> stocks = locationStockRepository.findByLocationId(locationId);
        int totalOccupied = stocks.stream()
                .map(s -> s.getQuantity().setScale(0, RoundingMode.UP).intValue())
                .reduce(0, Integer::sum);

        Location location = locationRepository.findById(locationId).orElse(null);
        if (location != null) {
            location.setOccupied(totalOccupied);
            locationRepository.save(location);
        }
    }

    private void confirmWZ(WarehouseDocument document, String username) {
        List<String> insufficientProducts = new ArrayList<>();

        // Phase 1: check all products first against available stock (considering reservations)
        for (WarehouseDocumentItem item : document.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProduct().getId()));

            int reservedQuantity = reservationService.getActiveReservedQuantity(product.getId());
            int availableQuantity = product.getQuantity() - reservedQuantity;

            if (availableQuantity < item.getQuantity()) {
                insufficientProducts.add(product.getName()
                        + " (available: " + Math.max(availableQuantity, 0)
                        + ", total: " + product.getQuantity()
                        + ", reserved: " + reservedQuantity
                        + ", requested: " + item.getQuantity() + ")");
            }
        }

        if (!insufficientProducts.isEmpty()) {
            throw new InsufficientStockException("Insufficient stock for products: " + String.join(", ", insufficientProducts));
        }

        // Phase 2: execute movements with FIFO batch deduction and fulfill reservations
        for (WarehouseDocumentItem item : document.getItems()) {
            Product product = productRepository.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.getProduct().getId()));

            product.setQuantity(product.getQuantity() - item.getQuantity());
            productRepository.save(product);

            // Fulfill active reservations for this product (FIFO)
            reservationService.fulfillActiveReservations(product.getId(), item.getQuantity(), username);

            // FIFO batch deduction: deduct from oldest batches first
            List<Batch> batches = batchRepository.findByProductIdOrderByCreatedAtAsc(product.getId());
            int remaining = item.getQuantity();

            for (Batch batch : batches) {
                if (remaining <= 0 || batch.getQuantity() <= 0) continue;

                Batch lockedBatch = batchRepository.findByIdForUpdate(batch.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Batch", batch.getId()));

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

            // Decrease location_stock if item has a location assigned
            if (item.getLocationId() != null) {
                LocationStock stock = locationStockRepository
                        .findByLocationIdAndProductIdWithLock(item.getLocationId(), item.getProduct().getId())
                        .orElse(null);

                if (stock != null) {
                    BigDecimal newQuantity = stock.getQuantity().subtract(BigDecimal.valueOf(item.getQuantity()));
                    if (newQuantity.compareTo(BigDecimal.ZERO) <= 0) {
                        locationStockRepository.delete(stock);
                    } else {
                        stock.setQuantity(newQuantity);
                        stock.setUpdatedAt(LocalDateTime.now());
                        locationStockRepository.save(stock);
                    }

                    // Update location.occupied
                    updateLocationOccupied(item.getLocationId());
                }
            }
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

    public byte[] exportDocumentPdf(Long id) {
        WarehouseDocument doc = documentRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseDocument", id));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument pdfDoc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            pdfDoc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(pdfDoc, page)) {
                float margin = 40f;
                float y = page.getMediaBox().getHeight() - margin;
                float pageW = page.getMediaBox().getWidth() - 2 * margin;
                float col0 = margin;
                float col1 = margin + pageW * 2 / 10; // label column
                float col2 = margin + pageW * 5 / 10;
                float col3 = margin + pageW * 7.5f / 10;
                float[] colStarts = {col0, col1, col2, col3};
                float[] colWidths = {pageW * 2 / 10, pageW * 3 / 10, pageW * 2.5f / 10, pageW * 2.5f / 10};

                PDFont fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDFont fontReg = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                int titleSize = 18;
                int normalSize = 10;
                int tableHeaderSize = 9;
                int tableRowSize = 8;

                // Title
                float tw = fontBold.getStringWidth("Warehouse Document") / 1000f * titleSize;
                cs.setFont(fontBold, titleSize);
                cs.beginText();
                cs.newLineAtOffset(margin + (pageW - tw) / 2, y - titleSize);
                cs.showText("Warehouse Document");
                cs.endText();
                y -= titleSize + 8;

                // Document info
                String[] infoLines = {
                    "Number: " + doc.getNumber(),
                    "Type: " + doc.getType(),
                    "Status: " + doc.getStatus(),
                    "Contractor: " + doc.getContractor().getName(),
                    "Date: " + doc.getCreatedAt().toLocalDate().toString(),
                    "Created by: " + doc.getCreatedBy()
                };
                if (doc.getNotes() != null && !doc.getNotes().isBlank()) {
                    infoLines = java.util.Arrays.copyOf(infoLines, infoLines.length + 1);
                    infoLines[infoLines.length - 1] = "Notes: " + doc.getNotes();
                }

                cs.setFont(fontReg, normalSize);
                for (String line : infoLines) {
                    cs.beginText();
                    cs.newLineAtOffset(margin, y - normalSize);
                    cs.showText(line);
                    cs.endText();
                    y -= normalSize + 2;
                }

                y -= 8;

                // Table header
                float headerH = tableHeaderSize + 6;
                float rowH = tableRowSize + 6;
                String[] headers = {"Product", "Quantity", "Unit Price", "Total"};

                cs.setFont(fontBold, tableHeaderSize);
                for (int i = 0; i < headers.length; i++) {
                    cs.beginText();
                    cs.newLineAtOffset(colStarts[i] + 2, y - headerH + 2);
                    cs.showText(headers[i]);
                    cs.endText();
                }
                // Draw header rect
                cs.setLineWidth(0.5f);
                for (int i = 0; i < colStarts.length; i++) {
                    cs.addRect(colStarts[i], y - headerH, colWidths[i], headerH);
                }
                cs.stroke();
                y -= headerH;

                // Table rows
                cs.setFont(fontReg, tableRowSize);
                for (WarehouseDocumentItem item : doc.getItems()) {
                    BigDecimal totalPrice = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    String[] rowData = {
                        item.getProduct().getName(),
                        String.valueOf(item.getQuantity()),
                        item.getUnitPrice().toString(),
                        totalPrice.toString()
                    };

                    for (int i = 0; i < rowData.length; i++) {
                        cs.beginText();
                        cs.newLineAtOffset(colStarts[i] + 2, y - rowH + 2);
                        // Truncate long text
                        String txt = rowData[i];
                        float txtW = fontReg.getStringWidth(txt) / 1000f * tableRowSize;
                        if (txtW > colWidths[i] - 4) {
                            // Approximate truncation
                            int maxChars = (int) ((colWidths[i] - 4) / (fontReg.getStringWidth("W") / 1000f * tableRowSize));
                            if (maxChars > 3 && txt.length() > maxChars) {
                                txt = txt.substring(0, maxChars - 3) + "...";
                            }
                        }
                        cs.showText(txt);
                        cs.endText();
                    }
                    // Draw cell rects
                    for (int i = 0; i < colStarts.length; i++) {
                        cs.addRect(colStarts[i], y - rowH, colWidths[i], rowH);
                    }
                    cs.stroke();
                    y -= rowH;
                }
            }
            pdfDoc.save(baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export document PDF", e);
        }
        return baos.toByteArray();
    }

    private WarehouseDocumentResponse toResponse(WarehouseDocument doc) {
        // Batch-load location codes to avoid N+1 in toItemResponse
        Map<Long, String> locationCodeMap = new HashMap<>();
        List<Long> locationIds = doc.getItems().stream()
                .map(WarehouseDocumentItem::getLocationId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!locationIds.isEmpty()) {
            locationRepository.findAllById(locationIds).forEach(
                    loc -> locationCodeMap.put(loc.getId(), loc.getCode()));
        }

        List<WarehouseDocumentItemResponse> itemResponses = doc.getItems().stream()
                .map(item -> toItemResponse(item, locationCodeMap))
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

    private WarehouseDocumentItemResponse toItemResponse(WarehouseDocumentItem item,
                                                          Map<Long, String> locationCodeMap) {
        BigDecimal totalPrice = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));

        String locationCode = item.getLocationId() != null
                ? locationCodeMap.get(item.getLocationId())
                : null;

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
                item.getManufacturingDate(),
                item.getLocationId(),
                locationCode
        );
    }
}
