package com.example.magazyn.service;

import com.example.magazyn.dto.LocationResponse;
import com.example.magazyn.dto.LocationTreeNode;
import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.repository.LocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;

    @Autowired
    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .sorted(Comparator.comparing(Location::getId))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<LocationResponse> getLocationById(Long id) {
        return locationRepository.findById(id)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<LocationTreeNode> getLocationTree() {
        List<Location> all = locationRepository.findAll();
        Map<Long, List<Location>> childrenByParent = all.stream()
                .filter(l -> l.getParentId() != null)
                .collect(Collectors.groupingBy(Location::getParentId));

        return all.stream()
                .filter(l -> l.getParentId() == null)
                .sorted(Comparator.comparing(Location::getId))
                .map(l -> buildTreeNode(l, childrenByParent))
                .toList();
    }

    private LocationTreeNode buildTreeNode(Location location, Map<Long, List<Location>> childrenByParent) {
        LocationTreeNode node = new LocationTreeNode();
        node.setId(location.getId());
        node.setCode(location.getCode());
        node.setName(location.getName());
        node.setType(location.getType().name());
        node.setDescription(location.getDescription());

        List<Location> children = childrenByParent.getOrDefault(location.getId(), List.of());
        children.sort(Comparator.comparing(Location::getId));
        for (Location child : children) {
            node.getChildren().add(buildTreeNode(child, childrenByParent));
        }
        return node;
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getChildren(Long parentId) {
        return locationRepository.findByParentId(parentId).stream()
                .sorted(Comparator.comparing(Location::getId))
                .map(this::toResponse)
                .toList();
    }

    public LocationResponse createLocation(com.example.magazyn.dto.LocationRequest request) {
        validateParent(request.getParentId());

        Location location = Location.builder()
                .code(request.getCode())
                .name(request.getName())
                .type(LocationType.valueOf(request.getType()))
                .parentId(request.getParentId())
                .description(request.getDescription())
                .build();

        Location saved = locationRepository.save(location);
        return toResponse(saved);
    }

    public LocationResponse updateLocation(Long id, com.example.magazyn.dto.LocationRequest request) {
        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));

        validateParent(request.getParentId());

        if (request.getCode() != null) {
            existing.setCode(request.getCode());
        }
        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getType() != null) {
            existing.setType(LocationType.valueOf(request.getType()));
        }
        existing.setParentId(request.getParentId());
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }

        Location saved = locationRepository.save(existing);
        return toResponse(saved);
    }

    public void deleteLocation(Long id) {
        Location location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Location not found with id: " + id));

        if (locationRepository.existsByParentId(id)) {
            throw new RuntimeException("Cannot delete location with children. Remove children first.");
        }

        locationRepository.deleteById(id);
    }

    private void validateParent(Long parentId) {
        if (parentId != null && !locationRepository.existsById(parentId)) {
            throw new RuntimeException("Parent location not found with id: " + parentId);
        }
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getCode(),
                location.getName(),
                location.getType().name(),
                location.getParentId(),
                location.getDescription()
        );
    }
}
