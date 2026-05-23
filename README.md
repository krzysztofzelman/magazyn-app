# Magazyn — Backend REST API

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apache-maven)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![JWT](https://img.shields.io/badge/JWT-jjwt%200.12.6-000000?logo=jsonwebtokens)](https://github.com/jwtk/jjwt)
[![Docker](https://img.shields.io/badge/Docker-compose-2496ED?logo=docker)](https://www.docker.com/)

REST API systemu zarządzania magazynem — autoryzacja JWT, pełny CRUD produktów, stany i ruchy magazynowe. Zbudowane na **Spring Boot 3.5** z **Java 17** i bazą **PostgreSQL 16**.

🌐 **Backend live:** [magazyn.kzelman.pl](https://magazyn.kzelman.pl)  
🖥️ **Frontend:** [magazyn-frontend.vercel.app](https://magazyn-frontend.vercel.app)  
📦 **Repozytorium:** [github.com/krzysztofzelman/magazyn-app](https://github.com/krzysztofzelman/magazyn-app)

---

## Spis treści

- [Tech Stack](#tech-stack)
- [Model danych](#model-danych)
- [Endpointy API](#endpointy-api)
  - [Auth](#-autoryzacja-auth)
  - [Produkty](#-produkty-products)
  - [Stan magazynowy](#-stan-magazynowy-stock)
- [Zmienne środowiskowe](#zmienne-środowiskowe)
- [Uruchomienie lokalne](#uruchomienie-lokalne)
- [Deploy na VPS](#deploy-na-vps)
- [CORS](#cors)
- [Struktura projektu](#struktura-projektu)

---

## Tech Stack

| Technologia | Wersja | Zastosowanie |
|---|---|---|
| **Java** | 17 (LTS) | Język programowania |
| **Spring Boot** | 3.5.14 | Framework aplikacyjny |
| **Spring Data JPA / Hibernate** | — | ORM, automatyczne DDL |
| **Spring Web** | — | REST API, osadzony Tomcat |
| **Spring Security** | — | Autoryzacja JWT, BCrypt |
| **Spring Validation** | — | Walidacja `@Valid`, `@NotBlank` itd. |
| **jjwt** | 0.12.6 | Generowanie i walidacja tokenów JWT |
| **PostgreSQL** | 16 | Baza danych (produkcja i lokalnie przez Docker) |
| **Hibernate** | — | Dialekt PostgreSQL, `ddl-auto=update` |
| **Lombok** | — | Adnotacje — eliminacja boilerplate |
| **Maven** | 3.9+ | Build i zarządzanie zależnościami |
| **Docker Compose** | — | Konteneryzacja bazy PostgreSQL |

---

## Model danych

### `User` — użytkownik

| Pole | Typ | Ograniczenia |
|---|---|---|
| `id` | `Long` | PK, autoinkrement |
| `username` | `String` | UNIQUE, NOT NULL |
| `password` | `String` | NOT NULL (BCrypt) |
| `role` | `String` | NOT NULL (np. `ROLE_USER`) |

### `Product` — produkt

| Pole | Typ | Ograniczenia |
|---|---|---|
| `id` | `Long` | PK, autoinkrement |
| `name` | `String` | NOT NULL |
| `sku` | `String` | UNIQUE, NOT NULL |
| `description` | `String` | — |
| `unit` | `String` | NOT NULL (np. `szt.`, `kg`, `m`, `opak.`) |
| `quantity` | `Integer` | DEFAULT 0 |
| `createdAt` | `LocalDateTime` | Auto-set @CreationTimestamp |

### `StockMovement` — ruch magazynowy

| Pole | Typ | Ograniczenia |
|---|---|---|
| `id` | `Long` | PK, autoinkrement |
| `product` | `Product` | @ManyToOne(LAZY), NOT NULL |
| `type` | `MovementType` (enum) | `PRZYJECIE` / `WYDANIE` / `KOREKTA` |
| `quantity` | `Integer` | NOT NULL |
| `note` | `String` | — |
| `createdAt` | `LocalDateTime` | Auto-set |
| `createdBy` | `String` | NOT NULL (username) |

---

## Endpointy API

**Base URL:** `https://magazyn.kzelman.pl/api` (produkcja) lub `http://localhost:8080/api` (lokalnie)

---

### 🔐 Autoryzacja (`/api/auth`)

Endpointy **publiczne** — nie wymagają tokena JWT.

#### `POST /api/auth/register` — rejestracja

```bash
curl -X POST https://magazyn.kzelman.pl/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"jan","password":"haslo123"}'
```

**Odpowiedź** `201 Created`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "jan",
  "role": "ROLE_USER"
}
```

**Odpowiedź** `400 Bad Request`:
```json
{ "error": "Nazwa użytkownika jest już zajęta" }
```

#### `POST /api/auth/login` — logowanie

```bash
curl -X POST https://magazyn.kzelman.pl/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jan","password":"haslo123"}'
```

**Odpowiedź** `200 OK`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "jan",
  "role": "ROLE_USER"
}
```

**Odpowiedź** `400 Bad Request`:
```json
{ "error": "Nieprawidłowa nazwa użytkownika lub hasło" }
```

---

### 📦 Produkty (`/api/products`)

Wszystkie endpointy wymagają nagłówka `Authorization: Bearer <token>`.

#### `GET /api/products` — lista wszystkich produktów

```bash
curl -H "Authorization: Bearer <token>" https://magazyn.kzelman.pl/api/products
```

**Odpowiedź** `200 OK`:
```json
[
  {
    "id": 1,
    "name": "Śruba M8",
    "sku": "SRU-M8-001",
    "description": "Śruba nierdzewna M8 x 30mm",
    "unit": "szt.",
    "quantity": 100,
    "createdAt": "2026-05-22T12:00:00"
  }
]
```

#### `GET /api/products/{id}` — produkt po ID

```bash
curl -H "Authorization: Bearer <token>" https://magazyn.kzelman.pl/api/products/1
```

**Odpowiedź** `200 OK` / `404 Not Found`

#### `GET /api/products/sku/{sku}` — produkt po SKU

```bash
curl -H "Authorization: Bearer <token>" https://magazyn.kzelman.pl/api/products/sku/SRU-M8-001
```

**Odpowiedź** `200 OK` / `404 Not Found`

#### `POST /api/products` — utworzenie produktu

```bash
curl -X POST https://magazyn.kzelman.pl/api/products \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Śruba M8","sku":"SRU-M8-001","description":"Śruba nierdzewna M8 x 30mm","unit":"szt."}'
```

**Odpowiedź** `201 Created` / `400 Bad Request` (np. duplikat SKU, brak wymaganych pól)

#### `PUT /api/products/{id}` — aktualizacja produktu

```bash
curl -X PUT https://magazyn.kzelman.pl/api/products/1 \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"name":"Śruba M10","sku":"SRU-M10-001","description":"Nowy opis","unit":"szt."}'
```

Aktualizacja częściowa — pomiń w JSON pola, których nie chcesz zmieniać.

**Odpowiedź** `200 OK` / `400 Bad Request`

#### `DELETE /api/products/{id}` — usunięcie produktu

```bash
curl -X DELETE https://magazyn.kzelman.pl/api/products/1 \
  -H "Authorization: Bearer <token>"
```

**Odpowiedź** `204 No Content` / `404 Not Found`

---

### 📊 Stan magazynowy (`/api/stock`)

Wszystkie endpointy wymagają nagłówka `Authorization: Bearer <token>`.

#### `GET /api/stock/{productId}` — aktualny stan

```bash
curl -H "Authorization: Bearer <token>" https://magazyn.kzelman.pl/api/stock/1
```

**Odpowiedź** `200 OK`:
```json
{
  "productId": 1,
  "sku": "SRU-M8-001",
  "quantity": 100
}
```

#### `GET /api/stock/{productId}/movements` — historia ruchów

```bash
curl -H "Authorization: Bearer <token>" https://magazyn.kzelman.pl/api/stock/1/movements
```

**Odpowiedź** `200 OK`:
```json
[
  {
    "id": 1,
    "productId": 1,
    "type": "PRZYJECIE",
    "quantity": 50,
    "note": "Dostawa od dostawcy X",
    "createdAt": "2026-05-22T12:00:00",
    "createdBy": "jan"
  }
]
```

#### `POST /api/stock/{productId}/movement` — dodanie ruchu

```bash
curl -X POST https://magazyn.kzelman.pl/api/stock/1/movement \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"type":"PRZYJECIE","quantity":50,"note":"Dostawa od dostawcy X"}'
```

**Typy ruchu:** `PRZYJECIE` (dodaje do stanu), `WYDANIE` (odejmuje — sprawdza dostępność), `KOREKTA` (ustawia dokładnie podaną wartość)

**Odpowiedź** `201 Created` / `400 Bad Request` (np. za mało towaru przy wydaniu)

---

## Zmienne środowiskowe

Aplikacja korzysta ze zmiennych środowiskowych skonfigurowanych w `application.properties`. W środowisku lokalnym można użyć pliku `.env` (automatycznie ignorowany przez Git).

| Zmienna | Opis | Przykład |
|---|---|---|
| `DB_URL` | URL połączenia z PostgreSQL | `jdbc:postgresql://localhost:5432/magazyn_db` |
| `DB_USERNAME` | Użytkownik bazy danych | `admin` |
| `DB_PASSWORD` | Hasło użytkownika bazy | `REMOVED` |
| `JWT_SECRET` | Sekretny klucz do podpisu JWT (minimum 32 znaki) | `kF8pL2mN4qR7sT5vW9xY1zA3bC6dE8gH0j...` |
| `JWT_EXPIRATION` | Czas ważności tokena w milisekundach | `86400000` (24h) |

### Przykładowy plik `.env`

```env
DB_URL=jdbc:postgresql://localhost:5432/magazyn_db
DB_USERNAME=admin
DB_PASSWORD=REMOVED
JWT_SECRET=change_me_to_a_random_string_at_least_32_characters_long
JWT_EXPIRATION=86400000
```

---

## Uruchomienie lokalne

### Wymagania

- **Java 17 JDK** ([Adoptium](https://adoptium.net/))
- **Maven 3.8+** (lub użyj wrappera `mvnw`)
- **Docker Desktop** (do uruchomienia PostgreSQL)

### Krok po kroku

```bash
# 1. Sklonuj repozytorium
git clone https://github.com/krzysztofzelman/magazyn-app.git
cd magazyn-app/magazyn

# 2. Skopiuj i skonfiguruj zmienne środowiskowe
cp .env.example .env
# Edytuj .env — ustaw dane do lokalnej bazy danych

# 3. Uruchom PostgreSQL przez Docker
docker compose up -d

# 4. Uruchom aplikację
./mvnw spring-boot:run
```

Aplikacja dostępna pod adresem **http://localhost:8080**.

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

## Deploy na VPS

### Budowanie i przesyłanie

```bash
# 1. Zbuduj JAR (pomiń testy dla szybszego builda)
./mvnw clean package -DskipTests

# 2. Prześlij na VPS
scp target/magazyn-0.0.1-SNAPSHOT.jar user@twoj-vps:/opt/magazyn/

# 3. Ustaw zmienne środowiskowe na VPS
export DB_URL=jdbc:postgresql://localhost:5432/magazyn_db
export DB_USERNAME=...
export DB_PASSWORD=...
export JWT_SECRET=...
export JWT_EXPIRATION=86400000

# 4. Uruchom
java -jar /opt/magazyn/magazyn-0.0.1-SNAPSHOT.jar --server.port=8080
```

### Uruchomienie jako usługa systemd (zalecane na produkcji)

```ini
# /etc/systemd/system/magazyn.service
[Unit]
Description=Magazyn REST API
After=network.target postgresql.service

[Service]
Type=simple
User=deploy
WorkingDirectory=/opt/magazyn
EnvironmentFile=/opt/magazyn/.env
ExecStart=/usr/bin/java -jar /opt/magazyn/magazyn-0.0.1-SNAPSHOT.jar --server.port=8080
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable magazyn
sudo systemctl start magazyn
sudo systemctl status magazyn
```

### Wymagania na VPS

- Java 17 JDK lub JRE
- PostgreSQL 16 (lub dostęp do zewnętrznej bazy)
- Domena skonfigurowana z przekierowaniem na port 8080 (np. przez Nginx reverse proxy)

---

## CORS

Backend zezwala na żądania z następujących źródeł:

```
http://localhost:5173           # lokalny dev frontendu (Vite)
http://REMOVED:5180        # frontend na VPS (opcjonalnie)
https://magazyn-frontend.vercel.app  # produkcyjny frontend (Vercel)
```

Konfiguracja w `CorsConfig.java` — dozwolone metody: GET, POST, PUT, DELETE, OPTIONS.

---

## Struktura projektu

```
magazyn/
├── src/
│   ├── main/
│   │   ├── java/com/example/magazyn/
│   │   │   ├── MagazynApplication.java          # Entry point
│   │   │   ├── auth/
│   │   │   │   ├── AuthController.java           # POST /api/auth/login, /register
│   │   │   │   ├── AuthService.java              # Logika rejestracji/logowania
│   │   │   │   ├── AuthResponse.java             # DTO: token, username, role
│   │   │   │   ├── LoginRequest.java             # DTO: username, password
│   │   │   │   └── RegisterRequest.java          # DTO: username, password
│   │   │   ├── config/
│   │   │   │   ├── CorsConfig.java               # CORS dla frontendu
│   │   │   │   ├── JwtAuthenticationFilter.java  # Filtrowanie JWT (OncePerRequestFilter)
│   │   │   │   └── SecurityConfig.java           # Spring Security, BCrypt, stateless sesje
│   │   │   ├── controller/
│   │   │   │   ├── ProductController.java        # CRUD produktów
│   │   │   │   └── StockController.java          # Ruchy i stan magazynowy
│   │   │   ├── dto/
│   │   │   │   ├── CreateProductRequest.java
│   │   │   │   ├── UpdateProductRequest.java
│   │   │   │   ├── ProductResponse.java
│   │   │   │   ├── StockMovementRequest.java
│   │   │   │   ├── StockMovementResponse.java
│   │   │   │   └── StockResponse.java
│   │   │   ├── entity/
│   │   │   │   ├── Product.java                  # Encja produktu
│   │   │   │   ├── StockMovement.java            # Encja ruchu magazynowego
│   │   │   │   ├── User.java                     # Encja użytkownika
│   │   │   │   └── MovementType.java             # Enum: PRZYJECIE, WYDANIE, KOREKTA
│   │   │   ├── repository/
│   │   │   │   ├── ProductRepository.java
│   │   │   │   ├── StockMovementRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── service/
│   │   │   │   ├── ProductService.java           # Logika biznesowa produktów
│   │   │   │   ├── StockService.java             # Logika ruchów magazynowych
│   │   │   │   └── CustomUserDetailsService.java # UserDetailsService dla Spring Security
│   │   │   └── util/
│   │   │       └── JwtUtil.java                  # Generowanie/walidacja JWT
│   │   └── resources/
│   │       └── application.properties            # Konfiguracja (zmienne env)
│   └── test/
│       └── java/com/example/magazyn/
│           ├── MagazynApplicationTests.java      # Test kontekstu
│           └── service/
│               └── ProductServiceTest.java       # Testy jednostkowe serwisu
├── docker-compose.yml                            # PostgreSQL 16
├── .env.example                                  # Szablon zmiennych środowiskowych
├── pom.xml                                       # Maven — zależności, wtyczki
├── mvnw / mvnw.cmd                               # Maven Wrapper
└── README.md                                     # Ta dokumentacja
```

---

## Licencja

Projekt prywatny — wszelkie prawa zastrzeżone.
