# syntax=docker/dockerfile:1.7

# --- Stage 1: build the layered jar ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Warm the local Maven cache before copying src so unchanged pom.xml = cached deps.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package && \
    java -Djarmode=layertools -jar target/claims-backend-0.0.1-SNAPSHOT.jar extract --destination /layered

# --- Stage 2: runtime ---
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd -r claims && useradd -r -g claims -u 1000 claims && \
    mkdir -p /app /app/uploads /app/logs && \
    chown -R 1000:claims /app
USER 1000:claims
WORKDIR /app

# Copy layered jar contents in the order that maximises cache reuse.
COPY --from=build --chown=1000:claims /layered/dependencies/ ./
COPY --from=build --chown=1000:claims /layered/spring-boot-loader/ ./
COPY --from=build --chown=1000:claims /layered/snapshot-dependencies/ ./
COPY --from=build --chown=1000:claims /layered/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
