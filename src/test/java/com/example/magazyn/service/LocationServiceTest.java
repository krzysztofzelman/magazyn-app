package com.example.magazyn.service;

import com.example.magazyn.dto.LocationRequest;
import com.example.magazyn.dto.LocationResponse;
import com.example.magazyn.dto.LocationTreeNode;
import com.example.magazyn.dto.TransferRequest;
import com.example.magazyn.dto.TransferResponse;
import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationStock;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.repository.LocationStockRepository;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.service.BarcodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LocationStockRepository locationStockRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private BarcodeService barcodeService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LocationService locationService;

    private Location createLocation(Long id, String code, String name, LocationType type, Long parentId) {
        return Location.builder()
                .id(id)
                .code(code)
                .name(name)
                .type(type)
                .parentId(parentId)
                .description("Description of " + name)
                .build();
    }

    private LocationRequest createRequest(String code, String name, String type, Long parentId) {
        LocationRequest request = new LocationRequest();
        request.setCode(code);
        request.setName(name);
        request.setType(type);
        request.setParentId(parentId);
        request.setDescription("Description");
        return request;
    }

    // ──────────────────────────────────────────────
    // createLocation
    // ──────────────────────────────────────────────

    @Test
    void createLocation_withoutParent_success() {
        LocationRequest request = createRequest("WH-01", "Main Warehouse", "WAREHOUSE", null);
        Location saved = createLocation(1L, "WH-01", "Main Warehouse", LocationType.WAREHOUSE, null);

        when(locationRepository.save(any(Location.class))).thenReturn(saved);

        LocationResponse response = locationService.createLocation(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("WH-01", response.getCode());
        assertEquals("Main Warehouse", response.getName());
        assertEquals("WAREHOUSE", response.getType());
        assertNull(response.getParentId());

        verify(locationRepository).save(any(Location.class));
        verify(locationRepository, never()).existsById(any());
    }

    @Test
    void createLocation_withValidParent_success() {
        LocationRequest request = createRequest("RACK-01", "Rack 1", "RACK", 1L);

        when(locationRepository.existsById(1L)).thenReturn(true);

        Location saved = createLocation(2L, "RACK-01", "Rack 1", LocationType.RACK, 1L);
        when(locationRepository.save(any(Location.class))).thenReturn(saved);

        LocationResponse response = locationService.createLocation(request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals(Long.valueOf(1L), response.getParentId());
        verify(locationRepository).existsById(1L);
    }

    @Test
    void createLocation_withInvalidParent_throws() {
        LocationRequest request = createRequest("RACK-01", "Rack 1", "RACK", 999L);

        when(locationRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> locationService.createLocation(request));
        assertTrue(exception.getMessage().contains("Parent location not found"));

        verify(locationRepository).existsById(999L);
        verify(locationRepository, never()).save(any());
    }

    @Test
    void createLocation_parentIdNull_noValidation() {
        LocationRequest request = createRequest("BIN-01", "Bin 1", "BIN", null);
        Location saved = createLocation(1L, "BIN-01", "Bin 1", LocationType.BIN, null);

        when(locationRepository.save(any(Location.class))).thenReturn(saved);

        locationService.createLocation(request);

        verify(locationRepository, never()).existsById(any());
        verify(locationRepository).save(any(Location.class));
    }

    // ──────────────────────────────────────────────
    // getLocationTree
    // ──────────────────────────────────────────────

    @Test
    void getLocationTree_withHierarchy_buildsCorrectStructure() {
        Location warehouse = createLocation(1L, "WH-01", "Warehouse", LocationType.WAREHOUSE, null);
        Location rack = createLocation(2L, "RACK-01", "Rack 1", LocationType.RACK, 1L);
        Location shelf = createLocation(3L, "SH-01", "Shelf 1", LocationType.SHELF, 2L);

        when(locationRepository.findAll()).thenReturn(List.of(warehouse, rack, shelf));

        List<LocationTreeNode> tree = locationService.getLocationTree();

        assertEquals(1, tree.size());
        LocationTreeNode root = tree.get(0);
        assertEquals("WH-01", root.getCode());
        assertEquals("Warehouse", root.getName());
        assertEquals(1, root.getChildren().size());

        LocationTreeNode rackNode = root.getChildren().get(0);
        assertEquals("RACK-01", rackNode.getCode());
        assertEquals(1, rackNode.getChildren().size());

        LocationTreeNode shelfNode = rackNode.getChildren().get(0);
        assertEquals("SH-01", shelfNode.getCode());
        assertEquals(0, shelfNode.getChildren().size());
    }

    @Test
    void getLocationTree_withNoRoots_returnsEmpty() {
        Location child = createLocation(2L, "RACK-01", "Rack", LocationType.RACK, 1L);

        when(locationRepository.findAll()).thenReturn(List.of(child));

        List<LocationTreeNode> tree = locationService.getLocationTree();

        assertTrue(tree.isEmpty());
    }

    @Test
    void getLocationTree_withOrphanChildren_ignoresThem() {
        Location orphan = createLocation(2L, "ORPHAN", "Orphan", LocationType.RACK, 999L);

        when(locationRepository.findAll()).thenReturn(List.of(orphan));

        List<LocationTreeNode> tree = locationService.getLocationTree();

        assertTrue(tree.isEmpty());
    }

    // ──────────────────────────────────────────────
    // deleteLocation
    // ──────────────────────────────────────────────

    @Test
    void deleteLocation_withoutChildren_success() {
        Location location = createLocation(1L, "WH-01", "Warehouse", LocationType.WAREHOUSE, null);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(locationRepository.existsByParentId(1L)).thenReturn(false);

        locationService.deleteLocation(1L);

        verify(locationRepository).deleteById(1L);
    }

    @Test
    void deleteLocation_withChildren_throws() {
        Location location = createLocation(1L, "WH-01", "Warehouse", LocationType.WAREHOUSE, null);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));
        when(locationRepository.existsByParentId(1L)).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> locationService.deleteLocation(1L));
        assertTrue(exception.getMessage().contains("children"));

        verify(locationRepository, never()).deleteById(any());
    }

    @Test
    void deleteLocation_notFound_throws() {
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> locationService.deleteLocation(999L));
        assertTrue(exception.getMessage().contains("not found"));

        verify(locationRepository, never()).deleteById(any());
    }

    // ──────────────────────────────────────────────
    // getAllLocations / getLocationById / getChildren
    // ──────────────────────────────────────────────

    @Test
    void getAllLocations_returnsSortedList() {
        Location loc2 = createLocation(2L, "B", "Location B", LocationType.WAREHOUSE, null);
        Location loc1 = createLocation(1L, "A", "Location A", LocationType.WAREHOUSE, null);

        when(locationRepository.findAll()).thenReturn(List.of(loc2, loc1));

        List<LocationResponse> result = locationService.getAllLocations();

        assertEquals(2, result.size());
        assertEquals("Location A", result.get(0).getName()); // sorted by id
        assertEquals("Location B", result.get(1).getName());
    }

    @Test
    void getLocationById_found() {
        Location location = createLocation(1L, "WH-01", "Warehouse", LocationType.WAREHOUSE, null);
        when(locationRepository.findById(1L)).thenReturn(Optional.of(location));

        Optional<LocationResponse> result = locationService.getLocationById(1L);

        assertTrue(result.isPresent());
        assertEquals("WH-01", result.get().getCode());
    }

    @Test
    void getLocationById_notFound() {
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<LocationResponse> result = locationService.getLocationById(999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getChildren_returnsChildrenSorted() {
        Location child1 = createLocation(2L, "CH-01", "Child 1", LocationType.RACK, 1L);
        Location child2 = createLocation(3L, "CH-02", "Child 2", LocationType.SHELF, 1L);

        when(locationRepository.findByParentId(1L)).thenReturn(List.of(child2, child1));

        List<LocationResponse> children = locationService.getChildren(1L);

        assertEquals(2, children.size());
        assertEquals("Child 1", children.get(0).getName()); // sorted by id
        assertEquals("Child 2", children.get(1).getName());
    }

    // ──────────────────────────────────────────────
    // updateLocation
    // ──────────────────────────────────────────────

    @Test
    void updateLocation_success() {
        Location existing = createLocation(1L, "OLD-CODE", "Old Name", LocationType.WAREHOUSE, null);
        LocationRequest request = createRequest("NEW-CODE", "New Name", "RACK", null);

        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(locationRepository.save(any(Location.class))).thenAnswer(i -> i.getArgument(0));

        LocationResponse response = locationService.updateLocation(1L, request);

        assertEquals("NEW-CODE", response.getCode());
        assertEquals("New Name", response.getName());
        assertEquals("RACK", response.getType());
        assertNull(response.getParentId());
    }

    @Test
    void updateLocation_withNewParent_validates() {
        Location existing = createLocation(2L, "RACK-01", "Rack", LocationType.RACK, 1L);
        LocationRequest request = createRequest("RACK-01", "Rack", "RACK", 999L);

        when(locationRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(locationRepository.existsById(999L)).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> locationService.updateLocation(2L, request));
        assertTrue(exception.getMessage().contains("Parent location not found"));
        verify(locationRepository, never()).save(any());
    }

    @Test
    void updateLocation_notFound_throws() {
        LocationRequest request = createRequest("CODE", "Name", "WAREHOUSE", null);
        when(locationRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> locationService.updateLocation(999L, request));
        assertTrue(exception.getMessage().contains("not found"));
    }

    @Test
    void updateLocation_partialUpdate() {
        Location existing = createLocation(1L, "CODE", "Name", LocationType.WAREHOUSE, null);
        LocationRequest request = new LocationRequest();
        request.setCode("NEW-CODE");

        when(locationRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(locationRepository.save(any(Location.class))).thenAnswer(i -> i.getArgument(0));

        LocationResponse response = locationService.updateLocation(1L, request);

        assertEquals("NEW-CODE", response.getCode());
        assertEquals("Name", response.getName()); // unchanged
        assertEquals("WAREHOUSE", response.getType()); // unchanged
    }

    // ──────────────────────────────────────────────
    // transferStock
    // ──────────────────────────────────────────────

    @Test
    void transferStock_success() {
        Location fromLoc = createLocation(1L, "MG-01-R01", "Regal 1", LocationType.SHELF, 10L);
        fromLoc.setBarcode("LOC-MG01-R01");
        Location toLoc = createLocation(2L, "MG-01-R02", "Regal 2", LocationType.SHELF, 10L);
        toLoc.setBarcode("LOC-MG01-R02");

        Product product = Product.builder().id(5L).name("Test Product").sku("SKU-TEST").unit("szt.").build();

        LocationStock fromStock = LocationStock.builder()
                .id(1L).locationId(1L).productId(5L)
                .quantity(BigDecimal.valueOf(50))
                .reservedQuantity(BigDecimal.valueOf(10))
                .updatedAt(LocalDateTime.now()).build();

        TransferRequest request = new TransferRequest();
        request.setFromBarcode("LOC-MG01-R01");
        request.setToBarcode("LOC-MG01-R02");
        request.setProductId(5L);
        request.setQuantity(20.0);

        when(locationRepository.findByBarcode("LOC-MG01-R01")).thenReturn(Optional.of(fromLoc));
        when(locationRepository.findByBarcode("LOC-MG01-R02")).thenReturn(Optional.of(toLoc));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(locationStockRepository.findByLocationIdAndProductId(1L, 5L)).thenReturn(Optional.of(fromStock));
        when(locationStockRepository.findByLocationIdAndProductId(2L, 5L)).thenReturn(Optional.empty());
        when(locationStockRepository.findByLocationId(1L)).thenReturn(List.of());
        when(locationStockRepository.findByLocationId(2L)).thenReturn(List.of());
        when(locationRepository.findById(1L)).thenReturn(Optional.of(fromLoc));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(toLoc));

        TransferResponse response = locationService.transferStock(request, "testuser");

        assertNotNull(response);
        assertEquals("MG-01-R01", response.getFromLocationCode());
        assertEquals("MG-01-R02", response.getToLocationCode());
        assertEquals("Test Product", response.getProductName());
        assertEquals(Double.valueOf(20.0), response.getQuantityMoved());
        assertEquals(BigDecimal.valueOf(30), fromStock.getQuantity()); // 50 - 20
        verify(locationStockRepository).save(fromStock);
        verify(locationStockRepository).save(any(LocationStock.class));
        verify(auditLogService).log(eq("testuser"), eq("STOCK_TRANSFER"), eq("LocationStock"), isNull(), anyString());
    }

    @Test
    void transferStock_insufficientStock_throwsException() {
        Location fromLoc = createLocation(1L, "MG-01-R01", "Regal 1", LocationType.SHELF, 10L);
        fromLoc.setBarcode("LOC-MG01-R01");
        Location toLoc = createLocation(2L, "MG-01-R02", "Regal 2", LocationType.SHELF, 10L);
        toLoc.setBarcode("LOC-MG01-R02");

        Product product = Product.builder().id(5L).name("Test Product").sku("SKU-TEST").build();

        LocationStock fromStock = LocationStock.builder()
                .id(1L).locationId(1L).productId(5L)
                .quantity(BigDecimal.valueOf(10))
                .reservedQuantity(BigDecimal.valueOf(5))
                .updatedAt(LocalDateTime.now()).build();

        TransferRequest request = new TransferRequest();
        request.setFromBarcode("LOC-MG01-R01");
        request.setToBarcode("LOC-MG01-R02");
        request.setProductId(5L);
        request.setQuantity(20.0);

        when(locationRepository.findByBarcode("LOC-MG01-R01")).thenReturn(Optional.of(fromLoc));
        when(locationRepository.findByBarcode("LOC-MG01-R02")).thenReturn(Optional.of(toLoc));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(locationStockRepository.findByLocationIdAndProductId(1L, 5L)).thenReturn(Optional.of(fromStock));

        assertThrows(RuntimeException.class,
                () -> locationService.transferStock(request, "testuser"));
    }

    @Test
    void transferStock_productNotInSource_throwsException() {
        Location fromLoc = createLocation(1L, "MG-01-R01", "Regal 1", LocationType.SHELF, 10L);
        fromLoc.setBarcode("LOC-MG01-R01");
        Location toLoc = createLocation(2L, "MG-01-R02", "Regal 2", LocationType.SHELF, 10L);
        toLoc.setBarcode("LOC-MG01-R02");

        Product product = Product.builder().id(5L).name("Test Product").build();

        TransferRequest request = new TransferRequest();
        request.setFromBarcode("LOC-MG01-R01");
        request.setToBarcode("LOC-MG01-R02");
        request.setProductId(5L);
        request.setQuantity(5.0);

        when(locationRepository.findByBarcode("LOC-MG01-R01")).thenReturn(Optional.of(fromLoc));
        when(locationRepository.findByBarcode("LOC-MG01-R02")).thenReturn(Optional.of(toLoc));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(locationStockRepository.findByLocationIdAndProductId(1L, 5L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> locationService.transferStock(request, "testuser"));
    }

    @Test
    void transferStock_deleteFromStockWhenZero() {
        Location fromLoc = createLocation(1L, "MG-01-R01", "Regal 1", LocationType.SHELF, 10L);
        fromLoc.setBarcode("LOC-MG01-R01");
        Location toLoc = createLocation(2L, "MG-01-R02", "Regal 2", LocationType.SHELF, 10L);
        toLoc.setBarcode("LOC-MG01-R02");

        Product product = Product.builder().id(5L).name("Test Product").sku("SKU-TEST").build();

        LocationStock fromStock = LocationStock.builder()
                .id(1L).locationId(1L).productId(5L)
                .quantity(BigDecimal.valueOf(20))
                .reservedQuantity(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now()).build();

        TransferRequest request = new TransferRequest();
        request.setFromBarcode("LOC-MG01-R01");
        request.setToBarcode("LOC-MG01-R02");
        request.setProductId(5L);
        request.setQuantity(20.0);

        when(locationRepository.findByBarcode("LOC-MG01-R01")).thenReturn(Optional.of(fromLoc));
        when(locationRepository.findByBarcode("LOC-MG01-R02")).thenReturn(Optional.of(toLoc));
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(locationStockRepository.findByLocationIdAndProductId(1L, 5L)).thenReturn(Optional.of(fromStock));
        when(locationStockRepository.findByLocationIdAndProductId(2L, 5L)).thenReturn(Optional.empty());
        when(locationStockRepository.findByLocationId(1L)).thenReturn(List.of());
        when(locationStockRepository.findByLocationId(2L)).thenReturn(List.of());
        when(locationRepository.findById(1L)).thenReturn(Optional.of(fromLoc));
        when(locationRepository.findById(2L)).thenReturn(Optional.of(toLoc));

        TransferResponse response = locationService.transferStock(request, "testuser");

        assertEquals(Double.valueOf(20.0), response.getQuantityMoved());
        verify(locationStockRepository).delete(fromStock);
        verify(locationStockRepository, never()).save(fromStock);
    }
}
