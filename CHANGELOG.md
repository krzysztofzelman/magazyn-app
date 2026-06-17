# Changelog

## [2026-06-17] — Java 25 LTS + CI naprawa

### Added
- Java 25 LTS: aktualizacja `pom.xml`, `Dockerfile`, CI workflow
- Profile Maven: `docker-build` (skipTests) i `ci` (test.groups)
- `CHANGELOG.md` — formalne śledzenie zmian

### Changed
- **Java 25 LTS**: `java.version` 17 → 25, wszystkie obrazy Docker `eclipse-temurin:25`
- **CI/CD**: GitHub Actions `actions/setup-java@v4` z `java-version: '25'`
- **README**: zaktualizowany badge Java 17 → 25, zaktualizowany opis ostatniego deployu
- **pom.xml**: dodano `maven.compiler.source` i `maven.compiler.target` ustawione na 25

### Fixed
- `RefreshTokenServiceTest` — używa `rotate()` zamiast usuniętego `validateRefreshToken()` (commit 5457786)

### Security
- Java 25 LTS: Virtual Threads, Pattern Matching, Record Patterns, Vector API

## [2026-06-15] — Audyt i naprawa izolacji wielodzierżawczej

### Added
- Pełna izolacja `tenant_id` we wszystkich repozytoriach (@Query z jawnym parametrem)
- `HandlerInterceptor` z `@PersistenceContext` zamiast `TenantSessionFilter`
- Seed data dla testów integracyjnych (tenants, users, warehouse)

### Changed
- 18 serwisów przekazuje `TenantContext.getTenantId()` do repozytoriów
- `@Scheduled releaseExpired()` iteruje wszystkich aktywnych najemców

### Fixed
- Cross-tenant data leak przez `findById()` (Hibernate @Filter nie działał)
- 8 endpointów zwracających HTTP 500
- Flyway V1 migracja i konfiguracja testów
- RateLimitFilter: Caffeine cache zamiast ConcurrentHashMap

## [2026-06-13] — Wstępny audyt projektu

### Added
- Testy frontendu (ThemeToggle, ErrorBoundary)
- Rotacja refresh tokenów z detekcją reuse
- V1__init.sql z pełnym schematem bazy

### Changed
- JWT token przeniesiony z localStorage do pamięci (ochrona XSS)
- Docker compose: PostgreSQL 18 → 16 z bind mount

### Fixed
- Webflux vs MVC conflict (servlet stack w testach)
- Testcontainers BOM dla spójności wersji
