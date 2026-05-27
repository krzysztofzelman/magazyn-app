package com.example.magazyn.service;

import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SeedService {

    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;

    public SeedService(LocationRepository locationRepository, ProductRepository productRepository) {
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
    }

    public Map<String, Object> seedLocations() {
        Map<String, Object> result = new HashMap<>();

        if (locationRepository.count() > 0) {
            result.put("success", false);
            result.put("message",
                    "Baza lokalizacji nie jest pusta. Seed mo\u017Cna uruchomi\u0107 tylko na pustej bazie.");
            return result;
        }

        // 1. Build all locations without parentId (using code references instead)
        List<SeedNode> hierarchy = buildHierarchy();

        // 2. Save all at once so Hibernate generates IDs
        List<Location> entities = hierarchy.stream()
                .map(n -> Location.builder()
                        .code(n.code)
                        .name(n.name)
                        .type(n.type)
                        .parentId(null)
                        .description(null)
                        .build())
                .toList();
        locationRepository.saveAll(entities);

        // 3. Re-read to get generated IDs and create code→entity map
        List<Location> saved = locationRepository.findAll();
        Map<String, Location> byCode = new HashMap<>();
        for (Location loc : saved) {
            byCode.put(loc.getCode(), loc);
        }

        // 4. Set parentId based on parentCode references
        for (SeedNode n : hierarchy) {
            if (n.parentCode != null) {
                Location child = byCode.get(n.code);
                Location parent = byCode.get(n.parentCode);
                if (child != null && parent != null) {
                    child.setParentId(parent.getId());
                }
            }
        }
        locationRepository.saveAll(saved);

        // 5. Assign products evenly to BINs
        List<Product> allProducts = productRepository.findAll();
        int productsAssigned = 0;

        if (!allProducts.isEmpty()) {
            List<Location> bins = saved.stream()
                    .filter(l -> l.getType() == LocationType.BIN)
                    .toList();

            if (!bins.isEmpty()) {
                int perBin = (int) Math.ceil((double) allProducts.size() / bins.size());
                int idx = 0;

                for (Location bin : bins) {
                    for (int i = 0; i < perBin && idx < allProducts.size(); i++) {
                        allProducts.get(idx).setLocationId(bin.getId());
                        idx++;
                    }
                }

                productRepository.saveAll(allProducts);
                productsAssigned = allProducts.size();
            }
        }

        result.put("success", true);
        result.put("locationsCreated", saved.size());
        result.put("productsAssigned", productsAssigned);
        return result;
    }

    private static class SeedNode {
        final String code;
        final String name;
        final LocationType type;
        final String parentCode;

        SeedNode(String code, String name, LocationType type, String parentCode) {
            this.code = code;
            this.name = name;
            this.type = type;
            this.parentCode = parentCode;
        }
    }

    private List<SeedNode> buildHierarchy() {
        List<SeedNode> list = new ArrayList<>();

        // MAG-A hierarchy
        list.add(new SeedNode("MAG-A", "Magazyn G\u0142\u00F3wny", LocationType.WAREHOUSE, null));
        list.add(new SeedNode("REG-A1", "Rega\u0142 A1 - Elektronika", LocationType.RACK, "MAG-A"));
        list.add(new SeedNode("POL-A1-1", "P\u00F3\u0142ka 1", LocationType.SHELF, "REG-A1"));
        list.add(new SeedNode("MJC-A1-1-01", "Miejsce 01", LocationType.BIN, "POL-A1-1"));
        list.add(new SeedNode("MJC-A1-1-02", "Miejsce 02", LocationType.BIN, "POL-A1-1"));
        list.add(new SeedNode("POL-A1-2", "P\u00F3\u0142ka 2", LocationType.SHELF, "REG-A1"));
        list.add(new SeedNode("MJC-A1-2-01", "Miejsce 01", LocationType.BIN, "POL-A1-2"));
        list.add(new SeedNode("REG-A2", "Rega\u0142 A2 - Narz\u0119dzia", LocationType.RACK, "MAG-A"));
        list.add(new SeedNode("POL-A2-1", "P\u00F3\u0142ka 1", LocationType.SHELF, "REG-A2"));
        list.add(new SeedNode("MJC-A2-1-01", "Miejsce 01", LocationType.BIN, "POL-A2-1"));
        list.add(new SeedNode("MJC-A2-1-02", "Miejsce 02", LocationType.BIN, "POL-A2-1"));

        // MAG-B hierarchy
        list.add(new SeedNode("MAG-B", "Magazyn Podr\u0119czny", LocationType.WAREHOUSE, null));
        list.add(new SeedNode("REG-B1", "Rega\u0142 B1 - Cz\u0119\u015Bci zamienne", LocationType.RACK, "MAG-B"));
        list.add(new SeedNode("POL-B1-1", "P\u00F3\u0142ka 1", LocationType.SHELF, "REG-B1"));
        list.add(new SeedNode("MJC-B1-1-01", "Miejsce 01", LocationType.BIN, "POL-B1-1"));
        list.add(new SeedNode("MJC-B1-1-02", "Miejsce 02", LocationType.BIN, "POL-B1-1"));

        return list;
    }
}
