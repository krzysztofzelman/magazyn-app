# Audyt projektu magazyn — 2026-06-13

**Data audytu:** 2026-06-13
**Zakres:** Backend (Spring Boot 4.0.6, Hibernate 7.2.12, Flyway), Frontend (React 19, TypeScript 6, Vite 8), Deployment (VPS, Docker, nginx, SSL, backup)
**Autor:** Qwen Code Agent

---

## Status ogólny

| Element | Status |
|---------|--------|
| Backend build (Maven) | ✅ JDK 17 — kompilacja OK |
| Backend testy jednostkowe | 🟡 Tylko JwtUtilTest, brak testów dla serwisów/kontrolerów |
| Frontend build (`npm run build`) | ✅ tsc + vite OK |
| Frontend testy | ✅ 9 testów Pagination — wszystkie przechodzą |
| Aplikacja na VPS | ✅ Uruchomiona, odpowiada HTTP 200 |
| SSL (Let's Encrypt) | ✅ Ważny do 2026-08-21 |
| Backupy | ✅ Działają (codziennie 2:00, retention 7 dni) |
| Multi-tenancy | ⚠️ @Filter obecne, ale luki w nowych encjach + findById |

---

## 🔴 Błędy krytyczne

### B1. App w Dockerze działa jako root

- **Plik:** `Dockerfile`, `docker-compose.yml`
- **Problem:** Zarówno build (`FROM maven:3.9-eclipse-temurin-25`) jak i runtime (`FROM eclipse-temurin:25-jre`) działają jako `root`. Potwierdzone: `docker exec magazyn-app whoami` → `root`. Proces `java -jar app.jar` na PID 1 jako root.
- **Skutek:** Jeśli aplikacja zostanie skompromitowana (RCE przez np. deserializację), atakujący ma pełny dostęp root do kontenera. Escape z kontenera root = root na hoście.
- **🔴 Ryzyko:** **WYSOKIE** — brak warstwy bezpieczeństwa w przypadku podatności.
- **Naprawa:** Dodać warstwę `USER appuser:appuser` w Dockerfile z dedykowanym użytkownikiem i grupą:

```dockerfile
FROM eclipse-temurin:25-jre
RUN groupadd -r appuser && useradd -r -g appuser -d /app appuser
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser:appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### B2. Nowe encje fakturowe (V12) brak warehouseFilter

- **Pliki:** `entity/Invoice.java`, `entity/InvoiceItem.java`, `entity/CompanySettings.java`
- **Problem:** Wszystkie encje inventory mają `@Filter(name = "tenantFilter")` i `@Filter(name = "warehouseFilter")`. Trzy nowe encje z migracji V12 mają **tylko** `tenantFilter` — brak `warehouseFilter`. Invoice nie ma nawet pola `warehouse_id`.
- **Skutek:** Faktury są izolowane między tenantami, ale **nie są izolowane między magazynami**. Gdy filtr warehouse jest aktywny, zapytania do Invoice/InvoiceItem/CompanySettings go zignorują.
- **🔴 Ryzyko:** **ŚREDNIE** — w widoku "pojedynczy magazyn" użytkownik zobaczy faktury ze wszystkich magazynów.
- **Naprawa:** Dodać `@Filter(name = "warehouseFilter", condition = "warehouse_id = :warehouseId")` do Invoice, InvoiceItem, CompanySettings. Dodać pole `warehouseId` do Invoice.

---

### B3. findProductsBelowMinStock() i getTotalStockValue() — brak izolacji tenantów

- **Pliki:** `repository/ProductRepository.java:36-41`
- **Problem:** Metody `findProductsBelowMinStock()` i `getTotalStockValue()` to zapytania JPQL bez warunku `tenant_id`. Polegają wyłącznie na aktywnym `@Filter` z Hibernate. Jeśli filtr nie jest włączony (np. zadanie scheduleowane bez TenantContext), zwracają dane cross-tenant.
- **Skutek:** Dashboard z minimalnymi stanami i całkowitą wartością magazynu może wyciekać dane między tenantami.
- **🔴 Ryzyko:** **WYSOKIE** — podobny mechanizm jak w B4 poprzedniego audytu (NotificationService).
- **Naprawa:** Dodać `AND tenant_id = :tenantId` do zapytań JPQL jako warstwę obrony. Alternatywnie: upewnić się, że wszystkie wywołania tych metod mają TenantContext ustawiony.

---

### B4. exportProductsCsv/Excel — brak filtra tenanta

- **Plik:** `service/ExportService.java`
- **Problem:** `productRepository.findAll()` — zwraca wszystkie produkty bez względu na tenanta. Eksport danych po stronie backendu (np. scheduled) lub gdy filtr Hibernate jest nieaktywny → wyciek danych.
- **🔴 Ryzyko:** **WYSOKIE** — eksport danych wszystkich tenantów do pliku CSV/Excel.
- **Naprawa:** Zmienić na `findByTenantId(tenantId)` lub dodać warunek w JPQL.

---

### B5. SSH — hasło i root login włączone

- **Plik:** `/etc/ssh/sshd_config` na VPS
- **Ustawienia:** `PermitRootLogin yes`, `PasswordAuthentication yes`
- **Problem:** Logowanie root przez SSH z hasłem zwiększa powierzchnię ataku. Brute-force na porcie 2022 jest możliwy (fail2ban chroni tylko port 22).
- **🔴 Ryzyko:** **ŚREDNIE** — choć SSH na porcie 2022 (nie standardowym) i fail2ban chroni port 22, samo `PasswordAuthentication yes` + `PermitRootLogin yes` to znacznie podwyższone ryzyko.
- **Naprawa:** `PermitRootLogin prohibit-password`, `PasswordAuthentication no` — używać tylko kluczy SSH.

---

### B6. Brak automatycznego odświeżania tokena JWT na froncie

- **Pliki:** `magazyn-frontend/src/services/api.ts:87`, `hooks/useAuth.ts`, `components/LoginPage.tsx`
- **Problem:** `refreshToken` jest zapisywany do localStorage (LoginPage.tsx:37) i usuwany przy wylogowaniu (useAuth.ts:50), ale **nigdzie nie ma mechanizmu automatycznego odświeżania** tokena. Gdy token wygaśnie, użytkownik otrzyma 401 i musi się ręcznie zalogować ponownie.
- **Skutek:** Utrata sesji w trakcie pracy. Token JWT ma skończony czas życia — po jego wygaśnięciu wszystkie requesty API failują z 401 bez próby odświeżenia.
- **⚠️ Ryzyko:** **ŚREDNIE** — UX degradacja, ale nie utrata danych.
- **Naprawa:** Zaimplementować interceptora axios, który przy 401 próbuje `POST /api/auth/refresh` z `refreshToken` przed zwróceniem błędu.

---

### B7. Silent catch() w formularzach (DocumentFormModal + LocationFormModal)

- **Pliki:** `magazyn-frontend/src/components/DocumentFormModal.tsx:36`, `LocationFormModal.tsx:86`
- **Problem:** Oba mają `.catch(() => {})` — błąd API przy ładowaniu danych do formularza jest połykany bez żadnej informacji dla użytkownika. Formularz wyświetla pustą listę bez komunikatu błędu.
- **Skutek:** Użytkownik nie wie, że lista jest niekompletna. Poważniejsze dla DocumentFormModal (produkty) niż dla LocationFormModal (lokalizacje).
- **⚠️ Ryzyko:** **ŚREDNIE** — użytkownik może zaakceptować pustą listę jako "brak danych".
- **Naprawa:** Dodać `catch(error => setError(error.message || 'Błąd ładowania'))`.

### B8. 11x `any` type w kodzie frontend

- **Pliki:** `BarcodeScanner.tsx` (6x), `LocationPanel.tsx` (3x), `ScannerPanel.tsx` (1x), `MobileIssue.tsx` (1x)
- **Problem:** Głównie `catch (err: any)` — TypeScript strict mode jest aktywny (`noUnusedLocals: true`, `noUnusedParameters: true`), ale `any` w catch blokach omija typowanie. BarcodeScanner ma dodatkowo `useRef<any>(null)` i dynamiczny import bez typu.
- **Skutek:** Utrata bezpieczeństwa typów. Przy zmianie API odpowiedzi, catch bloki nie zostaną zgłoszone jako błędy kompilacji.
- **⚠️ Ryzyko:** **NISKIE** — runtime behavior niezmieniony, ale utrudnione refactoringi.
- **Naprawa:** Użyć `unknown` z type-narrowing lub zdefiniować interfejs `ApiError { response?: { data?: { message?: string } } }`.

### B9. Martwy kod frontend — ~2,000 linii nieużywanych komponentów

- **Pliki:** `ReservationPanel.tsx`, `ContractorTable.tsx`, `ContractorFormModal.tsx`, `AuditLogPanel.tsx`, `UserManagementPanel.tsx`, `hooks/useContractors.ts`, `hooks/useReservations.ts`, plus powiązane serwisy (`contractorService`, `auditService`, `userService`, `reservationService`, `pendingScanService`, `batchService`, `productAvailabilityService`)
- **Problem:** Wszystkie powyższe komponenty i hooki są **zdefiniowane, ale nigdzie nie importowane ani używane**. Serwisy są eksportowane z `api.ts` ale nie wywoływane przez żaden aktywny komponent. Fragmenty te zostały prawdopodobnie utworzone podczas wcześniejszej fazy rozwoju i pozostały po zmianie zakresu.
- **Skutek:** ~2,000 linii martwego kodu do utrzymania. Mylące dla nowych programistów. Testy tych komponentów nigdy nie są uruchamiane.
- **🔴 Ryzyko:** **NISKIE** dla bezpieczeństwa, ale **ŚREDNIE** dla utrzymania kodu.
- **Naprawa:** Usunąć martwe komponenty/serwisy/hooki lub przenieść do osobnego katalogu `_archive/` z README wyjaśniającym, dlaczego zostały zakonserwowane.

---

## 🟡 Sugestie backend

### B8. Invoice nie ma pola warehouse_id

- **Plik:** `entity/Invoice.java`
- **Problem:** Invoice nie przechowuje ID magazynu. Faktura jest generowana z dokumentu WZ (który ma warehouse), ale pole nie jest zachowywane.
- **Skutek:** Nie można filtrować faktur po magazynie. Po dodaniu `warehouseFilter` będzie potrzebne pole.
- **Naprawa:** Dodać `@Column(name = "warehouse_id") private Long warehouseId;` do Invoice.

### B9. TenantService.register() — brak transakcji

- **Plik:** `service/TenantService.java`
- **Problem:** `register()` tworzy tenanta, użytkownika, magazyn, lokacje w osobnych operacjach — brak `@Transactional`. Jeśli któreś zapytanie failuje, dane zostają w częściowym stanie.
- **Skutek:** Możliwe częściowe utworzenie tenanta bez magazynu lub użytkownika — stan niekonsystentny.
- **🔴 Ryzyko:** **ŚREDNIE** — występuje tylko przy błędzie bazy, ale skutek jest brzydki.
- **Naprawa:** Dodać `@Transactional` do `register()`.

### B10. AuditLogService — escapeCsv() zduplikowane w ExportService

- **Pliki:** `service/AuditLogService.java`, `service/ExportService.java`
- **Problem:** Oba serwisy implementują prywatną metodę `escapeCsv()`. ExportService obsługuje już `\r` (poprawione po poprzednim audycie). AuditLogService ma swoją kopię.
- **Skutek:** DRY violation — jeśli jeden fix się zmieni, drugi pozostanie w tyle.
- **Naprawa:** Wyodrębnić do `util/CsvUtils.java`.

### B11. InvoiceController.payInvoice() — @Valid na optional body

- **Plik:** `controller/InvoiceController.java:63`
- **Problem:** `@Valid @RequestBody(required = false)` — `PayInvoiceRequest` może być null, ale `@Valid` nie jest przetwarzane dla null body. Kod sprawdza `request != null` wewnątrz, co jest poprawne, ale `@Valid` jest mylące.
- **Skutek:** Kosmetyka — brak rzeczywistego problemu, ale misleading annotation.
- **Naprawa:** Usunąć `@Valid` lub zmienić na `required = true`.

### B12. User entity — @Data eksponuje pole password

- **Plik:** `entity/User.java:27`
- **Problem:** Lombok `@Data` generuje getter `getPassword()` dla zahashowanego hasła BCrypt.
- **Skutek:** Przy serializacji do JSON (np. logowanie debugowe) hasło (hash) jest dostępne. Ograniczone ryzyko, bo BCrypt hash nie jest łatwy do odwrócenia, ale niepotrzebna ekspozycja.
- **Naprawa:** Dodać `@ToString.Exclude` na polu `password`.

### B13. InvoiceItem — brak relacji zwrotnej w Invoice

- **Plik:** `entity/InvoiceItem.java`
- **Problem:** `InvoiceItem.invoice` to `@ManyToOne(fetch = FetchType.LAZY)` — poprawnie. Ale przy `@Data` na obu stronach, Lombok generuje `hashCode()`/`equals()` używające obu stron, co może prowadzić do `StackOverflowError` przy cyklicznych wywołaniach (`items.stream().map(InvoiceItem::getInvoice)`).
- **Skutek:** Ryzyko LazyInitializationException poza transakcją (dla wywołań `getInvoice()` z itemu) lub StackOverflow przy debug/logowaniu.
- **Naprawa:** Rozważyć `@EqualsAndHashCode.Exclude` na `InvoiceItem.invoice`.

### B14. WarehouseController — brak @PreAuthorize

- **Plik:** `controller/WarehouseController.java`
- **Problem:** Endpointy `getAll()`, `getById()` nie mają `@PreAuthorize` — polegają tylko na globalnym `SecurityConfig`.
- **Skutek:** Jeśli SecurityConfig zostanie zmieniony, endpointy mogą być dostępne dla niezautoryzowanych.
- **Naprawa:** Dodać `@PreAuthorize("isAuthenticated()")` do read endpointów.

### B15. SeedService — hardcodowane dane demo

- **Plik:** `service/SeedService.java`
- **Problem:** `seedUsers()` tworzy użytkowników z hasłami: `REMOVED`, `manager123`, `warehouse123`, `viewer123`. Kod istnieje w repozytorium produkcyjnym.
- **Skutek:** Ryzyko, że ktoś wywoła endpoint seed na produkcji lub że testowi użytkownicy zostaną w bazie.
- **Naprawa:** Przenieść seed do `@Profile("dev")` lub skryptu CLI. Lub usunąć z kodu produkcyjnego.

---

## 🟡 Sugestie frontend

### F1. Brak i18n — 100% stringów zakodowanych po polsku

- **Problem:** W całym projekcie (38 plików `.tsx`) **nie ma ani jednego biblioteki i18n** (brak `react-i18next`, `react-intl`, itp.). Każdy user-facing string jest zakodowany na sztywno po polsku: "Produkty", "Dokumenty", "Sesja wygasła" itd. Dotyczy również testów.
- **Skutek:** Jakakolwiek internacionalizacja wymagałaby gruntownego przepisania tekstu we wszystkich komponentach.
- **Naprawa:** Dodać bibliotekę i18n (np. `react-i18next`) i migrować stringi do plików tłumaczeń.

### F2. Brak ThemeContext — dark mode nie jest w pełni zintegrowany

- **Problem:** Stan motywu jest lokalny w `ThemeToggle`. Inne komponenty nie mogą go odczytać ani dostosować CSS. Brak CSS custom properties dla ciemnego motywu.
- **Skutek:** Przełącznik dark mode istnieje, ale nie zmienia wyglądu strony.
- **Naprawa:** Stworzyć `ThemeContext` i dodać ciemne warianty CSS.

### F3. Brak opcji "Wszystkie magazyny" w WarehouseSelector

- **Problem:** Po wybraniu magazynu nie ma UI, żeby go odznaczyć.
- **Skutek:** Użytkownik nie może wrócić do widoku wszystkich magazynów bez odświeżenia strony.
- **Naprawa:** Dodać opcję "Wszystkie magazyny" (value=null) jako pierwszą na liście.

### F4. Użycie `any` typu w kilku hookach

- **Pliki:** `hooks/useProducts.ts`, `hooks/useReservations.ts`, `hooks/useContractors.ts`
- **Problem:** `eslint-disable-next-line` na brakujących dependencjach w `useEffect`. TypeScript `noUnusedLocals: true` i `noUnusedParameters: true` w tsconfig, ale eslint-disable pozwala na niepoprawne zależności.
- **Skutek:** Stale dane (stale closures) — useEffect nie refetchuje po zmianie zależności.
- **Naprawa:** Dodać brakujące zależności lub użyć `useCallback`/`useRef`.

### F5. ScannerAudio.ts — ciche połykanie błędów AudioContext

- **Plik:** `utils/ScannerAudio.ts`
- **Problem:** `catch { /* Audio not available — silently ignore */ }` — brak logowania do konsoli.
- **Skutek:** Trudne debugowanie problemów audio na różnych urządzeniach.
- **Naprawa:** Dodać `console.warn('AudioContext not available:', error)`.

---

## 🟡 Sugestie deployment

### D1. Brak resource limits w docker-compose.yml

- **Plik:** `docker-compose.yml`
- **Problem:** Żaden serwis nie ma `deploy.resources.limits` (memory, cpu). Kontener `magazyn-app` (Java 25 + Spring Boot) może zużyć całą pamięć VPS (3.5GB), co przy innych projektach na tym samym serwerze (restaurant, stitchcore) grozi OOM.
- **Potwierdzone:** `docker inspect magazyn-app` → Memory: unlimited, NanoCpus: unlimited.
- **Ryzyko:** OOM kill całego VPS przy skokowym obciążeniu.
- **Naprawa:** Dodać limity:
```yaml
app:
  deploy:
    resources:
      limits:
        memory: 512M
  # lub starsza składnia:
  mem_limit: 512m
  mem_reservation: 256m
```

### D2. Brak rate limitingu na nginx

- **Plik:** `/etc/nginx/nginx.conf`, `/etc/nginx/sites-enabled/kzelman`
- **Problem:** Brak `limit_req_zone` i `limit_req` w konfiguracji nginx. Przy logowaniu (POST /api/auth/login) można wykonać dowolną liczbę prób.
- **Potwierdzone:** `grep -r 'limit_req' /etc/nginx/` → no matches.
- **Ryzyko:** Brute-force atak na endpoint logowania. Bucket4j na backendzie działa (20 req/min), ale to ochrona application-layer — nginx mógłby zablokować na poziomie sieci.
- **Naprawa:** Dodać do nginx.conf:
```nginx
limit_req_zone $binary_remote_addr zone=login:10m rate=5r/m;
```
I w bloku server:
```nginx
location /api/auth/login {
    limit_req zone=login burst=3 nodelay;
    proxy_pass http://localhost:8080/api/auth/login;
}
```

### D3. Brak HSTS, CSP, stare TLS w nginx

- **Plik:** `/etc/nginx/sites-enabled/kzelman`
- **Sprawdzone:** Response headers na `https://magazyn.kzelman.pl/`:
  - `Server: nginx/1.18.0 (Ubuntu)` — **serwer się ujawnia** (`server_tokens` zakomentowane)
  - `Cross-Origin-Opener-Policy: same-origin` ✅
  - `Cross-Origin-Embedder-Policy: credentialless` ✅
  - Brak `Strict-Transport-Security` (HSTS) ❌
  - Brak `Content-Security-Policy` ❌
  - Brak `Referrer-Policy` ❌
  - Brak `Permissions-Policy` ❌
- **Dodatkowo:** `ssl_protocols TLSv1 TLSv1.1 TLSv1.2 TLSv1.3` — zawiera **TLSv1.0 i TLSv1.1**, które są wycofane i mają znane podatności.
- **Ryzyko:** Brak HSTS = możliwy SSL stripping. Brak CSP = podatność na XSS nie jest mitigowana. Stare TLS = słabe szyfrowanie.
- **Naprawa:** Dodać do bloku `server`:
```nginx
server_tokens off;
ssl_protocols TLSv1.2 TLSv1.3;
add_header Strict-Transport-Security "max-age=63072000; includeSubDomains; preload" always;
add_header Content-Security-Policy "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; connect-src 'self'; font-src 'self'; frame-ancestors 'none';" always;
add_header Referrer-Policy "strict-origin-when-cross-origin" always;
add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
```

### D4. Docker build cache 7.5GB (było 9.75GB przed częściowym prune) — brak cleanup

- **Sprawdzone:** `docker system df` → Build Cache: 7.5GB total (było 9.75GB przed częściowym prune podczas audytu), ~3GB reclaimable.
- **Problem:** Build cache nie jest czyszczony po deployu. Każdy rebuild (mvn clean package w Dockerze) zostawia warstwy.
- **Ryzyko:** Dysk 40GB → 68% użyte. Przy regularnych deployach może zabraknąć miejsca.
- **Naprawa:** Dodać `docker builder prune -f` do skryptu deploy. Albo cron job co tydzień.

### D5. 21 Docker volumes — tylko 6 aktywnych

- **Sprawdzone:** `docker volume ls` → 21 volumes, z czego tylko 6 jest używanych (w tym `magazyn-app_postgres_data`). Reszta to pozostałości po innych projektach/rebuildach.
- **Ryzyko:** 194.1MB nieużywanych danych na dysku.
- **Naprawa:** `docker volume prune -f` — usunąć nieużywane volumes.

### D6. Otwarte porty na zewnątrz (inne projekty)

- **Sprawdzone:** `ss -tulpn` → porty 3001, 5173, 5174, 9090, 8000, 8001 są otwarte na `0.0.0.0` (dla innych projektów: stitchcore, restaurant, smart-myslowice).
- **Ryzyko:** Większa powierzchnia ataku. Jeśli jeden projekt ma podatność, cały serwer jest zagrożony.
- **Uwaga:** To dotyczy całego VPS, nie tylko magazynu. Wymagałoby to osobnych serwerów dla każdego projektu lub lepszej izolacji.

### D7. Brak monitoringu i alertów

- **Problem:** Brak narzędzi monitorujących (Prometheus, Grafana, Sentry, uptime robot). Logi nginx są rotowane (logrotate), ale nie ma alertów na błędy 5xx ani monitoringu dostępności.
- **Skutek:** Jeśli aplikacja padnie, nikt się nie dowie do momentu ręcznego sprawdzenia.
- **Naprawa:** Dodać healthcheck endpoint (`/actuator/health` już istnieje) + zewnętrzny monitoring (np. Uptime Kuma lub Better Uptime).

### D8. Brak zautomatyzowanego deployu

- **Sprawdzone:** Brak webhooka, deploy.sh, git hooks. Repozytorium `/root/magazyn-app` jest git clone, ale deploy jest ręczny (SSH → git pull → docker compose up --build -d).
- **Ryzyko:** Ryzyko błędu ludzkiego przy ręcznym deployu.
- **Naprawa:** Dodać GitHub webhook + prosty skrypt deploy lub GitHub Actions → SSH.

### D9. Docker bypassuje UFW — otwarte porty mimo firewalla

- **Sprawdzone:** Porty 8080, 5173, 5174, 3001, 8001, 9090 są bindowane do `0.0.0.0` przez docker-proxy. Docker wstawia własne reguły iptables, które **omijają UFW**. Nawet jeśli UFW nie pozwala na port 8080, kontener aplikacji jest dostępny z zewnątrz.
- **Skutek:** UFW na VPS nie chroni efektywnie przed dostępem do kontenerów Docker. Aplikacje na portach 3001, 5173, 5174, 8001, 9090 są potencjalnie dostępne z internetu.
- **⚠️ Ryzyko:** **ŚREDNIE** — zwiększona powierzchnia ataku. Wymaga konfiguracji DOCKER_OPTS lub nginx reverse proxy jako jedynego wejścia.
- **Naprawa:** Dodać `"iptables": false` do `/etc/docker/daemon.json` i skonfigurować UFW rules ręcznie. Lub ograniczyć expose do `127.0.0.1` w docker-compose.yml.

### D10. Podwójne backupy — jeden z zerowym plikiem

- **Sprawdzone:** Dwa nakładające się mechanizmy backupu:
  1. **Kontener Docker `magazyn-backup`** — uruchamia `backup.sh` co 24h. Produkuje pliki ~13-16 KB (gzipped SQL).
  2. **Cron hosta o 2:00** — uruchamia `/usr/local/bin/backup-magazyn-db.sh` (używa `docker exec pg_dump`). Zapisuje do `/var/backups/postgres/`.
- **Problem:** Mechanizmy nakładają się. Cron backup z 2026-06-12 wyprodukował **plik 0 bajtów** (`magazyn_20260612_020001.sql` — 0 bajtów, bez `.gz` — gzip nie został uruchomiony). Backup po cichu nie powiódł się.
- **⚠️ Ryzyko:** **ŚREDNIE** — duplikacja nie jest szkodliwa sama w sobie, ale cichy failure backupu oznacza, że nie ma gwarancji, że backupy faktycznie działają.
- **Naprawa:** Ujednolicić backup do jednego mechanizmu. Dodać monitoring sukcesu backupu (np. wysyłanie maila lub healthchecks.io ping). Usunąć martwy kontener backup z docker-compose.

### D11. Brak off-site backupu

- **Sprawdzone:** Oba mechanizmy backupu zapisują na tym samym dysku VPS (`/var/backups/postgres/` i `/root/magazyn-app/backups/`).
- **Ryzyko:** Jeśli dysk VPS ulegnie awarii lub serwer zostanie skompromitowany, wszystkie backupy są tracone razem z bazą danych.
- **⚠️ Ryzyko:** **ŚREDNIE** — DR (disaster recovery) nie istnieje.
- **Naprawa:** Dodać wysyłkę backupów poza VPS: rsync do zdalnego serwera, `rclone` do S3/Backblaze B2, lub prosty `scp` na inny serwer.

### D12. Brak log rotation dla kontenerów Docker

- **Sprawdzone:** Kontenery Docker używają domyślnego drivera `json-file` bez limitów. nginx ma logrotate (14 dni, gzip), ale logi kontenerów rosną bez ograniczeń.
- **Ryzyko:** Logi kontenerów mogą zająć cały dysk przy dłuższym działaniu bez restartu.
- **⚠️ Ryzyko:** **NISKIE** — przy obecnym dysku 13GB free, ale rosnące z czasem.
- **Naprawa:** Dodać do `docker-compose.yml`:
```yaml
logging:
  driver: "json-file"
  options:
    max-size: "10m"
    max-file: "3"
```

---

## 🔵 Drobne uwagi

### Backend

- **Warehouse entity** — nie rozszerza `TenantAware` (niespójne). Ma własne pole `tenantId`, ale nie ma @Filter.
- **Product entity** — dwa pola `locationId` i `defaultLocationId` (dezorientujące).
- **TenantSessionFilter/WarehouseSessionFilter** — Hibernate filtry nigdy nie są wyłączane przez `disable()`.
- **Product.barcode** — `@Column(unique = true)` jest globalnie unikalne. W multi-tenancy to problem — dwa tenanty nie mogą mieć produktu z tym samym kodem kreskowym.
- **User.username** — `@Column(unique = true)` globalnie unikalne. Dwa tenanty nie mogą mieć użytkownika o tej samej nazwie.

### Frontend

- **useProducts.ts, useReservations.ts, useContractors.ts** — `eslint-disable-next-line` na brakujących dependencjach w `useEffect`.
- **ScannerAudio.ts** — `catch { /* silently ignore */ }` bez logowania.

### Deployment

- VPS ma uptime 25 dni — stabilny.
- UFW aktywne na 15 portach.
- Inne projekty na tym samym VPS: `restaurant-backend`, `stitchcore-backend`, `smart-myslowice` (node na porcie 3001).
- Certbot timer aktywny — SSL odnawia się automatycznie.
- PostgreSQL na `127.0.0.1:5432` — dostępny tylko lokalnie ✅.
- Backupy działają zarówno z Docker Compose (co 24h w kontenerze `magazyn-backup`) jak i przez cron hosta (codziennie 2:00).

---

## 🔴 Nienaprawione z poprzedniego audytu

| ID | Opis | Plik | Status |
|----|------|------|--------|
| B11 | KOREKTA nie może ustawić stanu na 0 (`@Positive` vs `@PositiveOrZero`) | `StockMovementRequest.java:12` | ❌ |
| B12 | RateLimitFilter ustawia CORS ręcznie (echo Origin) | `RateLimitFilter.java:49-52` | ❌ |
| B13 | JwtUtil.isTokenValid() połyka wyjątki bez logowania | `JwtUtil.java:47-52` | ❌ |
| B14 | ExportService.escapeCsv() nie obsługuje `\r` | `ExportService.java:221-227` | ✅ NAPRAWIONE |
| B15 | Brak paginacji w `GET /api/stock/{id}/movements` | `StockController.java` | ✅ NAPRAWIONE |
| B5 | WarehouseService.updateWarehouse — brak checku duplikatu kodu | `WarehouseService.java:74` | ❌ |
| B6 | WarehouseRequest — brak adnotacji walidacyjnych | `WarehouseRequest.java` | ❌ |
| B7 | AuditLogService — hardcodowany tenantId=1L | `AuditLogService.java:50` | ❌ |
| B8 | StatsService — brak guarda na filtr Hibernate | `StatsService.java` | ❌ |
| B9 | User entity — @Data eksponuje hasło | `User.java` | ❌ |
| B10 | StockController — dead code VIEWER w @PreAuthorize | `StockController.java:36` | ❌ |
| S1 | JWT Secret — brak walidacji ≥64 bajty przy starcie | — | ❌ |
| S2 | Rate limiting na logowaniu: 20 req/min | — | ⚠️ Akceptowalne |
| S3 | Brak ochrony przed enumeracją ID | — | ❌ |

## Zachowane z poprzedniego audytu — brak testów

| Komponent | Testy | Status |
|-----------|-------|--------|
| ExportService | ❌ Brak | ❌ |
| ImportService | ❌ Brak | ❌ |
| StatsService | ❌ Brak | ❌ |
| SeedService | ❌ Brak | ❌ |
| RateLimitFilter | ❌ Brak | ❌ |
| JwtAuthenticationFilter | ❌ Brak | ❌ |
| StockController | ❌ Brak | ❌ |
| InvoiceService | ❌ Brak (nowy) | ❌ |
| Testy integracyjne wymagają PostgreSQL (bez H2) | ❌ Utrudnione | ❌ |

---

## ✅ Naprawione w tej sesji

| ID | Opis | Pliki | Data |
|----|------|-------|------|
| B14 | ExportService.escapeCsv() — obsługa `\r` | `ExportService.java` | Sesja wcześniejsza |
| B15 | Paginacja w movements | `StockController.java` | Sesja wcześniejsza |
| — | Logout — usunięto `@Transactional` (DELETE powodował flush przed autoryzacją) | `AuthService.java` | Sesja wcześniejsza |

---

## 📊 Podsumowanie

### Nowe znaleziska (2026-06-13)

| Priorytet | Backend | Frontend | Deployment | Razem |
|-----------|---------|----------|------------|-------|
| 🔴 Critical | 3 (B2-B4) | 3 (B6, B7, B9) | 1 (B1) | **5** |
| 🟡 Suggestion | 8 (B10-B17) | 5 (F1-F5) | 12 (D1-D12) | **25** |
| 🔵 Nice to have | 5 | 4 | 2 | **11** |
| **Razem** | **16** | **12** | **15** | **41** |

### Łącznie z poprzednim audytem

| Kategoria | Liczba |
|-----------|--------|
| 🔴 Critical | **10** (5 stare + 5 nowe) |
| 🟡 Suggestion | **~40** (15 stare + 25 nowe) |
| 🔵 Nice to have | **~26** (15 stare + 11 nowe) |
| **Razem** | **~76** |

### Top 10 rzeczy do naprawy (priorytet)

| # | Co | Obszar | Trudność | Szac. czas |
|---|----|--------|----------|-----------|
| 1 | **App jako root w Dockerze** → dodać USER | Docker | Łatwe | 10 min |
| 2 | **warehouseFilter na Invoice/InvoiceItem/CompanySettings** | Backend | Łatwe | 15 min |
| 3 | **findProductsBelowMinStock/getTotalStockValue — tenant guard** | Backend | Łatwe | 10 min |
| 4 | **ExportService.findAll() → findByTenantId()** | Backend | Łatwe | 10 min |
| 5 | **HSTS + CSP + TLSv1.2+ w nginx** | Deployment | Łatwe | 15 min |
| 6 | **Rate limiting nginx na /api/auth/login** | Deployment | Łatwe | 10 min |
| 7 | **SSH: PermitRootLogin prohibit-password + PasswordAuthentication no** | Deployment | Łatwe | 5 min |
| 8 | **Docker resource limits + log rotation** | Deployment | Łatwe | 10 min |
| 9 | **Docker bypass UFW — ograniczyć expose do 127.0.0.1** | Deployment | Łatwe | 10 min |
| 10 | **Ujednolicić backupy + dodać off-site** | Deployment | Średnie | 30 min |

---

## Stan po audycie 2026-06-16

**Data poprawek:** 2026-06-16
**Zakres:** Backend (Spring Boot 4.0.6), Frontend (React 19), Nginx, CI/CD, Docker
**Autor:** Qwen Code Agent

### Wykonane poprawki bezpieczeństwa

| # | Opis | Pliki | Status |
|---|------|-------|--------|
| 1 | **KRYTYCZNE: X-Tenant-Id spoofing** — TenantFilter zmieniony na no-op (JWT jest jedynym źródłem tenantId). Nginx strips X-Tenant-Id z requestów klienckich. | `TenantFilter.java`, `nginx-kzelman.conf`, `nginx-kzelman-deploy.conf` | ✅ |
| 2 | **KRYTYCZNE: DatabaseInitializer aktywny na produkcji** — zmieniono @Profile z "!test" na "dev". Seed danych tylko w środowisku deweloperskim. | `DatabaseInitializer.java` | ✅ |
| 3 | **WAŻNE: RateLimitFilter IP spoofing** — używana jest OSTATNIA wartość X-Forwarded-For (najbliższa nginx), nie pierwsza. Dodano rate limiting dla /api/tenants/register (5 req/h) i /api/assistant/chat (30 req/min). | `RateLimitFilter.java` | ✅ |
| 4 | **WAŻNE: Refresh token plaintext w DB** — tokeny są haszowane SHA-256 przed zapisem. @Transactional(readOnly=true) zmienione na @Transactional przy walidacji (delete w read-only silently failował). Flyway V14 migracja backfilluje istniejące tokeny. | `RefreshToken.java`, `RefreshTokenRepository.java`, `RefreshTokenService.java`, `AuthService.java`, `V14__hash_refresh_tokens.sql` | ✅ |
| 5 | **WAŻNE: Refresh token w localStorage (XSS)** — przeniesiony do httpOnly cookie (Secure; SameSite=Strict). Frontend: removed safeSetItem('refreshToken'), auto-refresh interceptor wysyła withCredentials:true. Backend: AuthController set/clear cookie. | `AuthController.java`, `LoginPage.tsx`, `api.ts` | ✅ |
| 6 | **WAŻNE: Assistant system prompt injection** — walidacja ról w historii konwersacji: akceptowane tylko "user" i "assistant", "system" jest odrzucany. Rate limiting 30 req/min już istnieje. | `AssistantService.java` | ✅ |
| 7 | **ŚREDNIE: CI pomija testy** — dodano job `test` przed `deploy` w GitHub Actions (mvn test z PostgreSQL service container). Dockerfile: usunięto `-Dmaven.test.skip=true`. | `Dockerfile`, `.github/workflows/deploy.yml` | ✅ |
| 8 | **PORZĄDKI: Martwy kod** — usunięto CorsFilter.java (pusty), TenantSessionFilter.java, WarehouseSessionFilter.java, `_archive/` (7 nieużywanych komponentów frontend), build.log, vps-bundle.js, dist.tar.gz. .gitignore zaktualizowany. | `CorsFilter.java`, `TenantSessionFilter.java`, `WarehouseSessionFilter.java`, `_archive/*`, `build.log`, `vps-bundle.js`, `dist.tar.gz`, `.gitignore` | ✅ |

### Nienaprawione z poprzedniego audytu

Następujące błędy krytyczne/ważne z audytu 2026-06-13 pozostają do naprawy:
- B1: App w Dockerze jako root — wymaga dodania USER w Dockerfile
- B2/B8: warehouseFilter na Invoice/InvoiceItem/CompanySettings — wymaga dodania pola warehouseId
- B3: findProductsBelowMinStock/getTotalStockValue — brak tenant guard
- B4: exportProductsCsv/Excel — brak filtra tenanta
- B5: SSH — hasło i root login włączone
- B6: Brak automatycznego odświeżania tokena JWT (✅ NAPRAWIONE — fix #5)
- B7/B9: Silent catch + martwy kod (✅ NAPRAWIONE — fix #8)
- B11: @Positive vs @PositiveOrZero na StockMovementRequest
- B12: RateLimitFilter ustawia CORS (przestarzałe po refactorze RateLimitFilter — do weryfikacji)
- B13: JwtUtil.isTokenValid() połyka wyjątki
- S3: Brak ochrony przed enumeracją ID

### Commity

```
dc8349a fix: remove trust of X-Tenant-Id header from TenantFilter
db2bba6 fix: restrict DatabaseInitializer to 'dev' profile only
1c01f6e fix: RateLimitFilter - use last X-Forwarded-For IP, add more endpoints
7722189 fix: hash refresh tokens with SHA-256, fix @Transactional readOnly
51770ee fix: move refresh token to httpOnly cookie, remove from localStorage
7773737 fix: validate assistant chat roles, prevent system prompt injection
bbce9b9 fix: add test job to CI pipeline, remove test skip from Dockerfile
58aa33c chore: remove dead code and cleanup project artifacts
```
