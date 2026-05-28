# Magazyn — Backend REST API

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk)](https://adoptium.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-jjwt%200.13.0-000000?logo=jsonwebtokens)](https://github.com/jwtk/jjwt)
[![Docker](https://img.shields.io/badge/Docker-29.5.2-2496ED?logo=docker)](https://www.docker.com/)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=githubactions)](https://github.com/features/actions)

Backend REST API systemu zarządzania magazynem. Udostępnia w pełni funkcjonalne endpointy do rejestracji i logowania użytkowników (JWT), CRUD produktów z paginacją, zarządzania stanami magazynowymi (przyjęcia, wydania, korekty), statystyk dashboardu oraz eksportu danych do CSV/Excel. Kontrola dostępu oparta na rolach (RBAC).

**Frontend:** `https://magazyn-frontend.vercel.app`

**API (produkcja):** `https://magazyn.kzelman.pl/api`

**Swagger UI:** `https://magazyn.kzelman.pl/swagger-ui/index.html`

---

## Stack technologiczny

| Technologia | Wersja | Zastosowanie |
|---|---|---|
| Java | 25 (LTS) | Język programowania |
| Spring Boot | 4.0.6 | Framework aplikacyjny, osadzony Tomcat 11 |
| Spring Data JPA / Hibernate | — | ORM, automatyczne DDL (ddl-auto=update) |
| Spring Security | 7.x | Autoryzacja, uwierzytelnianie, BCrypt, @EnableMethodSecurity |
| Spring Validation | — | Walidacja adnotacjami (@NotBlank, @Size, @Positive) |
| jjwt (io.jsonwebtoken) | 0.13.0 | Generowanie i weryfikacja tokenów JWT |
| PostgreSQL | 18 | Relacyjna baza danych |
| Lombok | — | Redukcja boilerplate (@Data, @Builder, @NoArgsConstructor) |
| Maven | 3.9+ | Build i zarządzanie zależnościami |
| Apache POI | 5.4.0 | Generowanie plików Excel (.xlsx) |
| Bucket4j | 8.19.0 | Rate limiting dla endpointu logowania |
| Springdoc OpenAPI | 2.7.0 | Swagger UI / OpenAPI docs |
| Docker + Docker Compose | — | Budowa i uruchomienie w kontenerach |

---

## Architektura

### Warstwy aplikacji

```
Controller (@RestController) → Service (@Service) → Repository (JpaRepository) → PostgreSQL
                                   ↑
                              DTO (Request/Response)
                                   ↑
Security: JwtAuthenticationFilter → SecurityConfig (@EnableMethodSecurity)
                                   ↑
                          RateLimitFilter (Bucket4j)
```

**Zasada:** Kontrolery przyjmują i zwracają DTO, nie encje. Logika biznesowa w serwisach. Repozytoria operują bezpośrednio na encjach. Dostęp do chronionych zasobów odbywa się przez token JWT umieszczony w nagłówku `Authorization: Bearer <token>`.

### Autoryzacja

- Każde żądanie (oprócz `/api/auth/login`) przechodzi przez `JwtAuthenticationFilter`
- Filtr wyciąga token z nagłówka, waliduje go przez `JwtUtil`, ustawia kontekst bezpieczeństwa
- Endpoint `/api/auth/login` chroniony jest dodatkowo przez `RateLimitFilter` (20 żądań/minutę na IP)
- Role sprawdzane są na poziomie metod przez `@PreAuthorize`:

```java
@PreAuthorize("hasRole('ADMIN')")
@PreAuthorize("isAuthenticated()")
@PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #request.type.name() == 'PRZYJECIE')")
```

---

## Endpointy REST

### Autoryzacja — `/api/auth`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| POST | `/api/auth/register` | Rejestracja nowego użytkownika | Wymaga tokena |
| POST | `/api/auth/login` | Logowanie, zwraca token JWT | Publiczny (rate limit: 20/min/IP) |

**Register:** Wymaga `username` (3–50 znaków) i `password` (6–100 znaków). Tworzy użytkownika z rolą `ROLE_USER`.

**Login:** Przyjmuje `username` i `password`, autoryzuje przez `AuthenticationManager`, zwraca `{ token, username, role }`.

---

### Produkty — `/api/products`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/products?page=0&size=10&sort=name&search=` | Lista produktów z paginacją i wyszukiwaniem | Zalogowany |
| GET | `/api/products/{id}` | Produkt po ID | Zalogowany |
| GET | `/api/products/sku/{sku}` | Produkt po SKU | Zalogowany |
| POST | `/api/products` | Utworzenie produktu | ADMIN |
| PUT | `/api/products/{id}` | Aktualizacja produktu (częściowa) | ADMIN |
| DELETE | `/api/products/{id}` | Usunięcie produktu | ADMIN |

**Dane wejściowe (POST/PUT):** `name` (wymagane), `sku` (wymagane, unikalne), `description`, `unit` (wymagane), `price`, `minQuantity`.
**Walidacja:** Brak wymaganych pól → 400. Duplikat SKU → 400. Nieistniejące ID → 404.
**Paginacja:** GET `/api/products` zwraca `Page<ProductResponse>` z `content`, `totalPages`, `totalElements`, `number`.

---

### Stan magazynowy — `/api/stock`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/stock/{productId}` | Aktualny stan magazynowy produktu | Zalogowany |
| GET | `/api/stock/{productId}/movements` | Historia ruchów magazynowych (DESC) | Zalogowany |
| POST | `/api/stock/{productId}/movement` | Dodanie ruchu magazynowego | ADMIN / USER (tylko PRZYJECIE) |

**Typy ruchu:**
- `PRZYJECIE` — dodaje podaną ilość do stanu produktu
- `WYDANIE` — odejmuje od stanu (sprawdza dostępność; brak → 400)
- `KOREKTA` — ustawia stan dokładnie na podaną wartość

**Dane wejściowe (POST):** `type` (enum, wymagane), `quantity` (Integer > 0, wymagane), `note` (opcjonalne).
Endpoint zapisuje również nazwę użytkownika wykonującego ruch (pobraną z tokena JWT).

---

### Dashboard — `/api/stats`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/stats/dashboard` | Statystyki: liczba produktów, wartość stanu, top sprzedawane, alerty o niskich stanach | Zalogowany |

---

### Eksport danych — `/api/products/export/csv` i `/api/stock/export/excel`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/products/export/csv` | Eksport produktów do CSV lub XLSX | Zalogowany |
| GET | `/api/stock/export/excel` | Eksport stanu magazynowego do XLSX lub CSV | Zalogowany |

**Parametry:**

| Parametr | Domyślnie | Opis |
|---|---|---|
| `format` | `csv` (produkty) / `xlsx` (stan) | Format pliku: `csv` lub `xlsx` |
| `fields` | wszystkie | Filtrowane pola, np. `name,price,quantity` |

**Dostępne pola — produkty:** `id`, `name`, `sku`, `description`, `unit`, `quantity`, `price`, `minQuantity`, `createdAt`

**Dostępne pola — stan:** `productId`, `productName`, `sku`, `unit`, `quantity`, `price`, `stockValue`, `minQuantity`

**Nazwa pliku:** zawiera datę w formacie `yyyy-MM-dd`, np. `products-2026-05-26.csv` lub `stock-2026-05-26.xlsx`.

---

## Role i uprawnienia

| Rola | Uprawnienia |
|---|---|
| `ROLE_ADMIN` | Pełny CRUD produktów, wszystkie typy ruchów magazynowych (PRZYJECIE, WYDANIE, KOREKTA), podgląd statystyk i eksport |
| `ROLE_USER` | Podgląd produktów i stanów magazynowych, statystyki, eksport, możliwość dodawania tylko ruchów PRZYJECIE |

Nowi użytkownicy rejestrowani są zawsze z rolą `ROLE_USER`. Nadanie roli `ROLE_ADMIN` wymaga ręcznej zmiany w bazie danych.

---

## Zmienne środowiskowe

Wszystkie wrażliwe wartości konfiguracyjne są externalizowane przez `application.properties` i wczytywane ze zmiennych środowiskowych.

| Zmienna | Opis | Przykład |
|---|---|---|
| `DB_URL` | URL połączenia z PostgreSQL | `jdbc:postgresql://localhost:5432/magazyn_db` |
| `DB_USERNAME` | Użytkownik bazy danych | `magazyn_user` |
| `DB_PASSWORD` | Hasło użytkownika bazy | `bezpieczne_haslo` |
| `JWT_SECRET` | Klucz do podpisu tokenów JWT (min. 32 znaki) | `kF8pL2mN4qR7sT5v...` |
| `JWT_EXPIRATION` | Czas ważności tokena (ms) | `86400000` (24h) |

### Przykładowy plik `.env`

```env
DB_URL=jdbc:postgresql://localhost:5432/magazyn_db
DB_USERNAME=magazyn_user
DB_PASSWORD=zmien_haslo
JWT_SECRET=zmien_na_wlasny_klucz_o_dlugosci_co_najmniej_32_znakow
JWT_EXPIRATION=86400000
```

Plik `.env` znajduje się w `.gitignore` i nie podlega wersjonowaniu. W repozytorium dostępny jest szablon `.env.example`.

---

## Uruchomienie lokalne

### Wymagania

- **Java 25 JDK** — [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=25)
- **Maven 3.8+** — lub użyj dołączonego `mvnw` (Maven Wrapper)
- **Docker Desktop** — do PostgreSQL (lub lokalna instalacja PostgreSQL 16)

### Krok po kroku

```bash
# 1. Sklonuj repozytorium
git clone https://github.com/krzysztofzelman/magazyn-app.git
cd magazyn-app

# 2. Skopiuj plik zmiennych środowiskowych i dostosuj
cp .env.example .env

# 3. Uruchom PostgreSQL przez Dockera
docker compose up -d postgres

# 4. Uruchom aplikację (Maven Wrapper pobierze odpowiednią wersję Mavena)
./mvnw spring-boot:run
```

Aplikacja startuje na **http://localhost:8080**.

### Budowanie pliku JAR

```bash
./mvnw clean package -DskipTests
java -jar target/magazyn-0.0.1-SNAPSHOT.jar
```

### Uruchomienie przez Docker Compose (pełny stack)

```bash
# Buduje obraz i uruchamia app + postgres
docker compose up -d --build
```

---

## CI/CD

Wdrożenie produkcyjne odbywa się automatycznie przez **GitHub Actions** przy każdym pushu na gałąź `main`.

**Plik:** `.github/workflows/deploy.yml`

**Przebieg:**
1. Trigger: push do `main`
2. Runner: `ubuntu-latest`
3. Akcja: `appleboy/ssh-action` łączy się przez SSH z VPS (`REMOVED`, port `2022`)
4. Skrypt na VPS wykonuje:
   ```bash
   cd /root/magazyn-app
   git pull origin main
   docker compose down
   docker compose up -d --build
   docker image prune -f
   ```

**Wymagane Secret w repozytorium GitHub:**
- `VPS_SSH_KEY` — klucz prywatny SSH do połączenia z VPS

Aplikacja na produkcji działa w kontenerze Docker z automatycznym restartem (`restart: unless-stopped`).

---

## Nginx — reverse proxy

Na VPS działa Nginx jako reverse proxy, które:
- Terminuje SSL (certyfikat Let's Encrypt dla `magazyn.kzelman.pl`)
- Proxy na `localhost:8080` (port aplikacji w kontenerze)
- Dodaje nagłówki CORS

---

## Struktura projektu

```
magazyn-app/
├── .github/workflows/
│   └── deploy.yml                        # CI/CD — GitHub Actions → VPS (Docker)
├── src/
│   ├── main/
│   │   ├── java/com/example/magazyn/
│   │   │   ├── MagazynApplication.java   # Entry point @SpringBootApplication
│   │   │   ├── auth/                     # Autoryzacja
│   │   │   │   ├── AuthController.java   #   Endpointy /api/auth/**
│   │   │   │   ├── AuthService.java      #   Logika rejestracji/logowania
│   │   │   │   ├── AuthResponse.java     #   DTO: token, username, role
│   │   │   │   ├── LoginRequest.java     #   DTO: username, password
│   │   │   │   └── RegisterRequest.java  #   DTO: username, password
│   │   │   ├── config/                   # Konfiguracja
│   │   │   │   ├── CorsConfig.java       #   CORS dla frontendu
│   │   │   │   ├── JwtAuthenticationFilter.java # Filtr JWT
│   │   │   │   ├── OpenApiConfig.java    #   Swagger UI z BearerAuth
│   │   │   │   ├── RateLimitFilter.java  #   Rate limiting (Bucket4j)
│   │   │   │   └── SecurityConfig.java   #   Spring Security, BCrypt
│   │   │   ├── controller/               # REST API
│   │   │   │   ├── ExportController.java #   Eksport CSV/Excel
│   │   │   │   ├── ProductController.java#   CRUD produktów
│   │   │   │   ├── StatsController.java  #   Statystyki dashboardu
│   │   │   │   └── StockController.java  #   Ruchy i stan magazynowy
│   │   │   ├── dto/                      # Data Transfer Objects
│   │   │   │   ├── CreateProductRequest.java
│   │   │   │   ├── UpdateProductRequest.java
│   │   │   │   ├── ProductResponse.java
│   │   │   │   ├── StockMovementRequest.java
│   │   │   │   ├── StockMovementResponse.java
│   │   │   │   ├── StockResponse.java
│   │   │   │   └── StatsDashboardResponse.java
│   │   │   ├── entity/                   # Encje JPA
│   │   │   │   ├── MovementType.java     #   Enum: PRZYJECIE/WYDANIE/KOREKTA
│   │   │   │   ├── Product.java          #   Produkt
│   │   │   │   ├── StockMovement.java    #   Ruch magazynowy
│   │   │   │   └── User.java             #   Użytkownik
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java # @ControllerAdvice
│   │   │   ├── repository/               # Spring Data JPA
│   │   │   │   ├── ProductRepository.java
│   │   │   │   ├── StockMovementRepository.java
│   │   │   │   ├── TopSellingProjection.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── service/                  # Logika biznesowa
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── ExportService.java    #   Generowanie CSV/Excel
│   │   │   │   ├── ProductService.java   #   Operacje na produktach
│   │   │   │   ├── StatsService.java     #   Statystyki dashboardu
│   │   │   │   └── StockService.java     #   Ruchy magazynowe
│   │   │   └── util/
│   │   │       └── JwtUtil.java          # Generowanie/walidacja JWT
│   │   └── resources/
│   │       └── application.properties    # Konfiguracja (zmienne env)
│   └── test/
│       └── java/com/example/magazyn/
│           ├── MagazynApplicationTests.java
│           └── service/
│               └── ProductServiceTest.java  # Testy jednostkowe
├── .dockerignore                          # Pliki ignorowane przez Docker
├── .env.example                           # Szablon zmiennych środowiskowych
├── .gitignore                             # Ignorowane pliki
├── Dockerfile                             # Multi-stage build (Maven + JRE)
├── docker-compose.yml                     # PostgreSQL + aplikacja
├── pom.xml                                # Zależności Maven
├── mvnw / mvnw.cmd                        # Maven Wrapper
└── README.md                              # Niniejszy plik
```
