# Multi-stage build for ETL Framework

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom files
COPY pom.xml .
COPY etl-api/pom.xml etl-api/
COPY etl-core/pom.xml etl-core/
COPY etl-connectors/pom.xml etl-connectors/
COPY etl-operators/pom.xml etl-operators/
COPY etl-scheduler/pom.xml etl-scheduler/
COPY etl-executor/pom.xml etl-executor/
COPY etl-state/pom.xml etl-state/
COPY etl-checkpoint/pom.xml etl-checkpoint/
COPY etl-metrics/pom.xml etl-metrics/
COPY etl-web/pom.xml etl-web/
COPY etl-starter/pom.xml etl-starter/

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY etl-api/src etl-api/src
COPY etl-core/src etl-core/src
COPY etl-connectors/src etl-connectors/src
COPY etl-operators/src etl-operators/src
COPY etl-scheduler/src etl-scheduler/src
COPY etl-executor/src etl-executor/src
COPY etl-state/src etl-state/src
COPY etl-checkpoint/src etl-checkpoint/src
COPY etl-metrics/src etl-metrics/src
COPY etl-web/src etl-web/src
COPY etl-starter/src etl-starter/src

# Build application
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="etl-framework-team"
LABEL description="Reactive ETL Framework"
LABEL version="1.0.0-SNAPSHOT"

# Set working directory
WORKDIR /app

# Create data directories
RUN mkdir -p /data/checkpoints /var/log/etl-framework

# Copy JAR from build stage
COPY --from=build /app/etl-starter/target/etl-starter-*.jar /app/etl-framework.jar

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SPRING_PROFILES_ACTIVE=prod

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/etl-framework.jar"]
