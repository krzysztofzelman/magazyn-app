# 📦 Magazyn — REST API zarządzania magazynem

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Lombok](https://img.shields.io/badge/Lombok-18BFFF?style=for-the-badge&logo=lombok&logoColor=white)](https://projectlombok.org/)

Backend REST API do zarządzania stanem magazynowym produktów z pełnym CRUD-em.  
Wdrożony na **VPS** pod adresem **[REMOVED:8080](http://REMOVED:8080)**.  
Frontend dostępny na **[magazyn-frontend.vercel.app](https://magazyn-frontend.vercel.app)**.

---

## 📋 Spis treści

- [Opis projektu](#-opis-projektu)
- [Tech Stack](#-tech-stack)
- [Model danych](#-model-danych)
- [API Endpointy](#-api-endpointy)
- [Uruchomienie lokalne](#-uruchomienie-lokalne)
- [Uruchomienie przez Docker](#-uruchomienie-przez-docker)
- [Deploy produkcyjny](#-deploy-produkcyjny)
- [Struktura projektu](#-struktura-projektu)
- [Licencja](#-licencja)

---

## 📋 Opis projektu

**Magazyn** to aplikacja backendowa napisana w **Spring Boot 3.5** z **Java 17**, udostępniająca REST API do zarządzania produktami magazynowymi. Projekt został zaprojektowany w architekturze **MVC** z wyraźnym podziałem na warstwy:

| Warstwa      | Odpowiedzialność                                       |
| ------------ | ------------------------------------------------------ |
| **Controller** | Obsługa żądań HTTP i mapowanie na endpointy REST       |
| **Service**    | Logika biznesowa (walidacja, unikalność SKU)            |
| **Repository** | Warstwa dostępu do danych (Spring Data JPA)            |
| **Entity**     | Model danych mapowany na tabelę w bazie PostgreSQL     |

---

## 🧱 Tech Stack

| Technologia           | Wersja        | Opis                                               |
| --------------------- | ------------- | -------------------------------------------------- |
| **Java**              | 17            | Język programowania (LTS)                          |
| **Spring Boot**       | 3.5.14        | Framework aplikacyjny                              |
| **Spring Data JPA**   | —             | Mapowanie obiektowo-relacyjne (ORM)                |
| **Spring Web**        | —             | REST API (Tomcat osadzony)                         |
| **Spring Validation** | —             | Walidacja danych wejściowych                       |
| **Maven**             | —             | System budowania i zarządzania zależnościami       |
| **PostgreSQL**        | 16            | Produkcyjna baza danych                            |
| **Hibernate**         | —             | Implementacja JPA — automatyczne generowanie DDL   |
| **Docker**            | —             | Konteneryzacja bazy danych                         |
| **Lombok**            | —             | Eliminacja kodu boilerplate (gettery, settery itd.)|

---

## 📦 Model danych

Encja `Product` reprezentuje pojedynczy produkt w magazynie.  
Tabela w bazie danych: **`products`**

| Pole          | Typ Javy         | Kolumna SQL        | Ograniczenia                        |
| ------------- | ---------------- | ------------------ | ----------------------------------- |
| `id`          | `Long`           | `id`               | PK, autoinkrement                   |
| `name`        | `String`         | `name`             | `NOT NULL`                          |
| `sku`         | `String`         | `sku`              | `NOT NULL`, `UNIQUE`                |
| `description` | `String`         | `description`      | —                                   |
| `unit`        | `String`         | `unit`             | `NOT NULL` (np. szt., kg, m, opak.) |
| `createdAt`   | `LocalDateTime`  | `created_at`       | Automatycznie ustawiane przy insert |

---

## 🌐 API Endpointy

**Base URL (produkcja):** `http://REMOVED:8080/api/products`  
**Base URL (lokalnie):** `http://localhost:8080/api/products`

### Zestawienie endpointów

| Metoda   | Endpoint                    | Opis                               | Kod odpowiedzi                |
| -------- | --------------------------- | ---------------------------------- | ----------------------------- |
| `GET`    | `/api/products`             | Lista wszystkich produktów         | `200 OK`                      |
| `GET`    | `/api/products/{id}`        | Pojedynczy produkt po ID           | `200 OK` / `404 Not Found`    |
| `GET`    | `/api/products/sku/{sku}`   | Pojedynczy produkt po kodzie SKU   | `200 OK` / `404 Not Found`    |
| `POST`   | `/api/products`             | Utworzenie nowego produktu         | `201 Created` / `400 Bad Request` |
| `PUT`    | `/api/products/{id}`        | Aktualizacja istniejącego produktu | `200 OK` / `400 Bad Request`  |
| `DELETE`  | `/api/products/{id}`        | Usunięcie produktu                 | `204 No Content` / `404 Not Found` |

### Przykładowe żądanie POST / PUT

```json
{
  "name": "Śruba M8",
  "sku": "SRU-M8-001",
  "description": "Śruba nierdzewna M8 x 30mm",
  "unit": "szt."
}
```

> **Uwaga:** Pole `sku` musi być unikalne. Próba utworzenia lub aktualizacji produktu na istniejący SKU zwróci `400 Bad Request`.

### Przykłady z curl

```bash
# Lista produktów
curl http://REMOVED:8080/api/products

# Produkt po ID
curl http://REMOVED:8080/api/products/1

# Produkt po SKU
curl http://REMOVED:8080/api/products/sku/SRU-M8-001

# Utworzenie produktu
curl -X POST http://REMOVED:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Śruba M8","sku":"SRU-M8-001","description":"Śruba nierdzewna M8 x 30mm","unit":"szt."}'

# Aktualizacja produktu
curl -X PUT http://REMOVED:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Śruba M10","sku":"SRU-M10-001","unit":"szt."}'

# Usunięcie produktu
curl -X DELETE http://REMOVED:8080/api/products/1
```

---

## 🚀 Uruchomienie lokalne

### Wymagania

- [Java 17 JDK](https://adoptium.net/)
- [Maven 3.8+](https://maven.apache.org/download.cgi) (lub użyj wrappera `mvnw`)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (do uruchomienia PostgreSQL)

### Krok po kroku

```bash
# 1. Sklonuj repozytorium
git clone https://github.com/krzysztofzelman/magazyn-app.git
cd magazyn-app/magazyn

# 2. Uruchom PostgreSQL przez Docker (port 5432)
docker compose up -d

# 3. Uruchom aplikację — automatycznie połączy się z PostgreSQL
./mvnw spring-boot:run
```

Aplikacja będzie dostępna pod adresem: **http://localhost:8080**

### Konfiguracja bazy danych (application.properties)

```properties
spring.datasource.url=jdbc:postgresql://REMOVED:5432/magazyn_db
spring.datasource.username=admin
spring.datasource.password=REMOVED
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

Do celów lokalnych wystarczy zmienić `spring.datasource.url` na `jdbc:postgresql://localhost:5432/magazyn_db`.

### Budowanie pliku JAR

```bash
./mvnw clean package -DskipTests
java -jar target/magazyn-0.0.1-SNAPSHOT.jar
```

---

## 🐳 Uruchomienie przez Docker

### Tylko baza danych

```bash
docker compose up -d
```

Usługa `postgres` uruchomi PostgreSQL 16 z następującymi parametrami:

| Parametr       | Wartość       |
| -------------- | ------------- |
| Baza danych    | `magazyn_db`  |
| Użytkownik     | `admin`       |
| Hasło          | `REMOVED`    |
| Port           | `5432`        |

Dane są przechowywane w volume `postgres_data`.

---

## ☁️ Deploy produkcyjny

Aplikacja działa na **VPS** (IP: `REMOVED`) na porcie `8080`.  
Frontend (aplikacja kliencka) dostępny pod adresem: **[magazyn-frontend.vercel.app](https://magazyn-frontend.vercel.app)**

### Dozwolone źródła CORS

```java
.allowedOrigins(
    "http://localhost:5173",                         // lokalny dev frontendu (Vite)
    "http://REMOVED:5180",                      // frontend na VPS (opcjonalnie)
    "https://magazyn-frontend.vercel.app"            // produkcyjny frontend (Vercel)
)
```

### Proces deployu (skrócony)

1. Zbuduj JAR: `./mvnw clean package -DskipTests`
2. Skopiuj JAR na VPS (np. przez `scp`)
3. Uruchom: `java -jar magazyn-0.0.1-SNAPSHOT.jar --server.port=8080`

> W środowisku produkcyjnym zaleca się uruchomienie aplikacji jako usługi systemd (lub w kontenerze Docker).

---

## 📁 Struktura projektu

```
magazyn/
├── src/
│   ├── main/
│   │   ├── java/com/example/magazyn/
│   │   │   ├── MagazynApplication.java         # Entry point Spring Boot
│   │   │   ├── config/
│   │   │   │   └── CorsConfig.java             # Konfiguracja CORS
│   │   │   ├── controller/
│   │   │   │   └── ProductController.java      # REST endpointy
│   │   │   ├── entity/
│   │   │   │   └── Product.java                # Encja JPA
│   │   │   ├── repository/
│   │   │   │   └── ProductRepository.java      # Repozytorium JPA
│   │   │   └── service/
│   │   │       └── ProductService.java         # Logika biznesowa
│   │   └── resources/
│   │       └── application.properties          # Konfiguracja
│   └── test/
│       └── java/com/example/magazyn/
│           └── MagazynApplicationTests.java    # Test kontekstu
├── docker-compose.yml                          # PostgreSQL 16
├── pom.xml                                     # Maven — zależności i build
├── mvnw / mvnw.cmd                             # Maven Wrapper
└── README.md                                   # Ta dokumentacja
```

---

## 📄 Licencja

Projekt nie posiada określonej licencji. W razie potrzeby dodaj odpowiednią (np. MIT, Apache 2.0).
