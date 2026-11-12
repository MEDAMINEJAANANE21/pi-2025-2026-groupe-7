# Multi-stage Dockerfile for production deployment
# This builds Angular app and serves it through Spring Boot

# Stage 1: Build Angular application
FROM node:20-alpine AS frontend-build

WORKDIR /frontend

# Copy package files
COPY client/package*.json ./

# Install dependencies
RUN npm ci

# Copy frontend source
# Copy frontend source
COPY client/ ./
# Build Angular app for production
RUN npm run build

# Stage 2: Build Spring Boot application
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

# Copy Angular build artifacts to Spring Boot static resources
# The frontend build runs in /frontend and outputs to dist/browser (see client/angular.json).
# Copy that output into Spring Boot's static resources so the jar serves the frontend.
COPY --from=frontend-build /frontend/dist/browser/browser ./backend/server/src/main/resources/static

# Build Spring Boot application
RUN mvn clean package -DskipTests

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar from build stage
COPY --from=backend-build /app/backend/server/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Runtime environment variables with sensible defaults. These can be overridden
# at docker run time with `-e` or in compose files.
ENV SPRING_PROFILES_ACTIVE=prod \
	PORT=8080 \
	JAVA_OPTS=""

# Run the application. Use `sh -c` so shell expands the env vars and allows
# passing additional JVM args via JAVA_OPTS. Also forward ORS_KEY as a system
# property (-Dors.key=...) so the JVM app can read it as a property if needed.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
