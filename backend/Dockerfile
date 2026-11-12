# Multi-stage Dockerfile for backend-only deployment
# Builds Spring Boot backend modules and runs the server jar

# Stage 1: Build Spring Boot application
# Backend is a multi-module Maven project (root -> backend -> server + rest-api)
FROM maven:3-eclipse-temurin-21 AS backend-build

WORKDIR /app

# Copy Maven POM files for all modules (enables dependency caching)
COPY pom.xml ./
COPY backend/pom.xml ./backend/
COPY backend/rest-api/pom.xml ./backend/rest-api/
COPY backend/server/pom.xml ./backend/server/

# Download dependencies (this layer will be cached)
RUN mvn dependency:go-offline -B

# Copy backend source
COPY backend/rest-api/src ./backend/rest-api/src
COPY backend/server/src ./backend/server/src

# Build Spring Boot application
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=backend-build /app/backend/server/target/*.jar app.jar

EXPOSE 8080

# Runtime environment variables with sensible defaults. These can be overridden
# at docker run time with `-e` or in compose files.
ENV SPRING_PROFILES_ACTIVE=prod \
	PORT=8080 \
	JAVA_OPTS=""

# Run the application. Use `sh -c` so shell expands the env vars and allows
# passing additional JVM args via JAVA_OPTS.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
