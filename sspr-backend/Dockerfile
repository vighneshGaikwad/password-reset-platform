# Stage 1: Build the application using Temurin OpenJDK 17
FROM eclipse-temurin:17-jdk-jammy AS builder

LABEL stage="builder"

WORKDIR /app

# Copy Maven wrapper scripts and configuration
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Grant executable permission to Maven wrapper
RUN chmod +x ./mvnw

# Copy application source code
COPY src ./src

# Build production JAR without executing unit tests during container build
RUN ./mvnw clean package -DskipTests

# Stage 2: Lightweight runtime image using Temurin JRE 17
FROM eclipse-temurin:17-jre-jammy AS runtime

# Metadata labels
LABEL maintainer="DevSecOps Team" \
      version="1.0.0" \
      description="Self-Service Password Reset (SSPR) Backend Spring Boot Service"

# Install curl for container health check probes
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

# Security: Create non-root system group and user
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

WORKDIR /app

# Copy executable JAR artifact from builder stage
COPY --from=builder /app/target/sspr-backend-*.jar app.jar

# Set ownership of application files to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Set active profile to docker by default
ENV SPRING_PROFILES_ACTIVE=docker
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Expose backend service port
EXPOSE 8080

# Configure container health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/admin/dashboard/summary || exit 1

# Launch the Spring Boot application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
