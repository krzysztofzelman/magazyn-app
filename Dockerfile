# ============================================================
# Stage 1: Build with Maven
# ============================================================
FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /app

# Copy only pom.xml first for dependency caching
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -q || true

# Copy source and build
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -q

# ============================================================
# Stage 2: Runtime image
# ============================================================
FROM eclipse-temurin:25-jre AS runtime

# Install curl (needed for Docker HEALTHCHECK)
RUN apt-get update && \
    apt-get install -y curl --no-install-recommends && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    curl --version >/dev/null 2>&1

# Create non-root user
RUN groupadd -r appuser && \
    useradd -r -g appuser -d /app appuser

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

# Set ownership
RUN chown -R appuser:appuser /app

USER appuser

EXPOSE 8080

# Healthcheck — uses curl that was installed above
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=5 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
