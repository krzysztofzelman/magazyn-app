# Magazyn — Wielodzierżawczy System Zarządzania Magazynem (SaaS)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)](https://adoptium.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![JWT](https://img.shields.io/badge/JWT-jjwt%200.13.0-000000?logo=jsonwebtokens)](https://github.com/jwtk/jwt)
[![Docker](https://img.shields.io/badge/Docker-29.5.2-2496ED?logo=docker)](https://www.docker.com/)
[![Multi-Tenant](https://img.shields.io/badge/Multi--Tenant-SaaS-8A2BE2)](https://github.com/krzysztofzelman/magazyn-app)
[![PL/EN](https://img.shields.io/badge/lang-PL%2FEN-0099FF)](.)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=githubactions)](https://github.com/features/actions)
[![License](https://img.shields.io/badge/license-proprietary-red.svg)](LICENSE)

Backend REST API + frontend React SPA do kompleksowego zarządzania magazynem. System działa w modelu **SaaS (multi-tenant)** z izolacją danych przez `tenant_id`, wspiera wiele magazynów na firmę, oferuje samoobsługową rejestrację najemców oraz interfejs w języku polskim i angielskim.

**Produkcja:** [`https://magazyn.kzelman.pl`](https://magazyn.kzelman.pl)

**Swagger UI:** [`https://magazyn.kzelman.pl/swagger-ui/index.html`](https://magazyn.kzelman.pl/swagger-ui/index.html) (wymaga roli ADMIN)

**Ostatni audyt i deploy:** 2026-06-15 — gruntowna naprawa izolacji wielodzierżawczej: zastąpiono `TenantSessionFilter` (działał przed otwarciem sesji Hibernate) interceptorem `HandlerInterceptor` z `@PersistenceContext`, dodano jawny parametr `tenantId` do wszystkich metod `@Query` w repozytoriach (15 repozytoriów), zaktualizowano 18 serwisów do przekazywania `TenantContext.getTenantId()`, naprawiono `@Scheduled releaseExpired()` aby iterował wszystkich aktywnych najemców, obniżono Java 25 → 17 (zgodność z Docker `eclipse-temurin:17`), zmieniono `flyway.baseline-version=1` → 0 (automatyczne zastosowanie migracji V13). Szczegóły w [`AUDIT.md`](./AUDIT.md).

---

## Spis treści

- [Funkcjonalności](#funkcjonalności)
- [Stack technologiczny](#stack-technologiczny)
- [Architektura](#architektura)
  - [Schemat warstw](#schemat-warstw)
  - [Model danych (encje)](#model-danych-encje)
  - [Bezpieczeństwo](#bezpieczeństwo)
- [Endpointy REST](#endpointy-rest)
- [Frontend](#frontend)
- [Role i uprawnienia](#role-i-uprawnienia)
- [Zmienne środowiskowe](#zmienne-środowiskowe)
- [Uruchomienie lokalne](#uruchomienie-lokalne)
- [Docker](#docker)
- [Backup](#backup)
- [CI/CD](#cicd)
- [Struktura projektu](#struktura-projektu)

---

## Funkcjonalności

### Dokumenty magazynowe
- **PZ (Przyjęcie Zewnętrzne)** — przyjęcie towaru od dostawcy, automatyczne tworzenie partii (batch) z datą ważności, zwiększenie stanu magazynowego
- **WZ (Wydanie Zewnętrzne)** — wydanie towaru do odbiorcy, FIFO odpis z partii, automatyczne zwalnianie rezerwacji
- Cykl życia: **Szkic → Potwierdzony → Anulowany**
- Automatyczna numeracja dokumentów: `PZ/2026/001`, `WZ/2026/001`
- Eksport do PDF z użyciem iText7

### Śledzenie partii (Batch/Lot)
- Każde przyjęcie tworzy partie z numerem lotu, datą ważności i datą produkcji
- FIFO przy wydaniach — odpis z najstarszej partii (data ważności)
- Widok partii dla każdego produktu
- Powiadomienia o partiach bliskich wygaśnięcia

### Rezerwacje stanów
- Rezerwacja konkretnej ilości towaru na zamówienie lub ręcznie
- Automatyczne zwalnianie wygasłych rezerwacji (cron co 1h)
- Zwalnianie rezerwacji przy potwierdzeniu WZ
- Widok dostępności: stan fizyczny vs. dostępny (po odjęciu rezerwacji)

### Lokalizacje
- Hierarchiczna struktura: Magazyn → Regał → Półka → Kuweta (BIN)
- Drzewiasty widok w frontendzie
- Przypisanie produktu do konkretnej lokalizacji (BIN)

### Kontrahenci
- Zarządzanie dostawcami i odbiorcami
- Typy: Dostawca (SUPPLIER), Odbiorca (CUSTOMER)
- Unikalny NIP

### Eksport i import
- Eksport produktów do CSV / XLSX z wyborem pól
- Eksport stanów magazynowych do CSV / XLSX z wyborem pól
- Eksport dokumentów do PDF z użyciem iText7
- Import produktów z CSV / XLSX z upsert po SKU i walidacją
- Eksport dziennika audytu do CSV

### Skaner kodów kreskowych i QR
- Szybkie przyjęcie (Quick Receive) — skanuj kod, podaj ilość, system automatycznie tworzy przyjęcie na magazyn główny
- Szybkie wydanie (Quick Issue) — skanuj kod, podaj ilość, FIFO odpis z partii
- Podgląd produktu po zeskanowaniu — batch'e, lokalizacja, ostatni ruch

### Skanowanie lokalizacji w dokumentach PZ/WZ
- Skanowanie kodu kreskowego lokalizacji i przypisanie do pozycji dokumentu
- Dla PZ: skanowanie lokalizacji przypisuje ją do wszystkich pozycji bez lokalizacji
- Dla WZ: skanowanie lokalizacji sprawdza dostępność towaru w danej lokalizacji
- Obsługa przez dedykowane endpointy `/pz-documents/` i `/wz-documents/`

### Etykiety
- Generowanie etykiet A6 z kodem kreskowym CODE128 i QR dla lokalizacji
- Generowanie etykiet dla produktów
- Obsługa drukowania wielu etykiet jednocześnie (A4 layout)

### Sesje inwentaryzacyjne
- Tworzenie sesji inwentaryzacyjnych dla wybranego magazynu
- Automatyczne wypełnianie oczekiwanymi stanami z `location_stock`
- Skanowanie produktów w sesji — porównanie stanu oczekiwanego z rzeczywistym
- Raport różnic inwentaryzacyjnych (expected vs counted)
- Zamknięcie sesji aktualizuje `location_stock` na podstawie zliczonych wartości

### Zarządzanie użytkownikami i rolami
- Rejestracja użytkowników przez ADMIN
- Role: ADMIN (pełny dostęp), MANAGER/WAREHOUSE (operacje magazynowe), USER (podgląd + przyjęcia)
- Zmiana hasła przez każdego użytkownika dla siebie
- Dezaktywacja konta przez ADMIN

### Dashboard
- Liczba produktów i łączna wartość stanu
- Top sprzedawane produkty
- Alerty o niskich stanach (poniżej minQuantity)
- Liczba partii bliskich wygaśnięcia

### Powiadomienia e-mail
- Codzienny cron (6:00) — lista produktów z niskim stanem i partii bliskich wygaśnięcia
- Wysyłka do wszystkich użytkowników z rolą ADMIN
- Możliwość wyłączenia przez `app.notifications.enabled=false`

### Dziennik audytu
- Logowanie wszystkich akcji: logowanie, CRUD produktów, dokumenty, rezerwacje
- Filtrowanie po użytkowniku i typie akcji
- Widok tylko dla ADMIN

### Wielodzierżawczość (Multi-tenant SaaS)
- Współdzielona baza PostgreSQL z izolacją danych przez kolumnę `tenant_id` we wszystkich encjach
- Automatyczne ustawianie kontekstu najemcy z tokena JWT (`TenantContext` ThreadLocal)
- `HandlerInterceptor` z `@PersistenceContext` (zamiast `Servlet Filter`) — gwarantuje otwartą sesję Hibernate przed ustawieniem filtra, zastępuje poprzednie `TenantSessionFilter` i `WarehouseSessionFilter`
- Jawny parametr `tenantId` we wszystkich metodach `@Query` w repozytoriach — podwójne zabezpieczenie izolacji na poziomie zapytania SQL
- Filtr Hibernate `@Filter(name = "tenantFilter")` jako dodatkowa warstwa bezpieczeństwa automatycznie dodaje warunek `tenant_id = ?` do każdego zapytania
- Czyszczenie kontekstu po zakończeniu żądania (`TenantCleanupFilter`)
- Zadania `@Scheduled` (np. zwalnianie wygasłych rezerwacji) iterują wszystkich aktywnych najemców, aby uniknąć `NullPointerException` z braku kontekstu HTTP
- Rejestracja administracyjna nowego najemcy z automatycznym tworzeniem schematu bazowego (domyślny użytkownik ADMIN, domyślny magazyn, dane startowe)
- Każdy najemca widzi tylko swoje dane — izolacja na poziomie aplikacji, nie bazy

### Samoobsługowa rejestracja najemcy (Self-service)
- Publiczny endpoint `POST /api/tenants/register` (bez autoryzacji)
- Formularz: nazwa firmy, email, hasło, subdomena
- Automatyczne tworzenie konta ADMIN dla nowego najemcy
- Walidacja unikalności subdomeny
- Zwraca dane logowania, nazwę najemcy i klucz API

### Panel zarządzania najemcą
- Widok profilu najemcy: nazwa firmy, email, subdomena, aktualny plan
- Możliwość zmiany nazwy firmy i hasła głównego
- Podgląd kluczy API z możliwością regeneracji
- Widoczny tylko dla użytkowników z rolą ADMIN w danym tenant

### Limity planów (Plan limits enforcement)
- Każdy najemca ma przypisany plan: FREE / BASIC / PRO / ENTERPRISE
- Limity: max liczba użytkowników, produktów, magazynów, dokumentów miesięcznie
- Sprawdzanie limitów przy próbie utworzenia zasobu (user, product, warehouse, document)
- Zwraca błąd `429 PLAN_LIMIT_EXCEEDED` po przekroczeniu limitu
- Łatwa zmiana planu z poziomu panelu administracyjnego

### Dashboard z wykresami
- Wizualne karty (cards) z kluczowymi metrykami: produkty, wartość stanu, liczba dokumentów w tym miesiącu
- Wykres słupkowy: dokumenty PZ/WZ w ostatnich 30 dniach
- Wykres kołowy: rozkład wartości stanu magazynowego według produktów (top 10)
- Lista alertów: niskie stany, wygasające partie, przekroczone limity planu
- Odświeżanie przy każdym wejściu na dashboard

### Tryb ciemny (Dark mode)
- Przełącznik w headerze aplikacji (ikona księżyca/słońca)
- Zmienne CSS (`--bg-primary`, `--text-primary` itp.) definiujące paletę kolorów
- Stan zapisywany w `localStorage` — trwały między sesjami
- Płynne przejścia między motywami (CSS `transition`)
- Automatyczne dopasowanie do preferencji systemu (`prefers-color-scheme`) przy pierwszym uruchomieniu

### Powiadomienia e-mail HTML
- Codzienny cron (6:00) z raportem w formacie HTML
- Szablon z logo, stylowaniem inline i responsywnym layoutem
- Sekcje: niskie stany, wygasające partie, podsumowanie dokumentów z ostatnich 24h
- Stopka z linkiem do aplikacji i informacją o koncie
- Wysyłka przez `JavaMailSender` z szablonem HTML budowanym w `EmailService`

### Klucze API dla najemców (API Keys)
- Generowanie klucza API przy rejestracji najemcy (format: `mgz_` + 48 znaków hex)
- Możliwość wygenerowania nowego klucza z poziomu panelu najemcy (stary klucz traci ważność)
- Autoryzacja przez nagłówek `X-API-Key` — alternatywa dla JWT dla integracji zewnętrznych
- Klucz przechowywany jako zahaszowany `BCrypt` w tabeli `tenants`
- Endpointy wymagające klucza API oznaczone adnotacją

### Wielojęzyczność PL/EN
- Przełącznik języka w headerze aplikacji
- Wsparcie dla języka polskiego (`pl`) i angielskiego (`en`)
- Pliki tłumaczeń zorganizowane w `LangContext` (React Context API)
- Tłumaczenia obejmują: nawigację, etykiety formularzy, komunikaty błędów, tooltipy, powiadomienia
- Domyślny język: polski (`pl`)
- Łatwe dodawanie kolejnych języków przez rozszerzenie plików tłumaczeń

### Etykiety ZPL
- Generowanie etykiet w formacie ZPL (Zebra Programming Language) dla produktów
- Etykieta zawiera: nazwę produktu, SKU, kod kreskowy CODE128, datę ważności (jeśli batch), cenę
- Pobieranie jako plik tekstowy `.zpl` do bezpośredniego wysłania na drukarkę Zebra
- Endpoint `GET /api/products/{id}/label-zpl` zwraca surowy ZPL z nagłówkiem `Content-Type: application/x-zpl`
- Obsługa przez `LabelService` z możliwością rozszerzenia na lokalizacje

### Wielomagazynowość (Multi-warehouse)
- Każdy najemca może mieć wiele magazynów
- Przełącznik aktywnego magazynu w headerze aplikacji (zapisywany w `localStorage`)
- Kontekst magazynu (`WarehouseContext` ThreadLocal) ustawiany z nagłówka `X-Warehouse-Id`
- Filtr Hibernate `@Filter(name = "warehouseFilter")` automatycznie izoluje dane magazynu
- Zarządzanie magazynami: CRUD z nazwą, kodem, adresem i statusem aktywnym
- Podział lokalizacji, stanów magazynowych, dokumentów i sesji inwentaryzacyjnych według magazynu

---

## Stack technologiczny

### Backend

| Technologia | Wersja | Zastosowanie |
|---|---|---|
| Java | 25 (LTS) | Język programowania |
| Spring Boot | 4.0.6 | Framework aplikacyjny, osadzony Tomcat |
| Spring Data JPA / Hibernate | — | ORM, automatyczne DDL (ddl-auto=update) |
| Spring Security | 7.x | Autoryzacja, uwierzytelnianie, BCrypt, @EnableMethodSecurity |
| Spring Validation | — | Walidacja adnotacjami (@NotBlank, @Size, @Positive) |
| jjwt (io.jsonwebtoken) | 0.13.0 | Generowanie i weryfikacja tokenów JWT (HS512) |
| PostgreSQL | 18 | Relacyjna baza danych |
| Lombok | — | Redukcja boilerplate (@Data, @Builder, @NoArgsConstructor) |
| Apache POI | 5.4.0 | Generowanie plików Excel (.xlsx) |
| iText7 | 7.2.5 | Generowanie dokumentów PDF dla etykiet i dokumentów |
| ZXing | 3.5.2 | Generowanie kodów kreskowych CODE128 i QR |
| Bucket4j | 8.19.0 | Rate limiting (20 żądań/min na endpoint logowania) |
| Springdoc OpenAPI | 2.7.0 | Swagger UI / OpenAPI docs |
| Testcontainers | 1.21.4 | Testy integracyjne z bazą PostgreSQL |

### Frontend

| Technologia | Wersja | Zastosowanie |
|---|---|---|
| React | 19 | Framework UI |
| TypeScript | 6.0 | System typów |
| Vite | 8.0 | Bundler i dev server |
| Axios | 1.16 | Klient HTTP |
| Vitest | 4.1 | Testy jednostkowe |
| @testing-library/react | 16 | Testy komponentów React |

---

## Architektura

### Schemat warstw

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (React SPA)                   │
│  Vite build → bundle do /src/main/resources/static/      │
└──────────────────────┬──────────────────────────────────┘
                       │ same-origin (HTTPS)
┌──────────────────────▼──────────────────────────────────┐
│              Nginx reverse proxy (VPS)                    │
│  SSL termination (Let's Encrypt) → localhost:8080        │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│              Spring Boot (Docker container)               │
│  Controller → Service → Repository → PostgreSQL          │
│  Security: JwtAuthenticationFilter → @PreAuthorize       │
└──────────────────────┬──────────────────────────────────┘
                       │ JDBC (port 5432)
┌──────────────────────▼──────────────────────────────────┐
│              PostgreSQL 18 (Docker container)             │
│              Volume: postgres_data                        │
└─────────────────────────────────────────────────────────┘
```

**Zasada:** Kontrolery przyjmują i zwracają DTO, nie encje. Logika biznesowa w serwisach. Repozytoria operują na encjach JPA. Frontend wbudowany w JAR jako statyczne resources — same-origin, brak CORS.

### Model danych (encje)

```
Product ──1:N──> StockMovement
Product ──1:N──> Batch
Product ──1:N──> StockReservation
Product *──1──> Location
Product ──1:N──> WarehouseDocumentItem

WarehouseDocument ──1:N──> WarehouseDocumentItem
WarehouseDocument *──1──> Contractor
WarehouseDocumentItem *──1──> Product

Location (self-referencing ── parentId)
Location ──1:N──> LocationStock

LocationStock *──1──> Location (via locationId)
LocationStock *──1──> Product (via productId)
LocationStock: quantity, reservedQuantity, @Version (optimistic locking)

Batch *──1──> Location

User ──1:N──> RefreshToken
User ──1:N──> AuditLog

InventorySession ──1:N──> InventoryItem
```

| Encja | Tabela | Kluczowe pola |
|---|---|---|
| `Product` | `products` | id, name, sku (unique), description, unit, quantity, price, minQuantity, locationId, categoryId, defaultLocationId, barcode, trackExpiry |
| `StockMovement` | `stock_movements` | id, type (PRZYJECIE/WYDANIE/KOREKTA), quantity, note, createdBy, batchId, product_id |
| `Batch` | `batches` | id, lotNumber, expiryDate, manufacturingDate, quantity, product_id, locationId |
| `Location` | `locations` | id, code, name, type (WAREHOUSE/RACK/SHELF/BIN), parentId |
| `LocationStock` | `location_stock` | id, locationId, productId, quantity, reservedQuantity, updatedAt, @Version |
| `Contractor` | `contractors` | id, name, taxId (unique), type (SUPPLIER/CUSTOMER), active |
| `WarehouseDocument` | `warehouse_documents` | id, number (unique), type (PZ/WZ), status (DRAFT/CONFIRMED/CANCELLED), contractor_id |
| `WarehouseDocumentItem` | `warehouse_document_items` | id, quantity, unitPrice, lotNumber, expiryDate, product_id, document_id |
| `StockReservation` | `stock_reservations` | id, quantity, status (ACTIVE/RELEASED/FULFILLED), expiresAt, product_id |
| `InventorySession` | `inventory_sessions` | id, name, status (OPEN/CLOSED), warehouseId, createdBy |
| `InventoryItem` | `inventory_items` | id, sessionId, locationId, productId, expectedQuantity, countedQuantity |
| `AuditLog` | `audit_logs` | id, username, action, entityType, entityId, details, ipAddress, timestamp |
| `User` | `users` | id, username (unique), password (BCrypt), role, email |
| `RefreshToken` | `refresh_tokens` | id, token (UUID, unique), expiresAt, user_id |

### Bezpieczeństwo

- Każde żądanie (oprócz `POST /api/auth/login`) przechodzi przez `JwtAuthenticationFilter`
- Filtr wyciąga token JWT z nagłówka `Authorization: Bearer <token>`, weryfikuje podpis i datę ważności
- `RateLimitFilter` chroni endpoint logowania (20 żądań/minutę na IP)
- `AuditLogFilter` wyciąga adres IP klienta i zapisuje go w `ThreadLocal` (AuditContext)
- Role sprawdzane na poziomie metod przez `@PreAuthorize`
- Token odświeżania (refresh token) — UUID zapisany w bazie, rotacja przy każdym użyciu
- Sesje bezstanowe (stateless) — brak HttpSession, każdy request autoryzowany osobno

---

## Endpointy REST

### Autoryzacja — `/api/auth`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/auth/register` | Rejestracja nowego użytkownika | ADMIN |
| POST | `/api/auth/login` | Logowanie, zwraca token JWT + refresh token | Publiczny (rate limit 20/min/IP) |
| POST | `/api/auth/refresh` | Odświeżenie tokena JWT | Posiadacz refresh tokena |
| POST | `/api/auth/logout` | Wylogowanie (unieważnia refresh token) | Zalogowany |

### Produkty — `/api/products`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/products` | Lista produktów z paginacją i wyszukiwaniem | Zalogowany |
| GET | `/api/products/{id}` | Produkt po ID | Zalogowany |
| GET | `/api/products/sku/{sku}` | Produkt po SKU | Zalogowany |
| POST | `/api/products` | Utworzenie produktu | ADMIN |
| PUT | `/api/products/{id}` | Aktualizacja produktu (częściowa) | ADMIN |
| DELETE | `/api/products/{id}` | Usunięcie produktu | ADMIN |
| POST | `/api/products/import` | Import z CSV/XLSX | ADMIN |
| GET | `/api/products/import/template` | Pobranie szablonu importu | ADMIN |
| PATCH | `/api/products/{id}/location` | Przypisanie lokalizacji | ADMIN |
| GET | `/api/products/{id}/batches` | Partie produktu | Zalogowany |
| GET | `/api/products/{id}/availability` | Dostępność (stan - rezerwacje) | Zalogowany |

**Wyszukiwanie:** `?search=term` — po nazwie lub SKU (case-insensitive).

### Stan magazynowy — `/api/stock`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/stock/{productId}` | Aktualny stan produktu (ilość, wartość) | Zalogowany |
| GET | `/api/stock/{productId}/movements?page=0&size=10` | Historia ruchów (paginacja, DESC) | Zalogowany |
| POST | `/api/stock/{productId}/movement` | Dodanie ruchu | ADMIN / USER (tylko PRZYJECIE) |

**Typy ruchu:** `PRZYJECIE` (zwiększa stan), `WYDANIE` (zmniejsza, FIFO batch), `KOREKTA` (ustawia dokładnie).

### Dokumenty magazynowe — `/api/documents`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/documents` | Utworzenie dokumentu (Szkic) | ADMIN |
| GET | `/api/documents?type=PZ&status=DRAFT` | Lista dokumentów (filtrowana, paginacja) | Zalogowany |
| GET | `/api/documents/{id}` | Szczegóły dokumentu z pozycjami | Zalogowany |
| POST | `/api/documents/{id}/confirm` | Potwierdzenie dokumentu (PZ → +stock, WZ → -stock FIFO) | ADMIN |
| POST | `/api/documents/{id}/cancel` | Anulowanie dokumentu | ADMIN |
| GET | `/api/documents/{id}/export/pdf` | Eksport dokumentu do PDF | Zalogowany |

**Filtrowanie:** `?type=PZ&status=DRAFT&page=0&size=20`.

**Potwierdzenie PZ:** Tworzy partie (batch) dla każdej pozycji, zwiększa stan produktu.
**Potwierdzenie WZ:** FIFO odpis z partii, zwalnia aktywne rezerwacje, zmniejsza stan.

### Rezerwacje — `/api/reservations`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/reservations` | Utworzenie rezerwacji | ADMIN |
| GET | `/api/reservations?status=ACTIVE&productId=` | Lista rezerwacji (filtrowana) | Zalogowany |
| DELETE | `/api/reservations/{id}` | Zwolnienie rezerwacji | ADMIN |

### Lokalizacje — `/api/locations`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/locations` | Lista lokalizacji | Zalogowany |
| GET | `/api/locations/tree` | Drzewo lokalizacji (hierarchiczne) | Zalogowany |
| GET | `/api/locations/{id}` | Lokalizacja po ID | Zalogowany |
| GET | `/api/locations/{id}/products` | Produkty w lokalizacji | Zalogowany |
| GET | `/api/locations/{id}/stock` | Stan magazynowy w lokalizacji | Zalogowany |
| POST | `/api/locations` | Utworzenie lokalizacji | ADMIN |
| PUT | `/api/locations/{id}` | Aktualizacja lokalizacji | ADMIN |
| DELETE | `/api/locations/{id}` | Usunięcie lokalizacji (sprawdza dzieci) | ADMIN |
| GET | `/api/locations/{id}/barcode-image` | Obraz kodu kreskowego lokalizacji (PNG) | Zalogowany |
| GET | `/api/locations/{id}/qr-image` | Obraz kodu QR lokalizacji (PNG) | Zalogowany |
| GET | `/api/locations/{id}/label-pdf` | Etykieta A6 dla lokalizacji (PDF) | Zalogowany |
| POST | `/api/locations/transfer` | Przeniesienie towaru między lokalizacjami | MANAGER/WAREHOUSE |

### Skaner — `/api/scanner`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/scanner/lookup?code=` | Podgląd produktu po kodzie (wraz z partiami, lokalizacją, ostatnim ruchem) | Zalogowany |
| POST | `/api/scanner/quick-receive` | Szybkie przyjęcie na magazyn główny | MANAGER/WAREHOUSE |
| POST | `/api/scanner/quick-issue` | Szybkie wydanie z FIFO | MANAGER/WAREHOUSE |

### Dokumenty PZ/WZ — skanowanie lokalizacji

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/pz-documents/{id}/scan-location` | Przypisz lokalizację do wszystkich pozycji PZ | MANAGER/WAREHOUSE |
| POST | `/api/pz-documents/{id}/items/{itemId}/scan-location` | Przypisz lokalizację do konkretnej pozycji PZ | MANAGER/WAREHOUSE |
| POST | `/api/wz-documents/{id}/items/{itemId}/scan-location` | Skanuj lokalizację dla pozycji WZ (sprawdza dostępność) | MANAGER/WAREHOUSE |

### Sesje inwentaryzacyjne — `/api/inventory`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/inventory/sessions` | Utwórz sesję inwentaryzacyjną | ADMIN |
| GET | `/api/inventory/sessions` | Lista sesji | Zalogowany |
| GET | `/api/inventory/sessions/{id}` | Szczegóły sesji | Zalogowany |
| POST | `/api/inventory/sessions/{id}/scan` | Zeskanuj produkt w sesji | MANAGER/WAREHOUSE |
| GET | `/api/inventory/sessions/{id}/report` | Raport różnic inwentaryzacyjnych | Zalogowany |
| POST | `/api/inventory/sessions/{id}/close` | Zamknij sesję (aktualizuje stany) | ADMIN |

### Zarządzanie użytkownikami — `/api/users`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/users` | Lista użytkowników | ADMIN |
| GET | `/api/users/{id}` | Szczegóły użytkownika | ADMIN lub sam użytkownik |
| GET | `/api/users/me` | Szczegóły zalogowanego użytkownika | Zalogowany |
| POST | `/api/users` | Utwórz użytkownika | ADMIN |
| PUT | `/api/users/{id}` | Aktualizacja użytkownika | ADMIN |
| DELETE | `/api/users/{id}` | Dezaktywacja użytkownika | ADMIN |
| POST | `/api/users/change-password` | Zmiana hasła | Zalogowany |

### Kontrahenci — `/api/contractors`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/contractors?search=` | Lista kontrahentów z wyszukiwaniem | Zalogowany |
| GET | `/api/contractors/{id}` | Kontrahent po ID | Zalogowany |
| GET | `/api/contractors/search?name=&taxId=` | Wyszukiwanie kontrahentów | Zalogowany |
| POST | `/api/contractors` | Utworzenie kontrahenta | ADMIN |
| PUT | `/api/contractors/{id}` | Aktualizacja kontrahenta | ADMIN |
| DELETE | `/api/contractors/{id}` | Usunięcie kontrahenta (sprawdza dokumenty) | ADMIN |

### Dashboard — `/api/stats`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/stats/dashboard` | Statystyki: liczba produktów, wartość stanu, top sprzedawane, alerty, wygasające partie | Zalogowany |

### Eksport — `/api`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/products/export/csv?format=csv&fields=name,sku,quantity` | Eksport produktów | Zalogowany |
| GET | `/api/stock/export/excel?format=xlsx&fields=productName,quantity` | Eksport stanów magazynowych | Zalogowany |

### Audyt — `/api/audit`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/audit?username=&action=&page=0&size=20` | Dziennik audytu (filtrowany, paginacja) | ADMIN |
| GET | `/api/audit/export` | Eksport audytu do CSV | ADMIN |

### Partie — `/api/batches`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/batches/expiring?days=30` | Partie wygasające w ciągu N dni | Zalogowany |
| GET | `/api/batches/expired` | Partie już wygasłe | Zalogowany |

### Inicjalizacja — `/api/seed`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/seed/locations` | Zasiew przykładowych lokalizacji (drzewo 16 węzłów) | ADMIN |

### Najemcy (Tenants) — `/api/tenants`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/tenants/register` | Samoobsługowa rejestracja nowego najemcy | Publiczny |
| GET | `/api/tenants` | Lista najemców | ADMIN (nadrzędny) |
| GET | `/api/tenants/me` | Profil bieżącego najemcy | ADMIN |
| PUT | `/api/tenants/me` | Aktualizacja profilu najemcy | ADMIN |
| PUT | `/api/tenants/me/plan` | Zmiana planu taryfowego | ADMIN (nadrzędny) |
| POST | `/api/tenants/me/regenerate-api-key` | Regeneracja klucza API | ADMIN |

### Magazyny (Warehouses) — `/api/warehouses`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/warehouses` | Lista magazynów najemcy | Zalogowany |
| POST | `/api/warehouses` | Utworzenie magazynu | ADMIN |
| PUT | `/api/warehouses/{id}` | Aktualizacja magazynu | ADMIN |
| DELETE | `/api/warehouses/{id}` | Usunięcie magazynu | ADMIN |

### Produkty — etykiety ZPL

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/products/{id}/label-zpl` | Etykieta ZPL produktu (dla drukarki Zebra) | Zalogowany |

---

## Frontend

Frontend to SPA napisane w React 19 + TypeScript 6.0, budowane przez Vite i serwowane jako statyczne zasoby wbudowane w JAR.

### Widoki (zakładki)

| Zakładka | Komponent | Opis |
|---|---|---|
| **Panel** | `DashboardPanel` | Karty z metrykami (produkty, wartość stanu, partie do wygaśnięcia, alerty stanów). Lista najczęściej wydawanych produktów. Tabela produktów poniżej minimalnego stanu. |
| **Produkty** | `ProductTable`, `StockPanel`, `BatchPanel` | Tabela produktów z paginacją, wyszukiwarką. Dla każdego produktu: panel stanu z historią ruchów, panel partii (batch/lot) z datami ważności. CRUD przez modale. |
| **Dokumenty** | `DocumentList`, `DocumentFormModal`, `DocumentDetailModal` | Lista dokumentów PZ/WZ z filtrowaniem po typie i statusie. Tworzenie, potwierdzanie, anulowanie. Eksport PDF. |
| **Lokalizacje** | `LocationPanel`, `LocationTree`, `LocationDetailDrawer` | Hierarchiczne drzewo lokalizacji (Magazyn → Regał → Półka → Kuweta). Stan magazynowy w lokalizacji, przenoszenie towaru. |
| **Skaner** | `ScannerTab`, `ScannerPanel` | Multi-mode skaner kodów kreskowych — szybkie przyjęcie (PZ), szybkie wydanie (WZ), transfer, inwentaryzacja. Obsługa kamery (ZXing) i klawiatury. |
| **Inwentaryzacja** | `InventoryPanel` | Sesje inwentaryzacyjne: tworzenie, skanowanie produktów, raport różnic, zamykanie sesji z aktualizacją stanów. |
| **Ustawienia** | `TenantSettingsPanel`, `WarehousePanel` | Profil firmy (nazwa, subdomena, plan, licznik użytkowników). Klucz API (pokaż/ukryj/kopiuj/wygeneruj nowy). Zarządzanie magazynami (CRUD). |

**Header (widoczny zawsze):** przełącznik magazynu (`WarehouseSelector`), dark mode (`ThemeToggle`), język PL/EN (`LangToggle`), przyciski Dodaj/Import produktu (admin), profil użytkownika (`ProfilePanel` — podgląd + zmiana hasła), wylogowanie.

**Komponenty istniejące w kodzie, ale niepodpięte w interfejsie:** `ContractorTable`, `ReservationPanel`, `UserManagementPanel`, `AuditLogPanel` — wymagają dokończenia integracji. **Kontrahenci** i **Rezerwacje** są dostępne przez dedykowane endpointy API.

### Funkcje przekrojowe (header)

| Funkcja | Komponent | Opis |
|---|---|---|
| **Dark mode** | `ThemeToggle` | Przełącznik motywu ciemny/jasny z zapisem w localStorage |
| **Multi-language** | `LangToggle` | Przełącznik języka PL/EN z zapisem w localStorage |
| **Warehouse switcher** | `WarehouseSelector` | Przełącznik aktywnego magazynu dla zalogowanego użytkownika |

### Hooks i konteksty (stan)

| Hook / Context | Zarządza | Stan |
|---|---|---|
| `useAuth` | Stan autoryzacji, login/logout, JWT w localStorage | ✅ Podpięty |
| `useProducts` | Lista produktów, paginacja, wyszukiwanie, CRUD | ✅ Podpięty |
| `useDocuments` | Lista dokumentów (filtry typ/status), paginacja, CRUD | ✅ Podpięty |
| `useNotification` | Komunikaty toast (auto-hide 4s) | ✅ Podpięty |
| `useContractors` | Lista kontrahentów, wyszukiwanie, CRUD | ⏳ Istnieje, niepodpięty |
| `useReservations` | Lista rezerwacji, filtry, tworzenie/zwalnianie | ⏳ Istnieje, niepodpięty |
| `useBarcodeScanner` | Skaner kamery (ZXing + video stream) | ⏳ Istnieje, niepodpięty |
| `useScannerInput` | Keyboard-wedge scanner (buforowanie + debounce) | ⏳ Istnieje, niepodpięty |
| `ThemeContext` | Motyw dark/light, localStorage | ✅ Podpięty |
| `LangContext` | Język pl/en, tłumaczenia, localStorage | ✅ Podpięty |
| `WarehouseContext` | Aktywny magazyn, lista, przełączanie | ⏳ Istnieje, niepodpięty (użyto `onWarehouseChange` w api.ts) |

---

## Role i uprawnienia

| Rola | Uprawnienia |
|---|---|
| `ROLE_ADMIN` | Pełny dostęp: CRUD produktów, dokumenty PZ/WZ (tworzenie, potwierdzanie, anulowanie), rezerwacje, kontrahenci, lokalizacje, import/eksport, dziennik audytu, seed danych, zarządzanie użytkownikami, sesje inwentaryzacyjne |
| `ROLE_MANAGER` / `ROLE_WAREHOUSE` | Operacje magazynowe: tworzenie i potwierdzanie dokumentów PZ/WZ, skanowanie lokalizacji, szybkie przyjęcia/wydania, skanowanie w sesjach inwentaryzacyjnych, przenoszenie towaru między lokalizacjami |
| `ROLE_USER` | Podgląd: produkty, stany magazynowe, dokumenty, kontrahenci, lokalizacje, rezerwacje, statystyki, eksport. Może dodawać tylko ruchy PRZYJECIE. |

Nowi użytkownicy rejestrowani są z rolą `ROLE_USER`. Nadanie roli `ROLE_ADMIN`/`ROLE_MANAGER`/`ROLE_WAREHOUSE` wymaga ręcznej zmiany w bazie danych lub użycia panelu zarządzania użytkownikami przez ADMIN.

---

## Zmienne środowiskowe

| Zmienna | Opis | Wymagane |
|---|---|---|
| `DB_URL` | URL połączenia z PostgreSQL (`jdbc:postgresql://postgres:5432/magazyn_db`) | Tak |
| `DB_USERNAME` | Użytkownik bazy danych | Tak |
| `DB_PASSWORD` | Hasło użytkownika bazy | Tak |
| `JWT_SECRET` | Klucz do podpisu JWT (min. 32 znaki, Base64) | Tak |
| `JWT_EXPIRATION` | Czas ważności tokena (ms, domyślnie 86400000 = 24h) | Nie |
| `NOTIFICATIONS_ENABLED` | Włączenie powiadomień e-mail (domyślnie false) | Nie |
| `MAIL_HOST` | Host SMTP | Gdy notifications włączone |
| `MAIL_PORT` | Port SMTP (587) | Gdy notifications włączone |
| `MAIL_USERNAME` | Użytkownik SMTP | Gdy notifications włączone |
| `MAIL_PASSWORD` | Hasło SMTP | Gdy notifications włączone |
| `MAIL_FROM` | Adres nadawcy e-mail | Gdy notifications włączone |
| `DEFAULT_TENANT_PLAN` | Domyślny plan przy rejestracji (FREE/BASIC/PRO, domyślnie FREE) | Nie |

### Przykładowy plik `.env`

```env
DB_URL=jdbc:postgresql://postgres:5432/magazyn_db
DB_USERNAME=magazyn_user
DB_PASSWORD=zmien_haslo
JWT_SECRET=zmien_na_wlasny_klucz_o_dlugosci_co_najmniej_32_znakow
JWT_EXPIRATION=86400000
NOTIFICATIONS_ENABLED=false
MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=
DEFAULT_TENANT_PLAN=FREE
```

Plik `.env` znajduje się w `.gitignore`. W repozytorium dostępny jest szablon `.env.example`.

---

## Uruchomienie lokalne

### Wymagania

- **Java 17 JDK (LTS)** — [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=17)
- **Maven 3.8+** — lub użyj dołączonego `mvnw` (Maven Wrapper)
- **Docker Desktop** — do PostgreSQL (lub lokalna instalacja PostgreSQL 18)

### Szybki start (Docker Compose)

```bash
git clone https://github.com/krzysztofzelman/magazyn-app.git
cd magazyn-app
cp .env.example .env
# edytuj .env — ustaw JWT_SECRET i hasła
docker compose up -d --build
```

Aplikacja dostępna na `http://localhost:8080`.

### Uruchomienie bez Dockera (backend + zewnętrzny PostgreSQL)

```bash
# 1. Uruchom PostgreSQL (np. przez Dockera)
docker compose up -d postgres

# 2. Uruchom aplikację
./mvnw spring-boot:run
```

### Budowa frontendu osobno (do developmentu)

```bash
cd magazyn-frontend
npm install
npm run dev    # dev server na localhost:5173
```

### Uruchomienie testów

```bash
# Backend — wszystkie testy
./mvnw test

# Backend — tylko testy jednostkowe
./mvnw test -Dtest="*Test,*IntegrationTest"

# Frontend
cd magazyn-frontend
npx vitest
```

### Seed danych demo

Po uruchomieniu aplikacji:

```bash
# Zasiew przykładowych lokalizacji (drzewo 16 węzłów)
curl -X POST http://localhost:8080/api/seed/locations -H "Authorization: Bearer <admin-token>"
```

---

## Docker

### Obrazy i kontenery

```yaml
services:
  postgres:      # PostgreSQL 18, port 5432, volume postgres_data, healthcheck
  app:           # Spring Boot, port 8080, zależny od postgres (healthy)
  backup:        # PostgreSQL 18, uruchamia pg_dump w pętli 24h
```

### Dockerfile (multi-stage)

- **Build:** `maven:3.9-eclipse-temurin-17` — kompiluje projekt
- **Runtime:** `eclipse-temurin:17-jre` — uruchamia JAR

### Komendy

```bash
# Budowa i uruchomienie pełnego stacka
docker compose up -d --build

# Zatrzymanie
docker compose down

# Podgląd logów
docker compose logs -f app

# Czystka nieużywanych obrazów
docker image prune -f
```

---

## Backup

System automatycznego backupu bazy danych uruchamiany jako osobny kontener Docker.

- **Narzędzie:** `pg_dump` z kompresją gzip
- **Harmonogram:** co 24 godziny
- **Lokalizacja:** volume Docker `magazyn_backups` → `/backups/`
- **Nazwa pliku:** `magazyn_YYYYMMDD_HHMMSS.sql.gz`
- **Retencja:** 7 dni (automatyczne usuwanie starszych plików)

Backup można również wykonać ręcznie:

```bash
docker exec magazyn-app-postgres-1 pg_dump -U magazyn_user magazyn_db | gzip > backup_$(date +%F).sql.gz
```

---

## CI/CD

Wdrożenie produkcyjne odbywa się automatycznie przez GitHub Actions przy każdym pushu na gałąź `main`.

**Plik:** `.github/workflows/deploy.yml`

**Przebieg:**
1. Trigger: push do `main`
2. Runner: `ubuntu-latest`
3. Połączenie SSH z VPS (`REMOVED`, port `2022`)
4. Skrypt na VPS:
   ```bash
   docker compose exec postgres pg_dump -U magazyn_user magazyn_db | gzip > /backups/pre-deploy-$(date +%F-%H%M%S).sql.gz
   cd /root/magazyn-app
   git pull origin main
   docker compose down
   docker compose up -d --build
   docker image prune -f
   ```

**Wymagane Secret w GitHub:**
- `VPS_SSH_KEY` — klucz prywatny SSH

### Produkcja — architektura

```
Internet ──HTTPS──> Nginx (port 443, Let's Encrypt)
                         │ proxy_pass http://localhost:8080
                    ┌────▼────┐
                    │  Docker  │
                    │  :8080   │
                    └─────────┘
```

---

## Struktura projektu

```
magazyn-app/
│
├── src/main/java/com/example/magazyn/
│   ├── MagazynApplication.java        # @SpringBootApplication + @EnableScheduling
│   ├── auth/                          # Autoryzacja (kontroler, serwis, DTO)
│   ├── config/                        # Security, filtry JWT/RateLimit/AuditLog, OpenAPI, TenantContext, WarehouseContext, WarehouseSessionFilter
│   ├── controller/                    # REST API (18 kontrolerów: +TenantController, +WarehouseController)
│   ├── dto/                           # Data Transfer Objects (45+ klas: +RegisterTenantRequest, +TenantResponse, +ApiKeyResponse, +WarehouseRequest/Response)
│   ├── entity/                        # Encje JPA + enums (+Warehouse, +tenant_id na encjach, +@Filter(name = "tenantFilter"), +@Filter(name = "warehouseFilter"))
│   ├── exception/                     # GlobalExceptionHandler + 7 custom exceptions
│   ├── repository/                    # Spring Data JPA (15 repozytoriów: +WarehouseRepository)
│   ├── service/                       # Logika biznesowa (18 serwisów: +TenantService, +WarehouseService)
│   └── util/                          # JwtUtil, AuditContext
│
├── src/main/resources/
│   ├── application.properties         # Konfiguracja (env-based)
│   ├── db/migration/                  # Migracje Flyway (+V11__add_warehouses.sql)
│   └── static/                        # Frontend bundle (index.html, assets/)
│
├── src/test/java/com/example/magazyn/
│   ├── service/                       # Testy jednostkowe (Mockito)
│   ├── integration/                   # Testy integracyjne (WebTestClient)
│   └── MagazynApplicationTests.java
│
├── magazyn-frontend/                  # Frontend React + TypeScript
│   ├── src/
│   │   ├── components/                # Komponenty React (23 pliki: +DashboardPanel, +WarehousePanel, +WarehouseSelector, +TenantSettingsPanel, +ThemeToggle, +LangToggle)
│   │   ├── hooks/                     # Niestandardowe hooki (5)
│   │   ├── contexts/                  # Konteksty React (ThemeContext, LangContext, WarehouseContext)
│   │   ├── services/                  # Klient API (Axios — rozszerzony o serwisy tenant/warehouse)
│   │   ├── types/                     # Interfejsy TypeScript (rozszerzone o tenant/warehouse/apiKey)
│   │   └── __tests__/                 # Testy komponentów
│   ├── vite.config.ts
│   └── package.json
│
├── .github/workflows/deploy.yml       # CI/CD do VPS
├── scripts/backup.sh                   # Backup PostgreSQL
├── Dockerfile                          # Multi-stage build
├── docker-compose.yml                  # postgres + app + backup
├── .env.example                        # Szablon zmiennych środowiskowych
├── pom.xml                             # Maven (Spring Boot 4.0.6)
├── magazyn.service                     # systemd unit (legacy, nieużywany z Dockerem)
└── README.md                           # Niniejszy plik
```

---

## Licencja

Projekt prywatny — wszystkie prawa zastrzeżone.
