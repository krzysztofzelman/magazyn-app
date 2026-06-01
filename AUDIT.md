# Audyt projektu magazyn-app

> Data audytu: 2025-05-28  
> Zakres: 50 klas źródłowych + 8 testów  
> Ocena końcowa: **7/10**

---

## Spis treści

1. [Bugi i błędy](#1-bugi-i-błędy)
2. [Bezpieczeństwo](#2-bezpieczeństwo)
3. [Jakość kodu](#3-jakość-kodu)
4. [Testy](#4-testy)
5. [Wydajność](#5-wydajność)
6. [Ocena ogólna](#6-ocena-ogólna)

---

## 1. Bugi i błędy

### 1.1 KOREKTA nie może ustawić stanu na 0 (błąd logiki biznesowej)

**Plik:** `service/StockService.java:31`  
**Opis:** KOREKTA (adjustment) ma nadpisywać stan produktu bezwzględną wartością. Jednak `StockMovementRequest.quantity` ma adnotację `@Positive(message = "Ilość musi być większa od 0")` (DTO: `StockMovementRequest.java:11`). Oznacza to, że nie można skorygować stanu na 0, co jest poprawnym przypadkiem biznesowym (np. zerowanie stanu po inwentaryzacji).

**Wystąpienie:** `dto/StockMovementRequest.java` linia 12  
```java
@Positive(message = "Ilość musi być większa od 0")
private Integer quantity;
```
**Skutek:** Przy próbie ustawienia quantity=0 przez KOREKTA, walidacja zwróci 400 BAD_REQUEST z błędem walidacji. Biznesowo KOREKTA powinna akceptować 0.

**Sugerowana zmiana:** Zamienić `@Positive` na `@PositiveOrZero`, a w serwisie dodać osobny check dla KOREKTA vs PRZYJECIE/WYDANIE.

---

### 1.2 RateLimitFilter ustawia CORS headers ręcznie przy 429

**Plik:** `config/RateLimitFilter.java:49-52`  
**Opis:** Gdy rate limiting zwraca 429, filtr ręcznie ustawia nagłówki CORS:
```java
response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
response.setHeader("Access-Control-Allow-Headers", "*");
response.setHeader("Access-Control-Allow-Credentials", "true");
```
To ustawia `Access-Control-Allow-Origin` na wartość z nagłówka `Origin` żądania, bez żadnej walidacji. W efekcie:

- Każda domena może wysłać żądanie z przeglądarki i otrzymać odpowiedź z kodem 429
- `Access-Control-Allow-Credentials: true` w połączeniu z dynamicznym `Access-Control-Allow-Origin` z echo nagłówka `Origin` umożliwia każdej stronie efektywne wysłanie zapytania z credentials (cookies)
- Co prawda nie ma tu ciasteczek sesyjnych (JWT w localStorage), ale same credentials są bezużyteczne bez cookies — jednak echo Origin + Allow-Credentials to znany wzorzec wykorzystywany w atakach CSA

**Sugerowana zmiana:** Usunąć tę sekcję CORS z RateLimitFilter — CORS jest już obsłużony przez `CorsConfig` na poziomie Spring. Filtr powinien tylko ustawić status 429 i zwrócić JSON-a.

---

### 1.3 GlobalExceptionHandler vs SecurityConfig — potencjalny konflikt obsługi AccessDeniedException

**Plik:** `exception/GlobalExceptionHandler.java:38-47` oraz `config/SecurityConfig.java:69-79`  
**Opis:** Oba miejsca definiują obsługę AccessDeniedException. SecurityConfig robi to przez `accessDeniedHandler` w łańcuchu filtrów, a GlobalExceptionHandler przez `@ExceptionHandler`. W praktyce:

- Dla `@PreAuthorize` na kontrolerach, wyjątek najpierw przechodzi przez filter chain (gdzie łapie go `accessDeniedHandler`), a jeśli ten zapisze odpowiedź, nie dochodzi do `@ExceptionHandler`.
- Jeśli jednak `accessDeniedHandler` zawiedzie (rzuci wyjątkiem), `GlobalExceptionHandler` przechwyci go jako `Exception`.

**Skutek:** Drobna niespójność — oba handlery zwracają JSON, ale z nieco innym formatem. SecurityConfig zwraca `{"status":403,"message":"Brak uprawnień","timestamp":"..."}`, a GlobalExceptionHandler zwraca `ErrorResponse` ze status, message, timestamp i listą errors. Niespójność formatu nie powinna jednak wystąpić w praktyce.

---

### 1.4 JwtUtil.isTokenValid() łapie wszystkie wyjątki w ciszy

**Plik:** `util/JwtUtil.java:47-52`  
**Opis:**
```java
public boolean isTokenValid(String token) {
    try {
        getClaims(token);
        return true;
    } catch (Exception e) {
        return false;  // ❌ wyjątek połykany bez logowania
    }
}
```
Każdy wyjątek — od `ExpiredJwtException` przez `MalformedJwtException` po `IllegalArgumentException` (gdy klucz jest za krótki lub null) — jest połykany i zwracane jest `false`. W przypadku błędu konfiguracji (np. zmienne środowiskowe `JWT_SECRET` niezaładowane), aplikacja będzie po cichu odrzucać wszystkie tokeny, co jest bardzo trudne do zdiagnozowania.

**Sugerowana zmiana:** Przynajmniej logować wyjątki na poziomie DEBUG/TRACE, a w przypadku błędów konfiguracyjnych rzucać wyjątkiem przy starcie aplikacji.

---

### 1.5 ExportService — brak obsługi \r w CSV

**Plik:** `service/ExportService.java:221-227`  
**Opis:**
```java
private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
}
```
Funkcja sprawdza `\n` (line feed), ale nie sprawdza `\r` (carriage return). W Windows linie mogą zawierać `\r\n`, a `\r` sam w sobie może złamać format CSV w niektórych parserach (w tym Excel przy otwieraniu CSV UTF-8).

---

### 1.6 StockController — SpEL w @PreAuthorize wrażliwy na refaktoring

**Plik:** `controller/StockController.java:39`
```java
@PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #request.type.name() == 'PRZYJECIE')")
```
Wyrażenie SpEL wywołuje `#request.type.name()` aby uzyskać nazwę enuma. Działa to, ale:
- Jeśli enum `MovementType` zostanie przemianowany (np. `PRZYJECIE` → `RECEIPT`), zabezpieczenie przestanie działać — po cichu USER nie będzie mógł zrobić żadnego ruchu.
- `name()` w enumach jest niezmienne, więc refaktoring nie jest ryzykowny, ale wartość stringowa może być łatwo przeoczona.

Lepsze podejście: porównywać `#request.type == T(com.example.magazyn.entity.MovementType).PRZYJECIE`.

---

## 2. Bezpieczeństwo

### 2.1 JWT Secret — długość klucza krytyczna

**Plik:** `util/JwtUtil.java:15-19`
```java
public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expiration}") long expiration
) {
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
```
Wygenerowany token ma nagłówek `"alg":"HS512"` (potwierdzone w logowaniu produkcyjnym). Dla HS512 `Keys.hmacShaKeyFor()` wymaga minimum **64 bajtów (512 bitów)** klucza. Jeśli zmienna środowiskowa `JWT_SECRET` jest krótsza, aplikacja rzuci `InvalidKeyException` przy próbie wygenerowania tokena (po starcie, przy pierwszym logowaniu).

**Zalecenie:** Zweryfikować, że `JWT_SECRET` w produkcji ma ≥ 64 znaki (lub ≥ 64 bajty w UTF-8). Dodać walidację przy starcie aplikacji (`@PostConstruct`), która sprawdzi długość klucza i loguje warning lub fail-fast.

---

### 2.2 Rate limiting na logowaniu — 20 req/min

**Plik:** `config/RateLimitFilter.java:63`
```java
Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
```
20 requests per minute na IP. Brutalne ataki słownikowe (1000 haseł) zajęłyby 50 minut dla jednego IP. Dla projektu portfolio to akceptowalne, ale w środowisku produkcyjnym warto rozważyć niższy limit (5-10/min) i/lub blokadę IP po N nieudanych próbach.

---

### 2.3 Endpointy Swagger dostępne tylko dla ADMIN

**Plik:** `config/SecurityConfig.java:46`
```java
.requestMatchers("/swagger-ui/**", "/api-docs/**").hasRole("ADMIN")
```
Swagger dokumentuje wszystkie endpointy i ich schematy wejścia/wyjścia. Ograniczenie do ADMIN to dobra praktyka. Należy jednak pamiętać, że w `OpenApiConfig.java` nie dodano zabezpieczenia na poziomie Swagger (np. `tryItOut` wymaga tokena ADMIN do testowania endpointów).

---

### 2.4 Brak ochrony przed enumeracją ID

Endpointy jak `GET /api/products/{id}`, `GET /api/stats/dashboard` używają numerycznych ID i zwracają 404 vs 200. Umożliwia to enumerację istniejących ID produktów przez atakującego. Nie jest to krytyczna luka w projekcie magazynowym, ale warto o niej wiedzieć.

---

### 2.5 CORS — akceptowalny zakres

**Plik:** `config/CorsConfig.java:16-25`
```java
.allowedOrigins(
    "http://localhost:5173",
    "http://REMOVED:5180",
    "https://magazyn.kzelman.pl"
)
```
Lista dozwolonych originów jest precyzyjna i ograniczona do faktycznie używanych domen. Brak `allowCredentials(true)` w połączeniu z wildcard — dobrze.

**Uwaga:** `REMOVED:5180` to adres IP + port, co może być wewnętrznym adresem. Jeśli dostęp do niego jest publiczny (nie tylko z sieci VPS), to jest OK.

---

## 3. Jakość kodu

### 3.1 Mocne strony

- **Czysta architektura warstwowa** — Controller → Service → Repository, żadne encje nie wyciekają do kontrolerów
- **Wstrzykiwanie przez konstruktor** — wszędzie z wyjątkiem kilku `@Autowired` na polach (konsekwentne)
- **DTO/Response separacja** — każde wyjście API ma dedykowanego DTO
- **Lombok w encjach** — `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- **GlobalExceptionHandler** — spójny format błędów
- **PESSIMISTIC_WRITE w stock** — prawidłowe zabezpieczenie przed race condition
- **SeedService** — przemyślany seeding lokalizacji z hierarchią i przypisaniem produktów

### 3.2 Słabe strony

#### 3.2.1 Brak walidacji na kilku DTO

**Plik:** `dto/LocationRequest.java` — brak `@NotBlank` na `code`, `name`, `type`. Jeśli `type` będzie nieprawidłowy, poleci nieobsłużony `IllegalArgumentException` przy `LocationType.valueOf()`.

**Plik:** `dto/UpdateProductRequest.java` — brak adnotacji walidacyjnych. Kontroler ma `@Valid`, ale nie ma co walidować. Service sprawdza nulle, ale kontrola na poziomie kontrolera jest pusta.

**Plik:** `dto/AssignLocationRequest.java` — brak walidacji na `locationId`.

**Sugestia:** Dodać walidację wszędzie, nawet dla pól opcjonalnych (np. `@Size` na stringach).

#### 3.2.2 Duplikacja kodu w ExportService

**Plik:** `service/ExportService.java`

Cztery metody `exportProductsCsv`, `exportProductsExcel`, `exportStockCsv`, `exportStockExcel` dzielą ~80% wspólnego kodu:
- Tworzenie arkusza Excela z headerem i stylami
- Iteracja po produktach i wypisywanie wartości
- CSV z nagłówkami i wartościami

Każda z metod `export*Csv` i `export*Excel` ma praktycznie identyczną strukturę. Różnią się tylko listą pól i sposobem pobierania wartości.

**Sugestia:** Wyodrębnić wspólne metody: `writeCsv(List<Product>, List<String>, Function<Product, List<String>>)` i podobnie dla Excela. Można też użyć wzorca Template Method.

#### 3.2.3 UpdateProductRequest pozwala zmienić SKU na null

**Plik:** `service/ProductService.java:75-80`
```java
if (request.getSku() != null) {
    if (!existing.getSku().equals(request.getSku())
            && productRepository.findBySku(request.getSku()).isPresent()) {
```
Brak walidacji, że nowe SKU nie jest puste. Jeśli frontend wyśle `"sku": ""`, to SKU zostanie nadpisane pustym stringiem.

#### 3.2.4 Przywracanie kontekstu Security w StockService

**Plik:** `controller/StockController.java:39-43`
```java
@PreAuthorize("hasRole('ADMIN') or (hasRole('USER') and #request.type.name() == 'PRZYJECIE')")
...
String username = SecurityContextHolder.getContext().getAuthentication().getName();
```
Nazwa użytkownika pobierana w kontrolerze i przekazywana do serwisu. To prawidłowy wzorzec (unikamy `SecurityContextHolder.getContext()` w serwisie). Jednak nadal jest to string — bezpieczniej byłoby utworzyć adnotację `@CurrentUser` z resolwerem argumentów.

#### 3.2.5 Nieużywane importy

- `controller/ProductController.java` — importuje `HttpHeaders`, `HttpStatus` — oba używane ✅ (sprawdziłem)
- `entity/Location.java` — `@Column`, `@Enumerated`, `@Entity`, `@GeneratedValue`, `@Id`, `@Table` — wszystko używane ✅
- Brak oczywistych nieużywanych importów — kod jest czysty pod tym względem.

---

## 4. Testy

### 4.1 Pokrycie kodu

| Komponent | Testy jednostkowe | Testy integracyjne | Status |
|---|---|---|---|
| AuthService | ❌ | ✅ (AuthIntegrationTest) | Średnie |
| AuthController | ❌ | ✅ (AuthIntegrationTest) | Średnie |
| ProductService | ✅ (ProductServiceTest) | ✅ (ProductControllerIntegrationTest) | ✅ Dobre |
| ProductController | ❌ | ✅ (ProductControllerIntegrationTest) | Średnie |
| StockService | ✅ (StockServiceTest) | ❌ | Średnie |
| StockController | ❌ | ❌ | ❌ Brak |
| LocationService | ✅ (LocationServiceTest) | ✅ (LocationIntegrationTest) | ✅ Dobre |
| LocationController | ❌ | ✅ (LocationIntegrationTest) | Średnie |
| ExportService | ❌ | ❌ | ❌ Brak |
| StatsService | ❌ | ❌ | ❌ Brak |
| SeedService | ❌ | ❌ | ❌ Brak |
| ImportService | ❌ | ❌ | ❌ Brak |
| JwtUtil | ✅ (JwtUtilTest) | ❌ | ✅ Dobre |
| RateLimitFilter | ❌ | ❌ | ❌ Brak |
| JwtAuthenticationFilter | ❌ | ❌ | ❌ Brak |
| SecurityConfig | ❌ | ✅ (częściowo przez AuthIT) | Słabe |
| GlobalExceptionHandler | ❌ | ❌ | ❌ Brak |

### 4.2 Jakość istniejących testów

**Mocne strony:**
- Testy jednostkowe dla `LocationServiceTest` (17 testów) — świetne pokrycie: happy path, błędy, edge case'y, test hierarchii drzewa
- Testy jednostkowe dla `ProductServiceTest` (16 testów) — bardzo dobre: paginacja, search, create/update/delete, assign location, duplikaty
- `StockServiceTest` (11 testów) — solidne: każdy typ ruchu, walidacja ilości, brak stanu
- `JwtUtilTest` (10 testów) — testuje generowanie, ekstrakcję, walidację, różne role, wygaśnięcie
- `AuthIntegrationTest` (11 testów) — pokrywa rejestrację, logowanie, role, dostęp bez tokena
- `ProductControllerIntegrationTest` (11 testów) — CRUD, duplikaty, autoryzacja, przypisanie lokalizacji
- `LocationIntegrationTest` (11 testów) — drzewo, hierarchia, usuwanie z dziećmi, role

**Słabe strony:**
- **Brak MockMvc** — testy integracyjne używają `WebTestClient.bindToServer()` zamiast `MockMvc`. To jest wolniejsze (faktyczny start serwera na losowym porcie) i mniej wygodne niż `@AutoConfigureMockMvc`. Działa, ale kosztem czasu.
- **Testy integracyjne wymagają PostgreSQL** — wszystkie testy integracyjne mają `@SpringBootTest` i wymagają działającej bazy danych. `MagazynApplicationTests` jest `@Disabled("Requires a running PostgreSQL instance")` — to oznacza, że ani jeden test integracyjny nie może być uruchomiony lokalnie bez postawienia PostgreSQL.
- **Brak testów dla StockController** — trzy endpointy `POST /movement`, `GET /movements`, `GET /stock` nie mają żadnych testów.
- **Brak testów dla ExportService** — eksport CSV/XLSX nie ma testów (ani jednostkowych, ani integracyjnych).
- **Brak testów dla ImportService** — import z CSV/XLSX z wieloma scenariuszami błędów nie jest testowany.
- **Brak testów dla RateLimitFilter** — kluczowy komponent bezpieczeństwa bez pokrycia.
- **Brak testów dla StatsService** — zapytania agregujące i alerty o niskich stanach.
- **Test `login_success` w AuthIntegrationTest rejestruje użytkownika** — to tworzy zależność między testami i zapisuje do bazy, co może wpływać na inne testy. Lepiej seedować testowego użytkownika przed wszystkimi testami.
- **`@DirtiesContext` nie jest używane** — testy integracyjne modyfikują bazę i mogą na siebie wpływać.

### 4.3 Konkretne luki w testach

| Scenariusz | Plik | Powinien testować |
|---|---|---|
| KOREKTA z quantity=0 | StockServiceTest | Edge case: biznesowo dozwolone |
| WYDANIE z quantity=0 | StockServiceTest | Powinien rzucić błąd |
| Export CSV z polskimi znakami | ExportService | Kodowanie UTF-8, znaki diakrytyczne |
| Import pliku z błędnym separatorem | ImportService | Automatyczne wykrywanie separatora |
| Import pliku xlsx z formułami | ImportService | Formuły w komórkach numerycznych |
| RateLimit przekroczony | RateLimitFilter | 429 z correct body |
| RateLimit dla różnych IP | RateLimitFilter | Osobne buckety per IP |
| JWT wygasły w filtrze | JwtAuthenticationFilter | 401 przy wygasłym tokenie |

---

## 5. Wydajność

### 5.1 N+1 wykryte

- **StockMovement** — `findByProductIdOrderByCreatedAtDesc()` używa `@EntityGraph(attributePaths = "product")` ✅. Brak N+1.
- **TopSellingProducts** — custom JPQL z JOIN, tylko jedno zapytanie ✅.
- **ProductService.getAllProducts() paginacja** — Spring Data paginacja, tylko jedno zapytanie ✅.
- **SeedService.seedLocations()** — wykonuje `saveAll()`, potem `findAll()`, potem kolejny `saveAll()`. To 3 zapytania, wszystkie uzasadnione. **Uwaga:** `saveAll()` na dużej liczbie wpisów może być powolne — przy 16 lokalizacjach to pomijalne.

### 5.2 Brakujące indeksy

| Kolumna / zapytanie | Indeks istnieje? |
|---|---|
| `Product.sku` UNIQUE | ✅ (generowany automatycznie przez `@Column(unique=true)`) |
| `Product.name`, `Product.sku` LIKE | ❌ — `findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase` używa `LOWER()` + `LIKE('%...%')`. W PostgreSQL nie może użyć standardowego indeksu B-tree. Dla dużego zbioru danych (10k+ produktów) potrzebny indeks `pg_trgm` (trigram). |
| `Product.locationId` | ❌ — `findByLocationId()` wykona full scan. Jeśli średnio >100 produktów na lokalizację, warto dodać indeks. |
| `StockMovement.product_id` (FK) | ✅ — Hibernate automatycznie tworzy indeks dla FK. |
| `StockMovement.created_at` dla sortowania | ❌ — `findByProductIdOrderByCreatedAtDesc()` sortuje po `created_at` w ramach jednego product_id. Indyvidualnie dla product_id to pomijalne, ale dla wszystkich zapytań naraz bez gdzie product_id — to już inna sprawa. W praktyce zapytanie ma WHERE product_id, więc indeks na product_id wystarczy. |

### 5.3 Paginacja

- **`GET /api/products`** — paginacja ✅ — `Page<ProductResponse>` z parametrami `page`, `size`, `sort`.
- **`GET /api/stock/{productId}/movements`** — **brak paginacji** ❌. Dla produktów z tysiącami ruchów (np. po latach operacji), to zapytanie może zwrócić kilka tysięcy rekordów i obciążyć bazę + sieć. Powinno być stronicowane.
- **`GET /api/locations`** — brak paginacji, ale akceptowalne (zwykle <100 lokalizacji).
- **`GET /api/stats/dashboard`** — brak paginacji, ale aggregate query, zwykle <50 rekordów. OK.

### 5.4 Wąskie gardła

1. **`GET /api/stock/{id}/movements` — brak limitu** — przy 10k+ ruchach na produkcie, zapytanie i serializacja mogą trwać sekundy. Priorytetowo dodać paginację.
2. **`ExportService.exportProductsExcel()` ładuje wszystkie produkty** — `productRepository.findAll()` dla 10k produktów do Excela może zużyć dużo pamięci. Rozważyć strumieniowanie (Spring Data Streamable lub cursor).
3. **`@Transactional(readOnly = true)` na StatsService** — prawidłowo użyte ✅.
4. **`ProductService.getAllProducts()` z search** — `LIKE '%search%'` na tekście to najwolniejszy typ zapytania (nie może użyć indeksu). Dla małej bazy (<1000 produktów) nieistotne. Dla dużej — potrzeba `pg_trgm`.

---

## 6. Ocena ogólna

### 6.1 Ocena: 7/10

**Uzasadnienie:**
Projekt jest dobrze zaprojektowany i napisany — architektura jest czysta, separacja warstwowa, JWT z env-based secret, rate limiting, prawidłowa obsługa transakcji z pessimistic locking. Testy dla serwisów `ProductService`, `LocationService`, `StockService` są wzorowe. Jednak brak testów dla `ExportService`, `ImportService`, `StatsService`, `SeedService` i filtrów obniża ocenę. Ponadto kilka drobnych błędów (KOREKTA na 0, brak walidacji na DTO, pominięte \r w CSV) wymaga poprawek.

### 6.2 Mocne strony

- ✅ Czysta architektura warstwowa z DTO
- ✅ Prawidłowa obsługa błędów przez GlobalExceptionHandler
- ✅ PESSIMISTIC_WRITE dla operacji stock — brak race condition
- ✅ Rate limiting na logowaniu (Bucket4j)
- ✅ JWT z konfigurowalnym secretem i expiracją przez zmienne środowiskowe
- ✅ RBAC przez `@PreAuthorize` na kontrolerach
- ✅ Obsługa importu CSV/XLSX z detekcją separatora
- ✅ Obsługa eksportu CSV/XLSX z auto-size kolumn
- ✅ Drzewo lokalizacji z rekurencyjnym budowaniem
- ✅ Seeding danych testowych z przypisaniem do lokalizacji
- ✅ Statystyki dashboardu (top-selling, reorder alerts)
- ✅ Obsługa plików przez `MultipartFile`
- ✅ OpenAPI / Swagger z konfiguracją JWT
- ✅ Docker + Docker Compose dla prostego wdrożenia
- ✅ Testy jednostkowe dla serwisów z Mockito

### 6.3 Słabe strony

- ❌ KOREKTA nie może ustawić stanu na 0
- ❌ RateLimitFilter ręcznie ustawia CORS (echo Origin)
- ❌ JwtUtil połyka wszystkie wyjątki w ciszy
- ❌ Brak paginacji w `/api/stock/{id}/movements`
- ❌ Brak testów dla ExportService, ImportService, StatsService, SeedService, RateLimitFilter
- ❌ Brak testów dla StockController
- ❌ Brak walidacji na LocationRequest, UpdateProductRequest, AssignLocationRequest
- ❌ Duplikacja kodu w ExportService
- ❌ Brak profilu testowego z H2 (wszystkie testy integracyjne wymagają PostgreSQL)
- ❌ Potencjalna enumeracja ID przez numeryczne identyfikatory

### 6.4 Top 5 rzeczy do poprawy (priorytetowo)

| # | Co | Gdzie | Dlaczego |
|---|---|---|---|
| 1 | **Dodac testy dla brakujących komponentów** | ExportService, ImportService, StatsService, SeedService, StockController, filtry | Bez testów nie ma pewności, czy kod działa; to największa luka w projekcie |
| 2 | **Dodać walidację na DTO** | LocationRequest, UpdateProductRequest, AssignLocationRequest | Brak walidacji może prowadzić do niespójnych danych i brzydkich błędów |
| 3 | **Dodać paginację do ruchów magazynowych** | `StockService.getMovements()` -> zwracać `Page` | Wydajność dla dużych zbiorów; obecnie brak limitu |
| 4 | **Naprawić KOREKTA quantity=0** | `StockMovementRequest` → `@PositiveOrZero`, serwis → osobny check | Błąd logiki biznesowej |
| 5 | **Dodać obsługę \r w CSV + logowanie w JwtUtil** | ExportService.escapeCsv(), JwtUtil.isTokenValid() | Edge case'y które mogą powodować problemy w produkcji |

### 6.5 Czy projekt nadaje się na portfolio dla Junior Developera?

**Tak, absolutnie.** Projekt pokazuje pełny wachlarz umiejętności wymaganych na rynku:

- **Spring Boot 4.0** — najnowsza wersja frameworku
- **Spring Security + JWT** — autentykacja i autoryzacja
- **Spring Data JPA / Hibernate** — relacyjna baza danych z relacjami
- **REST API** — pełny CRUD z paginacją i wyszukiwaniem
- **Obsługa plików** — import CSV/XLSX, eksport CSV/XLSX
- **Rate limiting** — ochrona przed brute-force
- **Walidacja** — Jakarta Validation, Bean Validation
- **Testy** — JUnit 5, Mockito, WebTestClient (można poprawić pokrycie)
- **Docker** — gotowy docker-compose.yml
- **Dokumentacja API** — OpenAPI / Swagger
- **Integracja z frontendem** — React + Vercel
- **Wdrożenie na VPS** — nginx + Docker + domena

Projekt wymaga poprawek wymienionych w Top 5, ale nawet w obecnym stanie pokazuje znacznie więcej niż typowe "todo list API" na portfolio.

**Rekomendacja:** Poprawić punkty z listy (zwłaszcza testy), dodać README z instrukcją uruchomienia testów lokalnie z Testcontainers (PostgreSQL w kontenerze), i projekt będzie gotowy do pokazania na rozmowie kwalifikacyjnej.

---

*Koniec raportu*
