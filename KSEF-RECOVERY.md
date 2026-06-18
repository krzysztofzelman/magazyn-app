# KSeF Recovery Plan

## 1. Sesja wygasła

**Objawy:**
- W panelu KSeF: "Brak aktywnej sesji"
- W logach: `Brak aktywnej sesji KSeF. Zainicjuj sesję.`

**Rozwiązanie:**
```bash
# 1. Sprawdź stan sesji
curl http://localhost:8080/actuator/health/ksef

# 2. Zainicjuj nową sesję (jako ADMIN)
curl -X POST http://localhost:8080/api/ksef/admin/session/init \
  -H "Authorization: Bearer $TOKEN"

# 3. Zweryfikuj
curl http://localhost:8080/api/ksef/admin/dashboard \
  -H "Authorization: Bearer $TOKEN"
```

## 2. Błąd API KSeF

**Objawy:**
- W logach: `KSeFCommunicationException`
- Status faktury: ERROR

**Rozwiązanie:**
```bash
# 1. Sprawdź logi backendu
docker compose logs app | grep -i ksef

# 2. Sprawdź metryki Prometheus
curl http://localhost:9090/api/v1/query?query=rate(ksef_errors_total[5m])

# 3. Sprawdź status endpointu KSeF
curl https://ksef-test.mf.gov.pl/api/v1/status  # lub produkcyjny URL
```

## 3. Błąd walidacji faktury

**Objawy:**
- Status 400: VALIDATION_ERROR
- Faktura nie zostaje wysłana

**Rozwiązanie:**
1. Sprawdź logi walidacji: `docker compose logs app | grep -i validation`
2. Popraw dane faktury (NIP, kwoty, daty)
3. Wyślij ponownie przez panel KSeF

## 4. Awaria systemu KSeF (Ministerstwo Finansów)

**Objawy:**
- Timeouty na wszystkich żądaniach
- 502 Bad Gateway z backendu

**Rozwiązanie:**
```bash
# 1. Włącz tryb offline — faktury będą kolejkowane automatycznie
# (system automatycznie retransmituje z backoffem 3 próby)

# 2. Sprawdź czy awaria jest po stronie MF:
curl -I https://ksef.mf.gov.pl

# 3. Monitoruj metryki:
curl http://localhost:9090/api/v1/query?query=ksef_active_sessions
```

## 5. Problemy z wydajnością

**Objawy:**
- Wysoki czas odpowiedzi (>2s)
- W logach: `KSeF send failed, retrying`

**Rozwiązanie:**
```bash
# 1. Sprawdź JVM memory
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# 2. Sprawdź Prometheus alerts
curl http://localhost:9090/api/v1/alerts

# 3. Zwiększ limity w docker-compose jeśli potrzeba
# mem_limit: 512m → 1024m
```

## 6. Problemy z bazą danych

**Objawy:**
- `org.postgresql.util.PSQLException`
- Flyway migration errors

**Rozwiązanie:**
```bash
# 1. Sprawdź status bazy
docker compose exec postgres pg_isready

# 2. Sprawdź Flyway migracje
docker compose exec app curl http://localhost:8080/actuator/flyway 2>/dev/null || echo "Flyway endpoint not exposed"

# 3. Restart całego stacku
docker compose down
docker compose up -d
```

## 7. Alerty i powiadomienia

Monitoruj następujące alerty w Grafanie:
- **KSeFHighErrorRate** → więcej niż 5 błędów/min
- **KSeFNoActiveSession** → brak aktywnej sesji > 5 min
- **KSeFHighResponseTime** → p95 > 2s
- **KSeFRetryRateHigh** > 10 retransmisji/min
- **AppDown** → aplikacja niedostępna > 1 min

## Dane kontaktowe

- **Admin systemu**: [admin email]
- **Helpdesk KSeF MF**: +48 22 345 67 89
- **Status KSeF**: https://www.ksef.mf.gov.pl/status
