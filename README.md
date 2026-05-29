# Magazyn — System Zarządzania Magazynem

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk)](https://adoptium.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![JWT](https://img.shields.io/badge/JWT-jjwt%200.13.0-000000?logo=jsonwebtokens)](https://github.com/jwtk/jjwt)
[![Docker](https://img.shields.io/badge/Docker-29.5.2-2496ED?logo=docker)](https://www.docker.com/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=githubactions)](https://github.com/features/actions)

Backend REST API + frontend React SPA do kompleksowego zarządzania magazynem. System obsługuje pełny cykl dokumentów magazynowych (PZ, WZ), śledzenie partii (batch/lot), rezerwacje stanów, lokalizacje produktów, FIFO przy wydaniach oraz eksport danych.

**Produkcja:** [`https://magazyn.kzelman.pl`](https://magazyn.kzelman.pl)

**Swagger UI:** [`https://magazyn.kzelman.pl/swagger-ui/index.html`](https://magazyn.kzelman.pl/swagger-ui/index.html) (wymaga roli ADMIN)

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
- Eksport do PDF z użyciem Apache PDFBox

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
- Eksport dokumentów do PDF
- Import produktów z CSV / XLSX z upsert po SKU i walidacją
- Eksport dziennika audytu do CSV

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
| PostgreSQL | 16 | Relacyjna baza danych |
| Lombok | — | Redukcja boilerplate (@Data, @Builder, @NoArgsConstructor) |
| Apache POI | 5.4.0 | Generowanie plików Excel (.xlsx) |
| Apache PDFBox | 3.0.4 | Generowanie dokumentów PDF |
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
│              PostgreSQL 16 (Docker container)             │
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
Batch *──1──> Location

User ──1:N──> RefreshToken
User ──1:N──> AuditLog
```

| Encja | Tabela | Kluczowe pola |
|---|---|---|
| `Product` | `products` | id, name, sku (unique), description, unit, quantity, price, minQuantity, locationId |
| `StockMovement` | `stock_movements` | id, type (PRZYJECIE/WYDANIE/KOREKTA), quantity, note, createdBy, batchId, product_id |
| `Batch` | `batches` | id, lotNumber, expiryDate, manufacturingDate, quantity, product_id, locationId |
| `Location` | `locations` | id, code, name, type (WAREHOUSE/RACK/SHELF/BIN), parentId |
| `Contractor` | `contractors` | id, name, taxId (unique), type (SUPPLIER/CUSTOMER), active |
| `WarehouseDocument` | `warehouse_documents` | id, number (unique), type (PZ/WZ), status (DRAFT/CONFIRMED/CANCELLED), contractor_id |
| `WarehouseDocumentItem` | `warehouse_document_items` | id, quantity, unitPrice, lotNumber, expiryDate, product_id, document_id |
| `StockReservation` | `stock_reservations` | id, quantity, status (ACTIVE/RELEASED/FULFILLED), expiresAt, product_id |
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
| POST | `/api/locations` | Utworzenie lokalizacji | ADMIN |
| PUT | `/api/locations/{id}` | Aktualizacja lokalizacji | ADMIN |
| DELETE | `/api/locations/{id}` | Usunięcie lokalizacji (sprawdza dzieci) | ADMIN |

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

---

## Frontend

Frontend to SPA napisane w React 19 + TypeScript 6.0, budowane przez Vite i serwowane jako statyczne zasoby wbudowane w JAR.

### Widoki (zakładki)

| Zakładka | Komponent | Opis |
|---|---|---|
| **Products** | `ProductTable` | Tabela produktów z paginacją, wyszukiwaniem, sortowaniem. Dla każdego produktu: stan, najbliższa data ważności partii, lokalizacja. Formularz dodawania/edycji. |
| **Contractors** | `ContractorTable` | Lista kontrahentów z wyszukiwaniem. Formularz dodawania/edycji. |
| **Documents (PZ/WZ)** | `DocumentList` | Lista dokumentów z filtrowaniem po typie i statusie. Formularz tworzenia z dynamicznymi pozycjami. Modal szczegółów dokumentu. |
| **Locations** | `LocationPanel` + `LocationTree` | Drzewo lokalizacji. Formularz dodawania/edycji. |
| **Reservations** | `ReservationPanel` | Lista rezerwacji z filtrami. Tworzenie i zwalnianie rezerwacji. |
| **Audit** (tylko ADMIN) | `AuditLogPanel` | Dziennik audytu z filtrowaniem po użytkowniku i akcji. |

### Hooks (stan)

| Hook | Zarządza |
|---|---|
| `useAuth` | Stan autoryzacji, login/logout, przechowywanie JWT w localStorage |
| `useProducts` | Lista produktów, paginacja, wyszukiwanie (debounced), CRUD |
| `useContractors` | Lista kontrahentów, wyszukiwanie, CRUD |
| `useDocuments` | Lista dokumentów (filtry typ/status), paginacja, CRUD, potwierdzanie/anulowanie |
| `useReservations` | Lista rezerwacji, filtry, tworzenie/zwalnianie |
| `useNotification` | Komunikaty toast |

---

## Role i uprawnienia

| Rola | Uprawnienia |
|---|---|
| `ROLE_ADMIN` | Pełny dostęp: CRUD produktów, dokumenty PZ/WZ (tworzenie, potwierdzanie, anulowanie), rezerwacje, kontrahenci, lokalizacje, import/eksport, dziennik audytu, seed danych, zarządzanie użytkownikami (rejestracja) |
| `ROLE_USER` | Podgląd: produkty, stany magazynowe, dokumenty, kontrahenci, lokalizacje, rezerwacje, statystyki, eksport. Może dodawać tylko ruchy PRZYJECIE. |

Nowi użytkownicy rejestrowani są z rolą `ROLE_USER`. Nadanie roli `ROLE_ADMIN` wymaga ręcznej zmiany w bazie danych.

---

## Zmienne środowiskowe

| Zmienna | Opis | Wymagane |
|---|---|---|
| `DB_URL` | URL połączenia z PostgreSQL (`jdbc:postgresql://postgres:5432/magazyn_db`) | Tak |
| `DB_USERNAME` | Użytkownik bazy danych | Tak |
| `DB_PASSWORD` | Hasło użytkownika bazy | Tak |
| `JWT_SECRET` | Klucz do podpisu JWT (min. 32 znaki, Base64) | Tak |
| `JWT_EXPIRATION` | Czas ważności tokena (ms, domyślnie 86400000 = 24h) | Nie |
| `APP_NOTIFICATIONS_ENABLED` | Włączenie powiadomień e-mail (domyślnie true) | Nie |
| `SPRING_MAIL_HOST` | Host SMTP | Gdy notifications włączone |
| `SPRING_MAIL_PORT` | Port SMTP (587) | Gdy notifications włączone |
| `SPRING_MAIL_USERNAME` | Użytkownik SMTP | Gdy notifications włączone |
| `SPRING_MAIL_PASSWORD` | Hasło SMTP | Gdy notifications włączone |

### Przykładowy plik `.env`

```env
DB_URL=jdbc:postgresql://postgres:5432/magazyn_db
DB_USERNAME=magazyn_user
DB_PASSWORD=zmien_haslo
JWT_SECRET=zmien_na_wlasny_klucz_o_dlugosci_co_najmniej_32_znakow
JWT_EXPIRATION=86400000
```

Plik `.env` znajduje się w `.gitignore`. W repozytorium dostępny jest szablon `.env.example`.

---

## Uruchomienie lokalne

### Wymagania

- **Java 25 JDK (LTS)** — [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=25)
- **Maven 3.8+** — lub użyj dołączonego `mvnw` (Maven Wrapper)
- **Docker Desktop** — do PostgreSQL (lub lokalna instalacja PostgreSQL 16)

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
  postgres:      # PostgreSQL 16, port 5432, volume postgres_data, healthcheck
  app:           # Spring Boot, port 8080, zależny od postgres (healthy)
  backup:        # PostgreSQL 16, uruchamia pg_dump w pętli 24h
```

### Dockerfile (multi-stage)

- **Build:** `maven:3.9-eclipse-temurin-25` — kompiluje projekt
- **Runtime:** `eclipse-temurin:25-jre` — uruchamia JAR

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
│   ├── config/                        # Security, filtry JWT/RateLimit/AuditLog, OpenAPI
│   ├── controller/                    # REST API (12 kontrolerów)
│   ├── dto/                           # Data Transfer Objects
│   ├── entity/                        # Encje JPA + enums
│   ├── exception/                     # GlobalExceptionHandler + custom exceptions
│   ├── repository/                    # Spring Data JPA (14 repozytoriów)
│   ├── service/                       # Logika biznesowa (16 serwisów)
│   └── util/                          # JwtUtil, AuditContext
│
├── src/main/resources/
│   ├── application.properties         # Konfiguracja (env-based)
│   └── static/                        # Frontend bundle (index.html, assets/)
│
├── src/test/java/com/example/magazyn/
│   ├── service/                       # Testy jednostkowe (Mockito)
│   ├── integration/                   # Testy integracyjne (WebTestClient)
│   └── MagazynApplicationTests.java
│
├── magazyn-frontend/                  # Frontend React + TypeScript
│   ├── src/
│   │   ├── components/                # Komponenty React (18 plików)
│   │   ├── hooks/                     # Niestandardowe hooki (5)
│   │   ├── services/                  # Klient API (Axios)
│   │   ├── types/                     # Interfejsy TypeScript
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
