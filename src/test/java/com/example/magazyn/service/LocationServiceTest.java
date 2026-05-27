package com.example.magazyn.service;

import com.example.magazyn.dto.LocationRequest;
import com.example.magazyn.dto.LocationResponse;
import com.example.magazyn.dto.LocationTreeNode;
import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

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
}
