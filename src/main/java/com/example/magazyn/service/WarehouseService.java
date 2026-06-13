package com.example.magazyn.service;

import com.example.magazyn.config.TenantContext;
import com.example.magazyn.dto.WarehouseRequest;
import com.example.magazyn.dto.WarehouseResponse;
import com.example.magazyn.entity.Warehouse;
import com.example.magazyn.exception.DuplicateResourceException;
import com.example.magazyn.repository.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    public List<WarehouseResponse> getMyWarehouses() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Brak kontekstu tenanta");
        }
        return warehouseRepository.findByTenantIdOrderByName(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public WarehouseResponse getWarehouse(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Warehouse warehouse = warehouseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Magazyn nie znaleziony"));
        return toResponse(warehouse);
    }

    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Brak kontekstu tenanta");
        }

        if (warehouseRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new DuplicateResourceException(
                    "Kod '" + request.getCode() + "' jest ju\u017C u\u017Cywany");
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(request.getName());
        warehouse.setCode(request.getCode().toUpperCase().trim());
        warehouse.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        warehouse.setTenantId(tenantId);

        warehouse = warehouseRepository.save(warehouse);
        log.info("Created warehouse id={}, code={}, tenant={}", warehouse.getId(),
                warehouse.getCode(), tenantId);
        return toResponse(warehouse);
    }

    @Transactional
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        Long tenantId = TenantContext.getTenantId();
        Warehouse warehouse = warehouseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Magazyn nie znaleziony"));

        String newCode = request.getCode().toUpperCase().trim();
        // Check for duplicate code only if code changed
        if (!warehouse.getCode().equals(newCode)
                && warehouseRepository.existsByCodeAndTenantId(newCode, tenantId)) {
            throw new DuplicateResourceException(
                    "Kod '" + newCode + "' jest ju\u017C u\u017Cywany");
        }

        warehouse.setName(request.getName());
        warehouse.setCode(newCode);
        if (request.getIsActive() != null) {
            warehouse.setIsActive(request.getIsActive());
        }

        warehouse = warehouseRepository.save(warehouse);
        log.info("Updated warehouse id={}", warehouse.getId());
        return toResponse(warehouse);
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        Long tenantId = TenantContext.getTenantId();
        Warehouse warehouse = warehouseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new RuntimeException("Magazyn nie znaleziony"));
        warehouseRepository.delete(warehouse);
        log.info("Deleted warehouse id={}", id);
    }

    private WarehouseResponse toResponse(Warehouse w) {
        return new WarehouseResponse(
                w.getId(), w.getName(), w.getCode(),
                w.getIsActive(), w.getCreatedAt());
    }
}
