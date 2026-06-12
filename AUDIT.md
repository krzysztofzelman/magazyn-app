# Audyt projektu magazyn-app — aktualizacja 2025-07-14

**Data audytu:** 2025-07-14  
**Zakres:** Backend (Spring Boot 4.0.6, Hibernate 7.2.12, Flyway), Frontend (React 19, TypeScript 6, Vite 8), konfiguracja, testy  
**Autor:** Qwen Code Agent

---

## Status ogólny

| Element | Status |
|---------|--------|
| Backend build (Maven) | 🛠️ Zweryfikowano zmiany — wymaga JDK 17 do kompilacji |
| Backend testy jednostkowe | 🛠️ Naprawiono 9 błędów kompilacji w JwtUtilTest |
| Frontend build (`npm run build`) | ✅ tsc + vite OK (zweryfikowano) |
| Frontend testy (`npm test`) | ✅ 9 testów Pagination — wszystkie przeszły |
| Aplikacja na VPS | ⚠️ Działa, pozostały drobne błędy (patrz sekcje poniżej) |

---

## ✅ Naprawione w tej sesji (2025-07-14)

| ID | Opis | Pliki | Zmiana |
|----|------|-------|--------|
| B1 | **JwtUtilTest — 9 błędów kompilacji** | `JwtUtilTest.java` | Dodano `TENANT_ID = 1L` do wszystkich wywołań `generateToken()` |
| B2 | **EmailService — cross-tenant email leak** | `EmailService.java`, `UserRepository.java` | Dodano `findByRoleAndTenantId()`, zmieniono sygnaturę `sendHtmlToAdmins(subject, html, tenantId)` |
| B3 | **TenantService — licznik ignoruje tenanta** | `TenantService.java`, `UserRepository.java` | `count()` → `countByTenantId(tenantId)` |
| B4 | **NotificationService — brak izolacji tenantów** | `NotificationService.java` | Dodano `@Transactional`, iteracja po tenantach z `TenantContext` + Hibernate `@Filter` |
| F1 | **Dashboard race condition** | `DashboardPanel.tsx`, `api.ts` | Dodano `onWarehouseChange` listener, dashboard refetchuje przy zmianie magazynu |
| F2 | **Warehouse ID nie persistowany** | `api.ts` | `setCurrentWarehouseId()` zapisuje/usuwa z localStorage; odtwarzany przy starcie modułu |
| F3 | **Silent catch()** | `WarehouseSelector.tsx`, `TenantSettingsPanel.tsx` | Dodano komunikaty błędów zamiast pustego `.catch(() => {})` |

---

## Spis treści

1. [Błędy krytyczne (backend)](#1-błędy-krytyczne-backend)
2. [Błędy krytyczne (frontend)](#2-błędy-krytyczne-frontend)
3. [Sugestie backend](#3-sugestie-backend)
4. [Sugestie frontend](#4-sugestie-frontend)
5. [Drobne uwagi](#5-drobne-uwagi)
6. [Zachowane z poprzedniego audytu](#6-zachowane-z-poprzedniego-audytu)
7. [Podsumowanie](#7-podsumowanie)

---

## 1. Błędy krytyczne (backend)

### B1. JwtUtilTest — 9 błędów kompilacji

- **Plik:** `src/test/java/com/example/magazyn/service/JwtUtilTest.java:23,29,36,43,52,60,65,70`
- **Problem:** `JwtUtil.generateToken()` ma sygnaturę `(String username, String role, Long tenantId)` — 3 parametry. Testy wołają z 2 parametrami.
- **Skutek:** 0 testów backendu można uruchomić. Żadna regresja JWT nie zostanie wykryta.
- **Naprawa:** Dodać `, 1L` do każdego wywołania (10 miejsc).

### B2. EmailService — wyciek emaili między tenantami

- **Plik:** `src/main/java/com/example/magazyn/service/EmailService.java:54`
- **Problem:** `userRepository.findByRole("ROLE_ADMIN")` — brak filtra tenant_id. Zwraca adminów ze wszystkich tenantów.
- **Skutek:** Jeśli Tenant A wywoła powiadomienie o niskim stanie magazynowym, wszyscy admini Tenant B, C, D również dostają maila z nazwami produktów i poziomami stanów Tenant A. **Cross-tenant data leak.**
- **Naprawa:** Dodać zapytanie `findByRoleAndTenantId("ROLE_ADMIN", tenantId)` w UserRepository.

### B3. TenantService — userRepository.count() ignoruje tenanta

- **Plik:** `src/main/java/com/example/magazyn/service/TenantService.java:77,86`
- **Problem:** `userRepository.count()` zlicza użytkowników ze wszystkich tenantów. `userCount` w `TenantResponse` i `canAddUser()` są błędne.
- **Skutek:** Limit planu (`maxUsers=3`) nie działa. Tenant z 1 użytkownikiem może być zablokowany, jeśli inni tenanty mają łącznie 3+. Albo tenant może przekroczyć limit niezauważenie.
- **Naprawa:** Zastąpić `count()` → `countByTenantId(tenantId)`.

### B4. NotificationService — brak izolacji tenantów w zadaniu scheduled

- **Plik:** `src/main/java/com/example/magazyn/service/NotificationService.java`
- **Problem:** Metoda `@Scheduled` działa poza requestem HTTP → `TenantContext` pusty → zapytania typu `findExpiringBatches()` zwracają dane ze wszystkich tenantów. Dodatkowo **brak `@Transactional`** → przy dostępie do `b.getProduct().getName()` (Lazy loading) poleci `LazyInitializationException`.
- **Skutek:** Feature jest zepsuty — rzuci wyjątkiem przy pierwszym wykonaniu. Cross-tenant data leak.
- **Naprawa:** Dodać `@Transactional`, iterować po wszystkich tenantach i ustawiać `TenantContext.setTenantId()` dla każdego.

---

## 2. Błędy krytyczne (frontend)

### F1. Dashboard — race condition z WarehouseSelector

- **Plik:** `magazyn-frontend/src/components/DashboardPanel.tsx` (useEffect na mount)
- **Problem:** Dashboard ładuje dane statystyk natychmiast po zamontowaniu, zanim `WarehouseSelector` zdąży ustawić nagłówek `X-Warehouse-Id`. Dashboard nigdy nie robi refetcha po zmianie magazynu.
- **Skutek:** Użytkownik widzi globalne statystyki (wszystkie magazyny) zamiast filtrowanych po wybranym magazynie.
- **Naprawa:** Dashboard powinien zależeć od `currentWarehouseId` (np. przez kontekst lub props) i refetchować przy zmianie.

---

## 3. Sugestie backend

### B5. WarehouseService.updateWarehouse — brak checku duplikatu kodu

- **Plik:** `src/main/java/com/example/magazyn/service/WarehouseService.java:74`
- **Problem:** `createWarehouse()` sprawdza `existsByCodeAndTenantId()`, ale `updateWarehouse()` nie.
- **Skutek:** Można zmienić kod magazynu na istniejący w tym samym tenancie.

### B6. WarehouseRequest — brak adnotacji walidacyjnych

- **Plik:** `src/main/java/com/example/magazyn/dto/WarehouseRequest.java`
- **Problem:** Kontroler używa `@Valid @RequestBody`, ale DTO nie ma `@NotBlank`/`@Size`.
- **Skutek:** Puste nazwy/kody generują brzydkie błędy SQL zamiast 400 Bad Request.

### B7. AuditLogService — hardcodowany tenantId=1L

- **Plik:** `src/main/java/com/example/magazyn/service/AuditLogService.java:50`
- **Problem:** Fallback `TenantContext.getTenantId()` → `1L`.
- **Skutek:** Logi zdarzeń przed autoryzacją (np. nieudane logowania) dostają tenantId=1, nawet jeśli request był do innego tenanta.

### B8. StatsService — brak guarda na aktywny filtr Hibernate

- **Plik:** `src/main/java/com/example/magazyn/service/StatsService.java`
- **Problem:** Zapytania JPQL polegają na aktywnym `@Filter`, ale nie ma sprawdzenia czy filtr jest włączony.
- **Skutek:** Jeśli endpoint statystyk zostanie wywołany bez kontekstu tenanta, zapytania zwrócą globalne (cross-tenant) dane.

### B9. User entity — @Data eksponuje hasło

- **Plik:** `src/main/java/com/example/magazyn/entity/User.java`
- **Problem:** Lombok `@Data` generuje `getPassword()` → hash BCrypta jest dostępny dla każdego kodu.
- **Skutek:** Ryzyko wycieku hasła przez serializację.

### B10. StockController — dead code w @PreAuthorize (VIEWER)

- **Plik:** `src/main/java/com/example/magazyn/controller/StockController.java:36`
- **Problem:** `@PreAuthorize` wspomina `VIEWER`, ale `SecurityConfig` nie mapuje roli VIEWER do `/api/stock/**`.
- **Skutek:** Dead code — jeśli ktoś zmieni SecurityConfig, VIEWER nieoczekiwanie zyska dostęp do zapisu.

---

## 4. Sugestie frontend

### F2. Warehouse ID nie persistowany do localStorage

- **Plik:** `magazyn-frontend/src/services/api.ts:191-199`
- **Problem:** `setCurrentWarehouseId()` nie zapisuje ID do localStorage. Po odświeżeniu strony nagłówek `X-Warehouse-Id` znika.
- **Skutek:** Okno czasowe gdzie requesty lecą bez nagłówka magazynu → dane z wszystkich magazynów.

### F3. Silent catch() w WarehouseSelector i TenantSettingsPanel

- **Pliki:** `WarehouseSelector.tsx:25`, `TenantSettingsPanel.tsx:25`
- **Problem:** `.catch(() => {})` — błędy ładowania API są połykane bez informacji dla użytkownika.
- **Skutek:** Użytkownik nie wie, że lista magazynów/klucz API się nie załadowały.

### F4. Brak ThemeContext — dark mode nie w pełni zintegrowany

- **Problem:** Stan motywu jest lokalny w `ThemeToggle`, a nie w kontekście. Inne komponenty nie mogą go odczytać ani dostosować CSS.
- **Skutek:** Dark mode istnieje tylko jako przełącznik, ale nie zmienia wyglądu strony (brak CSS custom properties dla ciemnego motywu w głównym stylesheecie).

### F5. Brak i18n w większości komponentów

- **Problem:** `LangContext` istnieje i działa, ale ~80% komponentów ma stringi zakodowane na sztywno po polsku.
- **Dotknięte komponenty:** DashboardPanel, TenantSettingsPanel, ThemeToggle, Layout (częściowo), ProductTable, DocumentDetailModal, DeleteConfirmDialog, App.tsx (częściowo).

### F6. Brak opcji "Wszystkie magazyny" w WarehouseSelector

- **Problem:** Po wybraniu magazynu nie ma UI, żeby go odznaczyć i wrócić do widoku wszystkich magazynów.

---

## 5. Drobne uwagi

### Backend

- **WarehouseController** — brak `@PreAuthorize` na read endpointach (`getAll`, `getById`). Polega tylko na SecurityConfig.
- **Product entity** — dwa pola `locationId` i `defaultLocationId` (dezorientujące, ryzyko użycia złego).
- **Warehouse entity** — nie rozszerza `TenantAware` (niespójne z innymi encjami).
- **TenantSessionFilter / WarehouseSessionFilter** — Hibernate filtry nigdy nie są wyłączane przez `disable()`.

### Frontend

- **useProducts.ts, useReservations.ts, useContractors.ts** — `eslint-disable-next-line` na brakujących dependencjach w `useEffect`.

---

## 6. Zachowane z poprzedniego audytu

Poniższe znaleziska z poprzedniego audytu (2025-05-28) pozostają aktualne:

### Błędy

| ID | Opis | Plik | Status |
|----|------|------|--------|
| B11 | KOREKTA nie może ustawić stanu na 0 (`@Positive` vs `@PositiveOrZero`) | `StockMovementRequest.java:12` | ❌ Nienaprawione |
| B12 | RateLimitFilter ustawia CORS ręcznie (echo Origin) | `RateLimitFilter.java:49-52` | ❌ Nienaprawione |
| B13 | JwtUtil.isTokenValid() połyka wyjątki bez logowania | `JwtUtil.java:47-52` | ❌ Nienaprawione |
| B14 | ExportService.escapeCsv() nie obsługuje `\r` | `ExportService.java:221-227` | ❌ Nienaprawione |
| B15 | Brak paginacji w `GET /api/stock/{id}/movements` | `StockController.java` | ❌ Nienaprawione |

### Bezpieczeństwo

| ID | Opis | Status |
|----|------|--------|
| S1 | JWT Secret — wymagane ≥64 bajty dla HS512, brak walidacji przy starcie | ❌ Nienaprawione |
| S2 | Rate limiting na logowaniu: 20 req/min — akceptowalne dla portfolio | ⚠️ Średnie |
| S3 | Brak ochrony przed enumeracją ID (numeryczne ID w URL) | ❌ Nienaprawione |

### Testy — brak pokrycia

| Komponent | Testy | Status |
|-----------|-------|--------|
| ExportService | ❌ Brak | ❌ |
| ImportService | ❌ Brak | ❌ |
| StatsService | ❌ Brak | ❌ |
| SeedService | ❌ Brak | ❌ |
| RateLimitFilter | ❌ Brak | ❌ |
| JwtAuthenticationFilter | ❌ Brak | ❌ |
| StockController | ❌ Brak | ❌ |
| Testy integracyjne wymagają PostgreSQL (bez H2) | ❌ Utrudnione | ❌ |

### Wydajność

| Problem | Status |
|---------|--------|
| Brak indeksu trigram dla `LIKE '%...%'` na Product (dla dużych zbiorów) | ❌ |
| Brak paginacji w `GET /api/stock/{id}/movements` | ❌ |

---

## 7. Podsumowanie

### Nowe znaleziska (2025-07-14)

| Priorytet | Backend | Frontend | Razem |
|-----------|---------|----------|-------|
| 🔴 Critical | 4 (B1-B4) | 1 (F1) | **5** |
| 🟡 Suggestion | 5 (B5-B10, bez B8 jako Suggestion) | 5 (F2-F6) | **10** |
| 🔵 Nice to have | 4 | 3 | **7** |
| **Razem nowe** | **13** | **9** | **22** |

### Łącznie z poprzednim audytem

| Kategoria | Liczba |
|-----------|--------|
| 🔴 Critical | 5 |
| 🟡 Suggestion | ~15 |
| 🔵 Nice to have | ~15 |
| **Razem** | **~35** |

### Top 5 pozostałych rzeczy do naprawy

| # | Co | Szacowany czas | Trudność |
|---|----|---------------|----------|
| 1 | **RateLimitFilter CORS** — usunąć ręczne ustawianie CORS, zostawić CorsConfig | 10 min | Łatwe |
| 2 | **JwtUtil.isTokenValid()** — dodać logowanie wyjątków | 10 min | Łatwe |
| 3 | **KOREKTA quantity=0** — `@Positive` → `@PositiveOrZero` | 15 min | Łatwe |
| 4 | **Brak paginacji w movements** — dodać `Pageable` | 30 min | Średnie |
| 5 | **AuditLogService** — usunąć hardcodowany fallback `1L` | 10 min | Łatwe |
