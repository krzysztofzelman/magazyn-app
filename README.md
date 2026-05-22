# Magazyn 🏭

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![H2 Database](https://img.shields.io/badge/H2%20Database-004D40?style=for-the-badge&logo=h2&logoColor=white)](https://www.h2database.com/)
[![Lombok](https://img.shields.io/badge/Lombok-18BFFF?style=for-the-badge&logo=lombok&logoColor=white)](https://projectlombok.org/)

---

## 📋 Opis projektu

**Magazyn** to aplikacja backendowa REST API do zarządzania stanem magazynowym produktów. Umożliwia wykonywanie pełnych operacji CRUD (tworzenie, odczyt, aktualizacja, usuwanie) na produktach w magazynie.

Aplikacja została zbudowana w architekturze **MVC** (Model-View-Controller) z podziałem na warstwy:
- **Controller** – REST endpointy
- **Service** – logika biznesowa
- **Repository** – dostęp do bazy danych (warstwa persistence)
- **Entity** – model danych

---

## 🧱 Tech Stack

| Technologia                 | Wersja    | Opis                                         |
| --------------------------- | --------- | -------------------------------------------- |
| **Java**                    | 17        | Język programowania                          |
| **Spring Boot**             | 3.5.14    | Framework aplikacyjny                        |
| **Spring Data JPA**         | -         | Warstwa dostępu do danych                    |
| **Spring Web**              | -         | REST API                                     |
| **Spring Validation**       | -         | Walidacja danych wejściowych                 |
| **Maven**                   | -         | System budowania i zarządzania zależnościami |
| **H2 Database**             | -         | Wbudowana baza danych (profil domyślny)      |
| **PostgreSQL**              | 16        | Baza danych produkcyjna (Docker)             |
| **Docker**                  | -         | Konteneryzacja                               |
| **Lombok**                  | -         | Redukcja kodu boilerplate (adnotacje)        |
| **Hibernate**               | -         | ORM – mapowanie obiektowo-relacyjne          |

---

## 📦 Model danych – Product

Encja `Product` reprezentuje produkt w magazynie:

| Pole          | Typ               | Opis                                 |
| ------------- | ----------------- | ------------------------------------ |
| `id`          | `Long` (PK, auto) | Unikalny identyfikator produktu      |
| `name`        | `String`          | Nazwa produktu (wymagane)            |
| `sku`         | `String`          | Kod SKU – unikalny (wymagane)        |
| `description` | `String`          | Opis produktu (opcjonalne)           |
| `unit`        | `String`          | Jednostka miary, np. szt., kg, m (wymagane) |
| `createdAt`   | `LocalDateTime`   | Automatycznie generowana data utworzenia |

Tabela w bazie danych: **`products`**

---

## 🌐 API Endpointy

Podstawowy URL: **`http://localhost:8080/api/products`**

| Metoda   | Endpoint              | Opis                             | Status odpowiedzi                               |
| -------- | --------------------- | -------------------------------- | ----------------------------------------------- |
| `GET`    | `/api/products`       | Pobiera listę wszystkich produktów | `200 OK`                                        |
| `GET`    | `/api/products/{id}`  | Pobiera produkt po ID            | `200 OK` / `404 Not Found`                      |
| `GET`    | `/api/products/sku/{sku}` | Pobiera produkt po kodzie SKU    | `200 OK` / `404 Not Found`                      |
| `POST`   | `/api/products`       | Tworzy nowy produkt              | `201 Created` / `400 Bad Request`               |
| `PUT`    | `/api/products/{id}`  | Aktualizuje istniejący produkt   | `200 OK` / `400 Bad Request`                    |
| `DELETE` | `/api/products/{id}`  | Usuwa produkt                    | `204 No Content` / `404 Not Found`              |

### Przykładowe ciało żądania (POST / PUT)

```json
{
  "name": "Śruba M8",
  "sku": "SRU-M8-001",
  "description": "Śruba nierdzewna M8 x 30mm",
  "unit": "szt."
}
```

> **Uwaga:** Pole `sku` musi być unikalne – próba utworzenia/zaktualizowania produktu z istniejącym SKU zwróci błąd `400 Bad Request`.

---

## 🚀 Uruchomienie

### 1. Wymagania wstępne

- [Java 17 JDK](https://adoptium.net/)
- [Maven 3.8+](https://maven.apache.org/download.cgi) (lub użyj wrappera `mvnw`)
- [Docker](https://www.docker.com/) (opcjonalnie – do uruchomienia PostgreSQL)
- [Git](https://git-scm.com/) (opcjonalnie)

### 2. Uruchomienie lokalne (H2 – baza w pamięci)

Domyślnie aplikacja korzysta z wbudowanej bazy H2 – nie wymaga instalowania dodatkowej bazy danych.

```bash
# Sklonuj repozytorium (jeśli dotyczy)
git clone <repozytorium-url>
cd magazyn

# Zbuduj i uruchom
./mvnw spring-boot:run
```

Aplikacja będzie dostępna pod adresem: **http://localhost:8080**

Konsola H2: **http://localhost:8080/h2-console**
- JDBC URL: `jdbc:h2:mem:magazyn_db`
- User: `sa`
- Password: *(puste)*

### 3. Uruchomienie z PostgreSQL (Docker)

Aplikacja jest skonfigurowana do współpracy z PostgreSQL poprzez Docker Compose.

```bash
# Uruchom kontener PostgreSQL w tle
docker compose up -d

# Uruchom aplikację
./mvnw spring-boot:run
```

> Aby przełączyć się na PostgreSQL, zmień konfigurację w pliku `application.properties` na poniższe wartości:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/magazyn_db
spring.datasource.driverClassName=org.postgresql.Driver
spring.datasource.username=admin
spring.datasource.password=REMOVED
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

### 4. Budowanie pliku JAR

```bash
./mvnw clean package -DskipTests
java -jar target/magazyn-0.0.1-SNAPSHOT.jar
```

### 5. Testowanie API (przykład z curl)

```bash
# Pobierz wszystkie produkty
curl http://localhost:8080/api/products

# Utwórz nowy produkt
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Śruba M8","sku":"SRU-M8-001","description":"Śruba nierdzewna M8 x 30mm","unit":"szt."}'

# Pobierz produkt po ID
curl http://localhost:8080/api/products/1

# Pobierz produkt po SKU
curl http://localhost:8080/api/products/sku/SRU-M8-001

# Zaktualizuj produkt
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Śruba M10","sku":"SRU-M10-001","unit":"szt."}'

# Usuń produkt
curl -X DELETE http://localhost:8080/api/products/1
```

---

## 🔧 Konfiguracja

Kluczowe ustawienia w `src/main/resources/application.properties`:

| Właściwość               | Wartość domyślna       | Opis                                     |
| ------------------------ | ---------------------- | ---------------------------------------- |
| `spring.datasource.url`  | `jdbc:h2:mem:magazyn_db` | URL bazy danych                         |
| `spring.jpa.hibernate.ddl-auto` | `update`         | Automatyczne tworzenie/aktualizacja schematu bazy |
| `spring.jpa.show-sql`    | `true`                 | Wyświetlanie zapytań SQL w konsoli       |
| `spring.h2.console.enabled` | `true`              | Włączenie konsoli H2                     |

---

## 📁 Struktura projektu

```
magazyn/
├── src/
│   ├── main/
│   │   ├── java/com/example/magazyn/
│   │   │   ├── MagazynApplication.java      # Klasa główna (entry point)
│   │   │   ├── controller/
│   │   │   │   └── ProductController.java    # REST endpointy
│   │   │   ├── entity/
│   │   │   │   └── Product.java              # Model danych (JPA)
│   │   │   ├── repository/
│   │   │   │   └── ProductRepository.java    # Warstwa dostępu do danych
│   │   │   └── service/
│   │   │       └── ProductService.java       # Logika biznesowa
│   │   └── resources/
│   │       └── application.properties        # Konfiguracja aplikacji
│   └── test/
├── docker-compose.yml                        # Konfiguracja kontenera PostgreSQL
├── pom.xml                                   # Plik Maven (zależności, build)
├── mvnw / mvnw.cmd                           # Maven Wrapper
└── HELP.md
```

---

## 📜 Uwagi

- **Brak repozytorium GitHub** – ten projekt nie ma aktualnie skonfigurowanego zdalnego repozytorium. Aby utworzyć repozytorium, wykonaj:

  ```bash
  # Zainicjuj lokalne repozytorium Git
  git init
  git add .
  git commit -m "Initial commit"

  # Utwórz repozytorium na GitHub, a następnie:
  git remote add origin https://github.com/TWOJA_NAZWA_UZYTKOWNIKA/magazyn.git
  git branch -M main
  git push -u origin main
  ```

---

## 📄 Licencja

Projekt jest udostępniany bez określonej licencji. W razie potrzeby dodaj odpowiednią licencję (np. MIT, Apache 2.0).
