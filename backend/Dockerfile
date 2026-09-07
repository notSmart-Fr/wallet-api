# ==============================================================================
# Stage 1: Build Phase (JDK 25 + Maven Wrapper)
# ==============================================================================
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /app

# Copy Maven configuration and wrapper files first to leverage Docker layer caching
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Download dependencies (cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B

# Copy application source code
COPY src ./src

# Build production JAR without running tests
RUN ./mvnw clean package -DskipTests

# ==============================================================================
# Stage 2: Runtime Phase (Slim JRE 25)
# ==============================================================================
FROM eclipse-temurin:25-jre-alpine AS runner
WORKDIR /app

# Create a non-root user for security best practices
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy compiled JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose Spring Boot default web port
EXPOSE 8080

# Configure JVM options for container memory limits & Virtual Threads
ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]