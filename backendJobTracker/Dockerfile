# syntax=docker/dockerfile:1
# =====================================================
# Місце: backendJobTracker/Dockerfile
# =====================================================
# Production multi-stage build для Spring Boot backend.
# Не для dev — у dev використовуй `./mvnw spring-boot:run` (швидко, з DevTools hot reload).
#
# Three stages:
#  1. builder   — компіляція з Maven (JDK)
#  2. extractor — розпакування layered JAR (JRE)
#  3. runtime   — мінімальний final image з extracted layers
#
# Layered jar дає кращий Docker layer caching:
# при зміні коду тільки `application/` layer перебудовується (~1MB),
# а не весь fat JAR (~200MB з усіма deps).

# ============= Stage 1: Build =============
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Спочатку Maven wrapper і pom.xml — для кешування dependencies layer.
# Поки pom.xml не змінюється, Docker reuse'ить кешований layer
# і не качає 200MB+ deps заново при кожній зміні коду.
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# На Windows mvnw може втратити executable bit при git clone — фіксимо
RUN chmod +x mvnw

# Pre-download залежностей. Cache mount переживає окремі docker build
# запуски — retry після мережевого збою не тягне все з нуля.
RUN --mount=type=cache,id=maven-repo,target=/root/.m2/repository ./mvnw -B dependency:go-offline

# Тепер source. Зміни в src/ НЕ invalidate'нуть deps layer вище.
COPY src ./src

# Package. Tests у CI окремо, тут skip.
# -B = batch mode (без інтерактивних промптів)
RUN --mount=type=cache,id=maven-repo,target=/root/.m2/repository ./mvnw -B clean package -DskipTests \
    && cp target/*.jar app.jar

# ============= Stage 2: Extract layered JAR =============
FROM eclipse-temurin:21-jre-alpine AS extractor

WORKDIR /builder

COPY --from=builder /build/app.jar app.jar

# Spring Boot 3.2+: `tools` jarmode (раніше було `layertools` — застаріле).
# destination має бути окремою порожньою підпапкою — сам app.jar лежить
# поруч у /builder, і extract відмовляється писати в непорожню директорію.
# --launcher обов'язковий: без нього tools jarmode не витягує loader-класи
# (org.springframework.boot.loader.*) в окремий шар, а пакує app у thin jar
# з Class-Path у lib/ — несумісно з ENTRYPOINT через JarLauncher нижче.
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# ============= Stage 3: Runtime =============
FROM eclipse-temurin:21-jre-alpine

# Non-root user — security best practice. Якщо процес скомпрометований —
# атакуючий не root в контейнері.
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# COPY у порядку від найрідше-змінних до найчастіше-змінних.
# При зміні коду тільки останній layer (application) перебудовується.
COPY --from=extractor --chown=spring:spring /builder/extracted/dependencies/ ./
COPY --from=extractor --chown=spring:spring /builder/extracted/spring-boot-loader/ ./
COPY --from=extractor --chown=spring:spring /builder/extracted/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /builder/extracted/application/ ./

USER spring

EXPOSE 8080

# JVM tuning для контейнерної середи.
# JAVA_TOOL_OPTIONS автоматично підбирається JVM — не треба пасити в ENTRYPOINT.
#
# - MaxRAMPercentage=75: 75% memory limit контейнера (cgroup-aware)
# - ExitOnOutOfMemoryError: fail-fast при OOM, краще ніж zombie процес
# - HeapDumpOnOutOfMemoryError + HeapDumpPath: автоматичний heap dump для post-mortem
# - UseG1GC: явно (хоча default на Java 21)
# - egd file:/dev/./urandom: швидший startup (entropy source)
# - timezone UTC: timestamps в логах завжди UTC, frontend конвертить для UI
ENV JAVA_TOOL_OPTIONS="\
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -XX:+UseG1GC \
    -Djava.security.egd=file:/dev/./urandom \
    -Duser.timezone=UTC"

# Healthcheck через actuator. Alpine busybox містить wget з --spider.
# start-period 60s — Spring Boot стартує не миттєво (Flyway, JPA init).
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Spring Boot 3.2+ launcher: org.springframework.boot.loader.launch.JarLauncher
# (старий шлях org.springframework.boot.loader.JarLauncher не працює в 3.2+).
# Exec form (JSON array) → Java отримує SIGTERM від Docker для graceful shutdown.
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]