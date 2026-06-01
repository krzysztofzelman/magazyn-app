package com.example.magazyn.service;

import com.example.magazyn.dto.CreateReservationRequest;
import com.example.magazyn.dto.WarehouseDocumentItemRequest;
import com.example.magazyn.dto.WarehouseDocumentRequest;
import com.example.magazyn.entity.Contractor;
import com.example.magazyn.entity.ContractorType;
import com.example.magazyn.entity.DocumentType;
import com.example.magazyn.entity.Location;
import com.example.magazyn.entity.LocationType;
import com.example.magazyn.entity.Product;
import com.example.magazyn.entity.ReservationReferenceType;
import com.example.magazyn.entity.User;
import com.example.magazyn.repository.ContractorRepository;
import com.example.magazyn.repository.LocationRepository;
import com.example.magazyn.repository.ProductRepository;
import com.example.magazyn.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
@Transactional
public class SeedService {

    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ContractorRepository contractorRepository;
    private final WarehouseDocumentService warehouseDocumentService;
    private final ReservationService reservationService;
    private final PasswordEncoder passwordEncoder;

    public SeedService(LocationRepository locationRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository,
                       ContractorRepository contractorRepository,
                       WarehouseDocumentService warehouseDocumentService,
                       ReservationService reservationService,
                       PasswordEncoder passwordEncoder) {
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.contractorRepository = contractorRepository;
        this.warehouseDocumentService = warehouseDocumentService;
        this.reservationService = reservationService;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================================
    //  USERS
    // ============================================================

    public Map<String, Object> seedUsers() {
        Map<String, Object> result = new HashMap<>();

        if (userRepository.existsByUsername("admin")) {
            result.put("success", false);
            result.put("message", "U\u017Cytkownicy ju\u017C istniej\u0105 w bazie.");
            return result;
        }

        List<User> users = List.of(
                User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("REMOVED"))
                        .role("ROLE_ADMIN")
                        .email("admin@magazyn.local")
                        .isActive(true)
                        .build(),
                User.builder()
                        .username("manager")
                        .password(passwordEncoder.encode("manager123"))
                        .role("ROLE_MANAGER")
                        .email("manager@magazyn.local")
                        .isActive(true)
                        .build(),
                User.builder()
                        .username("warehouse")
                        .password(passwordEncoder.encode("warehouse123"))
                        .role("ROLE_WAREHOUSE")
                        .email("warehouse@magazyn.local")
                        .isActive(true)
                        .build(),
                User.builder()
                        .username("viewer")
                        .password(passwordEncoder.encode("viewer123"))
                        .role("ROLE_VIEWER")
                        .email("viewer@magazyn.local")
                        .isActive(true)
                        .build()
        );
        userRepository.saveAll(users);

        result.put("success", true);
        result.put("usersCreated", users.size());
        return result;
    }

    // ============================================================
    //  PRODUCTS
    // ============================================================

    public Map<String, Object> seedProducts() {
        Map<String, Object> result = new HashMap<>();

        if (productRepository.findBySku("ART-001").isPresent()) {
            result.put("success", false);
            result.put("message", "Produkty ju\u017C istniej\u0105 w bazie.");
            return result;
        }

        List<Product> products = new ArrayList<>();

        // Non-expiry products
        products.add(product("ART-001", "Rezystor 10k\u2126 0.25W", "szt", BigDecimal.valueOf(0.50), 200, 50, true));
        products.add(product("ART-002", "Kondensator 100\u00B5F 16V", "szt", BigDecimal.valueOf(2.50), 150, 30, true));
        products.add(product("ART-004", "Ta\u015Bma izolacyjna czarna 20m", "szt", BigDecimal.valueOf(8.00), 80, 10, true));
        products.add(product("ART-005", "\u015Aruba M8x30 nierdzewna", "szt", BigDecimal.valueOf(1.20), 500, 100, true));
        products.add(product("ART-008", "Kabel HDMI 2.0 2m", "szt", BigDecimal.valueOf(22.00), 40, 10, true));
        products.add(product("ART-010", "O-ring zestaw 100szt", "szt", BigDecimal.valueOf(12.00), 30, 5, true));
        products.add(product("ART-014", "Uszczelka silikonowa 3m", "szt", BigDecimal.valueOf(4.50), 60, 20, true));

        // Expiry-tracked products
        products.add(product("ART-003", "Pasta lutownicza SN63PB37", "szt", BigDecimal.valueOf(15.00), 50, 10, false, true));
        products.add(product("ART-006", "Olej maszynowy 1L", "szt", BigDecimal.valueOf(35.00), 25, 5, false, true));
        products.add(product("ART-007", "Filtr powietrza HEPA", "szt", BigDecimal.valueOf(45.00), 20, 5, false, true));
        products.add(product("ART-009", "Bateria CR2032 litowa", "szt", BigDecimal.valueOf(6.50), 100, 20, false, true));
        products.add(product("ART-011", "P\u0142yn ch\u0142odniczy 5L", "szt", BigDecimal.valueOf(55.00), 15, 3, false, true));
        products.add(product("ART-012", "Smar litowy EP 400g", "szt", BigDecimal.valueOf(18.00), 35, 5, false, true));
        products.add(product("ART-013", "Wk\u0142ad filtruj\u0105cy olej", "szt", BigDecimal.valueOf(28.00), 25, 5, false, true));
        products.add(product("ART-015", "Klej epoksydowy 24h 50ml", "szt", BigDecimal.valueOf(24.00), 40, 10, false, true));

        productRepository.saveAll(products);

        result.put("success", true);
        result.put("productsCreated", products.size());
        return result;
    }

    private Product product(String sku, String name, String unit, BigDecimal price,
                            int quantity, int minQuantity, boolean trackExpiry) {
        return product(sku, name, unit, price, quantity, minQuantity, true, trackExpiry);
    }

    private Product product(String sku, String name, String unit, BigDecimal price,
                            int quantity, int minQuantity, boolean assignBarcode, boolean trackExpiry) {
        Product.ProductBuilder builder = Product.builder()
                .sku(sku)
                .name(name)
                .unit(unit)
                .price(price)
                .quantity(quantity)
                .minQuantity(minQuantity)
                .trackExpiry(trackExpiry);
        if (assignBarcode) {
            builder.barcode("590" + sku.replaceAll("[^0-9]", "") + "000");
        }
        return builder.build();
    }

    // ============================================================
    //  CONTRACTORS
    // ============================================================

    public Map<String, Object> seedContractors() {
        Map<String, Object> result = new HashMap<>();

        if (contractorRepository.count() > 0) {
            result.put("success", false);
            result.put("message", "Kontrahenci ju\u017C istniej\u0105 w bazie.");
            return result;
        }

        List<Contractor> contractors = List.of(
                Contractor.builder()
                        .name("ElektroParts Sp. z o.o.")
                        .taxId("1234567890")
                        .address("ul. Elektronowa 15, 30-001 Krak\u00F3w")
                        .email("zamowienia@elektroparts.pl")
                        .phone("+48 12 345 67 89")
                        .type(ContractorType.SUPPLIER)
                        .build(),
                Contractor.builder()
                        .name("TechSupply S.A.")
                        .taxId("2345678901")
                        .address("Al. Technologiczna 8, 02-001 Warszawa")
                        .email("sales@techsupply.pl")
                        .phone("+48 22 456 78 90")
                        .type(ContractorType.SUPPLIER)
                        .build(),
                Contractor.builder()
                        .name("Narz\u0119dziaProfessionale")
                        .taxId("3456789012")
                        .address("ul. Przemys\u0142owa 22, 40-001 Katowice")
                        .email("info@narzedziapro.pl")
                        .phone("+48 32 567 89 01")
                        .type(ContractorType.SUPPLIER)
                        .build(),
                Contractor.builder()
                        .name("Firma Produkcyjna XYZ")
                        .taxId("4567890123")
                        .address("ul. Fabryczna 100, 90-001 \u0141\u00F3d\u017A")
                        .email("order@xyzprodukcja.pl")
                        .phone("+48 42 678 90 12")
                        .type(ContractorType.CUSTOMER)
                        .build(),
                Contractor.builder()
                        .name("Serwis Elektronika")
                        .taxId("5678901234")
                        .address("ul. Naprawcza 5, 50-001 Wroc\u0142aw")
                        .email("serwis@elektronika.pl")
                        .phone("+48 71 789 01 23")
                        .type(ContractorType.CUSTOMER)
                        .build(),
                Contractor.builder()
                        .name("Zak\u0142ad Przemys\u0142owy S.A.")
                        .taxId("6789012345")
                        .address("ul. Hutnicza 50, 44-100 Gliwice")
                        .email("po@zakladprzemyslowy.pl")
                        .phone("+48 32 890 12 34")
                        .type(ContractorType.CUSTOMER)
                        .build()
        );
        contractorRepository.saveAll(contractors);

        result.put("success", true);
        result.put("contractorsCreated", contractors.size());
        return result;
    }

    // ============================================================
    //  PZ / WZ DOCUMENTS
    // ============================================================

    public Map<String, Object> seedDocuments(String username) {
        Map<String, Object> result = new HashMap<>();

        // Fetch seeded contractors by taxId
        Contractor elektroParts = contractorRepository.findAll().stream()
                .filter(c -> "1234567890".equals(c.getTaxId()))
                .findFirst().orElse(null);
        Contractor techSupply = contractorRepository.findAll().stream()
                .filter(c -> "2345678901".equals(c.getTaxId()))
                .findFirst().orElse(null);
        Contractor narzedzia = contractorRepository.findAll().stream()
                .filter(c -> "3456789012".equals(c.getTaxId()))
                .findFirst().orElse(null);
        Contractor firmaXyz = contractorRepository.findAll().stream()
                .filter(c -> "4567890123".equals(c.getTaxId()))
                .findFirst().orElse(null);
        Contractor serwis = contractorRepository.findAll().stream()
                .filter(c -> "5678901234".equals(c.getTaxId()))
                .findFirst().orElse(null);

        if (Stream.of(elektroParts, techSupply, narzedzia, firmaXyz, serwis).anyMatch(c -> c == null)) {
            result.put("success", false);
            result.put("message", "Brak kontrahent\u00F3w w bazie. Najpierw zasiej kontrahent\u00F3w.");
            return result;
        }

        List<Product> allProducts = productRepository.findAll();
        Map<String, Product> bySku = new HashMap<>();
        for (Product p : allProducts) {
            bySku.put(p.getSku(), p);
        }

        int pzCreated = 0;
        int wzCreated = 0;

        // --- PZ/2026/001 from ElektroParts: electronic components ---
        Product rezystor = bySku.get("ART-001");
        Product kondensator = bySku.get("ART-002");
        Product pasta = bySku.get("ART-003");
        Product tasma = bySku.get("ART-004");
        Product sruby = bySku.get("ART-005");

        if (rezystor != null && kondensator != null && pasta != null && tasma != null && sruby != null) {
            WarehouseDocumentRequest req1 = new WarehouseDocumentRequest();
            req1.setType(DocumentType.PZ);
            req1.setContractorId(elektroParts.getId());
            req1.setNotes("Dostawa nr 1/2026 - cz\u0119\u015Bci elektroniczne");

            List<WarehouseDocumentItemRequest> items1 = new ArrayList<>();

            WarehouseDocumentItemRequest r1 = new WarehouseDocumentItemRequest();
            r1.setProductId(rezystor.getId()); r1.setQuantity(100); r1.setUnitPrice(BigDecimal.valueOf(0.40));
            items1.add(r1);

            WarehouseDocumentItemRequest r2 = new WarehouseDocumentItemRequest();
            r2.setProductId(kondensator.getId()); r2.setQuantity(80); r2.setUnitPrice(BigDecimal.valueOf(2.00));
            items1.add(r2);

            WarehouseDocumentItemRequest r3 = new WarehouseDocumentItemRequest();
            r3.setProductId(pasta.getId()); r3.setQuantity(30);
            r3.setUnitPrice(BigDecimal.valueOf(12.00));
            r3.setLotNumber("LOT-PASTA-2026-001");
            r3.setExpiryDate(LocalDate.of(2027, 6, 30));
            r3.setManufacturingDate(LocalDate.of(2026, 1, 15));
            items1.add(r3);

            WarehouseDocumentItemRequest r4 = new WarehouseDocumentItemRequest();
            r4.setProductId(tasma.getId()); r4.setQuantity(50); r4.setUnitPrice(BigDecimal.valueOf(6.00));
            items1.add(r4);

            WarehouseDocumentItemRequest r5 = new WarehouseDocumentItemRequest();
            r5.setProductId(sruby.getId()); r5.setQuantity(300); r5.setUnitPrice(BigDecimal.valueOf(0.90));
            items1.add(r5);

            req1.setItems(items1);

            try {
                var doc1 = warehouseDocumentService.createDocument(req1, username);
                warehouseDocumentService.confirmDocument(doc1.getId(), username);
                pzCreated++;
            } catch (Exception e) {
                result.put("pz1Error", e.getMessage());
            }
        }

        // --- PZ/2026/002 from TechSupply: oils, filters, batteries ---
        Product olej = bySku.get("ART-006");
        Product filtr = bySku.get("ART-007");
        Product hdmi = bySku.get("ART-008");
        Product bateria = bySku.get("ART-009");
        Product oring = bySku.get("ART-010");

        if (olej != null && filtr != null && hdmi != null && bateria != null && oring != null) {
            WarehouseDocumentRequest req2 = new WarehouseDocumentRequest();
            req2.setType(DocumentType.PZ);
            req2.setContractorId(techSupply.getId());
            req2.setNotes("Dostawa nr 2/2026 - materia\u0142y eksploatacyjne");

            List<WarehouseDocumentItemRequest> items2 = new ArrayList<>();

            WarehouseDocumentItemRequest r1 = new WarehouseDocumentItemRequest();
            r1.setProductId(olej.getId()); r1.setQuantity(15);
            r1.setUnitPrice(BigDecimal.valueOf(28.00));
            r1.setLotNumber("LOT-OIL-2026-A");
            r1.setExpiryDate(LocalDate.of(2028, 12, 31));
            r1.setManufacturingDate(LocalDate.of(2026, 2, 1));
            items2.add(r1);

            WarehouseDocumentItemRequest r2 = new WarehouseDocumentItemRequest();
            r2.setProductId(filtr.getId()); r2.setQuantity(12);
            r2.setUnitPrice(BigDecimal.valueOf(38.00));
            r2.setLotNumber("LOT-FLT-2026-001");
            r2.setExpiryDate(LocalDate.of(2027, 8, 15));
            r2.setManufacturingDate(LocalDate.of(2026, 1, 20));
            items2.add(r2);

            WarehouseDocumentItemRequest r3 = new WarehouseDocumentItemRequest();
            r3.setProductId(hdmi.getId()); r3.setQuantity(20); r3.setUnitPrice(BigDecimal.valueOf(18.00));
            items2.add(r3);

            WarehouseDocumentItemRequest r4 = new WarehouseDocumentItemRequest();
            r4.setProductId(bateria.getId()); r4.setQuantity(60);
            r4.setUnitPrice(BigDecimal.valueOf(5.00));
            r4.setLotNumber("LOT-BAT-2026-001");
            r4.setExpiryDate(LocalDate.of(2030, 1, 1));
            r4.setManufacturingDate(LocalDate.of(2025, 6, 1));
            items2.add(r4);

            WarehouseDocumentItemRequest r5 = new WarehouseDocumentItemRequest();
            r5.setProductId(oring.getId()); r5.setQuantity(20); r5.setUnitPrice(BigDecimal.valueOf(10.00));
            items2.add(r5);

            req2.setItems(items2);

            try {
                var doc2 = warehouseDocumentService.createDocument(req2, username);
                warehouseDocumentService.confirmDocument(doc2.getId(), username);
                pzCreated++;
            } catch (Exception e) {
                result.put("pz2Error", e.getMessage());
            }
        }

        // --- PZ/2026/003 from NarzedziaProfessionale: chemicals, seals ---
        Product plyn = bySku.get("ART-011");
        Product smar = bySku.get("ART-012");
        Product wklad = bySku.get("ART-013");
        Product uszczelka = bySku.get("ART-014");
        Product klej = bySku.get("ART-015");

        if (plyn != null && smar != null && wklad != null && uszczelka != null && klej != null) {
            WarehouseDocumentRequest req3 = new WarehouseDocumentRequest();
            req3.setType(DocumentType.PZ);
            req3.setContractorId(narzedzia.getId());
            req3.setNotes("Dostawa nr 3/2026 - chemia i uszczelnienia");

            List<WarehouseDocumentItemRequest> items3 = new ArrayList<>();

            WarehouseDocumentItemRequest r1 = new WarehouseDocumentItemRequest();
            r1.setProductId(plyn.getId()); r1.setQuantity(10);
            r1.setUnitPrice(BigDecimal.valueOf(45.00));
            r1.setLotNumber("LOT-COOL-2026-001");
            r1.setExpiryDate(LocalDate.of(2028, 3, 1));
            r1.setManufacturingDate(LocalDate.of(2026, 2, 15));
            items3.add(r1);

            WarehouseDocumentItemRequest r2 = new WarehouseDocumentItemRequest();
            r2.setProductId(smar.getId()); r2.setQuantity(20);
            r2.setUnitPrice(BigDecimal.valueOf(14.00));
            r2.setLotNumber("LOT-GREASE-2026-A");
            r2.setExpiryDate(LocalDate.of(2029, 6, 30));
            r2.setManufacturingDate(LocalDate.of(2026, 1, 10));
            items3.add(r2);

            WarehouseDocumentItemRequest r3 = new WarehouseDocumentItemRequest();
            r3.setProductId(wklad.getId()); r3.setQuantity(15);
            r3.setUnitPrice(BigDecimal.valueOf(22.00));
            r3.setLotNumber("LOT-FILT-2026-002");
            r3.setExpiryDate(LocalDate.of(2027, 12, 31));
            r3.setManufacturingDate(LocalDate.of(2026, 3, 1));
            items3.add(r3);

            WarehouseDocumentItemRequest r4 = new WarehouseDocumentItemRequest();
            r4.setProductId(uszczelka.getId()); r4.setQuantity(40); r4.setUnitPrice(BigDecimal.valueOf(3.50));
            items3.add(r4);

            WarehouseDocumentItemRequest r5 = new WarehouseDocumentItemRequest();
            r5.setProductId(klej.getId()); r5.setQuantity(25);
            r5.setUnitPrice(BigDecimal.valueOf(19.00));
            r5.setLotNumber("LOT-EPOXY-2026-001");
            r5.setExpiryDate(LocalDate.of(2027, 9, 30));
            r5.setManufacturingDate(LocalDate.of(2026, 2, 1));
            items3.add(r5);

            req3.setItems(items3);

            try {
                var doc3 = warehouseDocumentService.createDocument(req3, username);
                warehouseDocumentService.confirmDocument(doc3.getId(), username);
                pzCreated++;
            } catch (Exception e) {
                result.put("pz3Error", e.getMessage());
            }
        }

        // --- WZ/2026/001 to Firma Produkcyjna XYZ: resistors and capacitors ---
        if (rezystor != null && kondensator != null && firmaXyz != null) {
            WarehouseDocumentRequest req4 = new WarehouseDocumentRequest();
            req4.setType(DocumentType.WZ);
            req4.setContractorId(firmaXyz.getId());
            req4.setNotes("Wydanie nr 1/2026 - zam\u00F3wienie ZAM-001");

            List<WarehouseDocumentItemRequest> items4 = new ArrayList<>();

            WarehouseDocumentItemRequest r1 = new WarehouseDocumentItemRequest();
            r1.setProductId(rezystor.getId()); r1.setQuantity(30);
            items4.add(r1);

            WarehouseDocumentItemRequest r2 = new WarehouseDocumentItemRequest();
            r2.setProductId(kondensator.getId()); r2.setQuantity(20);
            items4.add(r2);

            req4.setItems(items4);

            try {
                var doc4 = warehouseDocumentService.createDocument(req4, username);
                warehouseDocumentService.confirmDocument(doc4.getId(), username);
                wzCreated++;
            } catch (Exception e) {
                result.put("wz1Error", e.getMessage());
            }
        }

        // --- WZ/2026/002 to Serwis Elektronika: screws ---
        if (sruby != null && serwis != null) {
            WarehouseDocumentRequest req5 = new WarehouseDocumentRequest();
            req5.setType(DocumentType.WZ);
            req5.setContractorId(serwis.getId());
            req5.setNotes("Wydanie nr 2/2026 - zam\u00F3wienie ZAM-002");

            List<WarehouseDocumentItemRequest> items5 = new ArrayList<>();

            WarehouseDocumentItemRequest r1 = new WarehouseDocumentItemRequest();
            r1.setProductId(sruby.getId()); r1.setQuantity(100);
            items5.add(r1);

            req5.setItems(items5);

            try {
                var doc5 = warehouseDocumentService.createDocument(req5, username);
                warehouseDocumentService.confirmDocument(doc5.getId(), username);
                wzCreated++;
            } catch (Exception e) {
                result.put("wz2Error", e.getMessage());
            }
        }

        result.put("success", true);
        result.put("pzDocumentsCreated", pzCreated);
        result.put("wzDocumentsCreated", wzCreated);
        return result;
    }

    // ============================================================
    //  RESERVATIONS
    // ============================================================

    public Map<String, Object> seedReservations(String username) {
        Map<String, Object> result = new HashMap<>();

        Product rezystor = productRepository.findBySku("ART-001").orElse(null);
        Product olej = productRepository.findBySku("ART-006").orElse(null);

        if (rezystor == null || olej == null) {
            result.put("success", false);
            result.put("message", "Brak produkt\u00F3w w bazie. Najpierw zasiej produkty.");
            return result;
        }

        int created = 0;

        try {
            CreateReservationRequest req1 = new CreateReservationRequest();
            req1.setProductId(rezystor.getId());
            req1.setQuantity(10);
            req1.setReferenceType(ReservationReferenceType.ORDER);
            req1.setReferenceId("ZAM-001");
            req1.setNotes("Rezerwacja na potrzeby zam\u00F3wienia ZAM-001");
            req1.setExpiresAt(LocalDateTime.now().plusDays(30));
            reservationService.reserve(req1, username);
            created++;
        } catch (Exception e) {
            result.put("reservation1Error", e.getMessage());
        }

        try {
            CreateReservationRequest req2 = new CreateReservationRequest();
            req2.setProductId(olej.getId());
            req2.setQuantity(3);
            req2.setReferenceType(ReservationReferenceType.MANUAL);
            req2.setNotes("Rezerwacja r\u0119czna na potrzeby produkcji");
            req2.setExpiresAt(LocalDateTime.now().plusDays(14));
            reservationService.reserve(req2, username);
            created++;
        } catch (Exception e) {
            result.put("reservation2Error", e.getMessage());
        }

        result.put("success", true);
        result.put("reservationsCreated", created);
        return result;
    }

    // ============================================================
    //  ALL-IN-ONE SEED
    // ============================================================

    public Map<String, Object> seedAll() {
        Map<String, Object> result = new HashMap<>();
        String systemUser = "admin";

        result.putAll(seedUsers());
        result.putAll(seedLocations());
        result.putAll(seedProducts());
        result.putAll(seedContractors());
        result.putAll(seedDocuments(systemUser));
        result.putAll(seedReservations(systemUser));

        return result;
    }

    private void assignProductLocations() {
        List<Product> allProducts = productRepository.findAll();
        if (allProducts.isEmpty()) return;

        List<Location> bins = locationRepository.findAll().stream()
                .filter(l -> l.getType() == LocationType.BIN)
                .toList();
        if (bins.isEmpty()) return;

        int perBin = (int) Math.ceil((double) allProducts.size() / bins.size());
        int idx = 0;

        for (Location bin : bins) {
            for (int i = 0; i < perBin && idx < allProducts.size(); i++) {
                allProducts.get(idx).setLocationId(bin.getId());
                idx++;
            }
        }

        productRepository.saveAll(allProducts);
    }

    // ============================================================
    //  LOCATIONS (existing)
    // ============================================================

    public Map<String, Object> seedLocations() {
        Map<String, Object> result = new HashMap<>();

        if (locationRepository.count() > 0) {
            result.put("success", false);
            result.put("message",
                    "Baza lokalizacji nie jest pusta. Seed mo\u017Cna uruchomi\u0107 tylko na pustej bazie.");
            return result;
        }

        List<SeedNode> hierarchy = buildHierarchy();

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

        List<Location> saved = locationRepository.findAll();
        Map<String, Location> byCode = new HashMap<>();
        for (Location loc : saved) {
            byCode.put(loc.getCode(), loc);
        }

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

        list.add(new SeedNode("MAG-B", "Magazyn Podr\u0119czny", LocationType.WAREHOUSE, null));
        list.add(new SeedNode("REG-B1", "Rega\u0142 B1 - Cz\u0119\u015Bci zamienne", LocationType.RACK, "MAG-B"));
        list.add(new SeedNode("POL-B1-1", "P\u00F3\u0142ka 1", LocationType.SHELF, "REG-B1"));
        list.add(new SeedNode("MJC-B1-1-01", "Miejsce 01", LocationType.BIN, "POL-B1-1"));
        list.add(new SeedNode("MJC-B1-1-02", "Miejsce 02", LocationType.BIN, "POL-B1-1"));

        return list;
    }
}
