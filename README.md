# Magazyn — Backend REST API

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)](https://adoptium.net/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-jjwt%200.12.6-000000?logo=jsonwebtokens)](https://github.com/jwtk/jjwt)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088FF?logo=githubactions)](https://github.com/features/actions)

Backend REST API systemu zarządzania magazynem. Udostępnia w pełni funkcjonalne endpointy do rejestracji i logowania użytkowników (JWT), CRUD produktów z paginacją oraz zarządzania stanami magazynowymi (przyjęcia, wydania, korekty) z kontrolą dostępu opartą na rolach (RBAC).

**Frontend:** [magazyn-frontend.vercel.app](https://magazyn-frontend.vercel.app)  
**API (produkcja):** `https://magazyn.kzelman.pl/api`

---

## Stack technologiczny

| Technologia | Wersja | Zastosowanie |
|---|---|---|
| Java | 17 (LTS) | Język programowania |
| Spring Boot | 3.5.14 | Framework aplikacyjny, osadzony Tomcat |
| Spring Data JPA / Hibernate | — | ORM, automatyczne DDL (ddl-auto=update) |
| Spring Security | 6.5.x | Autoryzacja, uwierzytelnianie, BCrypt, @EnableMethodSecurity |
| Spring Validation | — | Walidacja adnotacjami (@NotBlank, @Size, @Positive) |
| jjwt (io.jsonwebtoken) | 0.12.6 | Generowanie i weryfikacja tokenów JWT |
| PostgreSQL | 16 | Relacyjna baza danych |
| Lombok | — | Redukcja boilerplate (@Data, @Builder, @NoArgsConstructor) |
| Maven | 3.9+ | Build i zarządzanie zależnościami |
| Docker Compose | — | Lokalne uruchomienie PostgreSQL |

---

## Architektura

### Warstwy aplikacji

```
Controller (@RestController) → Service (@Service) → Repository (JpaRepository) → PostgreSQL
                                   ↑
                              DTO (Request/Response)
                                   ↑
Security: JwtAuthenticationFilter → SecurityConfig (@EnableMethodSecurity)
```

**Zasada:** Kontrolery przyjmują i zwracają DTO, nie encje. Logika biznesowa w serwisach. Repozytoria operują bezpośrednio na encjach. Dostęp do chronionych zasobów odbywa się przez token JWT umieszczony w nagłówku `Authorization: Bearer <token>`.

### Autoryzacja

- Każde żądanie (oprócz `/api/auth/**`) przechodzi przez `JwtAuthenticationFilter`
- Filtr wyciąga token z nagłówka, waliduje go przez `JwtUtil`, ustawia kontekst bezpieczeństwa
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
| POST | `/api/auth/register` | Rejestracja nowego użytkownika | Publiczny |
| POST | `/api/auth/login` | Logowanie, zwraca token JWT | Publiczny |

**Register:** Wymaga `username` (3–50 znaków) i `password` (6–100 znaków). Tworzy użytkownika z rolą `ROLE_USER`. Sprawdza unikalność nazwy użytkownika.

**Login:** Przyjmuje `username` i `password`, autoryzuje przez `AuthenticationManager`, zwraca `{ token, username, role }`.

---

### Produkty — `/api/products`

| Metoda | Ścieżka | Opis | Dostęp |
|---|---|---|---|
| GET | `/api/products?page=0&size=10&sort=name` | Lista produktów z paginacją | Zalogowany |
| GET | `/api/products/{id}` | Produkt po ID | Zalogowany |
| GET | `/api/products/sku/{sku}` | Produkt po SKU | Zalogowany |
| POST | `/api/products` | Utworzenie produktu | ADMIN |
| PUT | `/api/products/{id}` | Aktualizacja produktu (częściowa) | ADMIN |
| DELETE | `/api/products/{id}` | Usunięcie produktu | ADMIN |

**Dane wejściowe (POST/PUT):** `name` (wymagane), `sku` (wymagane, unikalne), `description`, `unit` (wymagane).  
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

## Role i uprawnienia

| Rola | Uprawnienia |
|---|---|
| `ROLE_ADMIN` | Pełny CRUD produktów, wszystkie typy ruchów magazynowych (PRZYJECIE, WYDANIE, KOREKTA) |
| `ROLE_USER` | Podgląd produktów i stanów magazynowych, możliwość dodawania tylko ruchów PRZYJECIE |

Nowi użytkownicy rejestrowani są zawsze z rolą `ROLE_USER`. Nadanie roli `ROLE_ADMIN` wymaga ręcznej zmiany w bazie danych.

---

## Zmienne środowiskowe

Wszystkie wrażliwe wartości konfiguracyjne są externalizowane przez `application.properties` i wczytywane ze zmiennych środowiskowych.

| Zmienna | Opis | Przykład |
|---|---|---|
| `DB_URL` | URL połączenia z PostgreSQL | `jdbc:postgresql://localhost:5432/magazyn_db` |
| `DB_USERNAME` | Użytkownik bazy danych | `admin` |
| `DB_PASSWORD` | Hasło użytkownika bazy | `REMOVED` |
| `JWT_SECRET` | Klucz do podpisu tokenów JWT (min. 32 znaki) | `kF8pL2mN4qR7sT5v...` |
| `JWT_EXPIRATION` | Czas ważności tokena (ms) | `86400000` (24h) |

### Przykładowy plik `.env`

```env
DB_URL=jdbc:postgresql://localhost:5432/magazyn_db
DB_USERNAME=admin
DB_PASSWORD=REMOVED
JWT_SECRET=zmien_na_wlasny_klucz_o_dlugosci_co_najmniej_32_znakow
JWT_EXPIRATION=86400000
```

Plik `.env` znajduje się w `.gitignore` i nie podlega wersjonowaniu. W repozytorium dostępny jest szablon `.env.example`.

---

## Wymagania lokalne

- **Java 17 JDK** — [Adoptium Temurin](https://adoptium.net/temurin/releases/?version=17)
- **Maven 3.8+** — lub użyj dołączonego `mvnw` (Maven Wrapper)
- **PostgreSQL 16** — zalecane uruchomienie przez Docker Compose
- **Docker Desktop** — opcjonalnie, do PostgreSQL

---

## Uruchomienie lokalne

```bash
# 1. Sklonuj repozytorium
git clone https://github.com/krzysztofzelman/magazyn-app.git
cd magazyn-app/magazyn

# 2. Skopiuj plik zmiennych środowiskowych i dostosuj
cp .env.example .env

# 3. Uruchom PostgreSQL przez Dockera
docker compose up -d

# 4. Uruchom aplikację (Maven Wrapper pobierze odpowiednią wersję Mavena)
./mvnw spring-boot:run
```

Aplikacja startuje na **http://localhost:8080**.

### Budowanie pliku JAR

```bash
./mvnw clean package -DskipTests
java -jar target/magazyn-0.0.1-SNAPSHOT.jar
```

### Uruchomienie testów

```bash
./mvnw test
```

---

## CI/CD

Wdrożenie produkcyjne odbywa się automatycznie przez **GitHub Actions** przy każdym pushu na gałąź `main`.

**Plik:** `.github/workflows/deploy.yml`

**Przebieg:**
1. Trigger: push do `main`
2. Runner: `ubuntu-latest`
3. Akcja: `appleboy/ssh-action` łączy się przez SSH z VPS (`REMOVED`, port `2022`)
4. Skrypt na VPS:
   ```bash
   cd /var/www/magazyn-app
   git pull
   mvn clean package -DskipTests
   systemctl restart magazyn
   ```

**Wymagane Secret w repozytorium GitHub:**
- `VPS_SSH_KEY` — klucz prywatny SSH do połączenia z VPS

Aplikacja na produkcji działa jako usługa **systemd** (`magazyn.service`), co zapewnia automatyczne restarty przy awariach i starcie systemu.

---

## Struktura projektu

```
magazyn/
├── .github/workflows/
│   └── deploy.yml                    # CI/CD — GitHub Actions → VPS
├── src/
│   ├── main/
│   │   ├── java/com/example/magazyn/
│   │   │   ├── MagazynApplication.java         # Entry point @SpringBootApplication
│   │   │   ├── auth/                           # Autoryzacja
│   │   │   │   ├── AuthController.java         #   Endpointy /api/auth/**
│   │   │   │   ├── AuthService.java            #   Logika rejestracji/logowania
│   │   │   │   ├── AuthResponse.java           #   DTO: token, username, role
│   │   │   │   ├── LoginRequest.java           #   DTO: username, password
│   │   │   │   └── RegisterRequest.java        #   DTO: username, password
│   │   │   ├── config/                         # Konfiguracja
│   │   │   │   ├── CorsConfig.java             #   CORS dla frontendu
│   │   │   │   ├── JwtAuthenticationFilter.java#   Filtr JWT (OncePerRequestFilter)
│   │   │   │   └── SecurityConfig.java         #   Spring Security, BCrypt, @EnableMethodSecurity
│   │   │   ├── controller/                     # REST API
│   │   │   │   ├── ProductController.java      #   CRUD produktów
│   │   │   │   └── StockController.java        #   Ruchy i stan magazynowy
│   │   │   ├── dto/                            # Data Transfer Objects
│   │   │   │   ├── CreateProductRequest.java
│   │   │   │   ├── UpdateProductRequest.java
│   │   │   │   ├── ProductResponse.java
│   │   │   │   ├── StockMovementRequest.java
│   │   │   │   ├── StockMovementResponse.java
│   │   │   │   └── StockResponse.java
│   │   │   ├── entity/                         # Encje JPA
│   │   │   │   ├── Product.java                #   Produkt
│   │   │   │   ├── StockMovement.java          #   Ruch magazynowy
│   │   │   │   ├── User.java                   #   Użytkownik
│   │   │   │   └── MovementType.java           #   Enum: PRZYJECIE/WYDANIE/KOREKTA
│   │   │   ├── exception/
│   │   │   │   └── GlobalExceptionHandler.java # @ControllerAdvice — JSON + status HTTP
│   │   │   ├── repository/                     # Spring Data JPA
│   │   │   │   ├── ProductRepository.java
│   │   │   │   ├── StockMovementRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── service/                        # Logika biznesowa
│   │   │   │   ├── ProductService.java         #   Operacje na produktach
│   │   │   │   ├── StockService.java           #   Ruchy magazynowe
│   │   │   │   └── CustomUserDetailsService.java#  UserDetailsService
│   │   │   └── util/
│   │   │       └── JwtUtil.java                # Generowanie/walidacja JWT
│   │   └── resources/
│   │       └── application.properties          # Konfiguracja (zmienne env)
│   └── test/
│       └── java/com/example/magazyn/
│           ├── MagazynApplicationTests.java
│           └── service/
│               └── ProductServiceTest.java     # Testy jednostkowe
├── docker-compose.yml                          # PostgreSQL 16 dla dewelopmentu
├── .env.example                                # Szablon zmiennych środowiskowych
├── pom.xml                                     # Zależności Maven
├── mvnw / mvnw.cmd                             # Maven Wrapper
└── README.md                                   # Niniejszy plik
```
