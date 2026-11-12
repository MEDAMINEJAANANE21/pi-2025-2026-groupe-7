# Image de production unique : le jar Spring Boot sert aussi l'application Angular.

# 1. Frontend
FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY client/package*.json ./
RUN npm ci
COPY client/ ./
RUN npm run build

# 2. Backend (projet Maven multi-module : racine → backend → rest-api + server)
FROM maven:3-eclipse-temurin-21 AS backend-build
WORKDIR /app
# Les pom.xml d'abord : la couche des dépendances reste en cache tant qu'ils ne changent pas.
COPY pom.xml ./
COPY backend/pom.xml ./backend/
COPY backend/lombok.config ./backend/
COPY backend/rest-api/pom.xml ./backend/rest-api/
COPY backend/server/pom.xml ./backend/server/
RUN mvn -B dependency:go-offline
COPY backend/rest-api/src ./backend/rest-api/src
COPY backend/server/src ./backend/server/src
COPY --from=frontend-build /frontend/dist/browser/browser ./backend/server/src/main/resources/static
RUN mvn -B clean package -DskipTests

# 3. Exécution
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/server/target/server-*.jar app.jar
EXPOSE 8080
ENV PORT=8080 \
    JAVA_OPTS=""
# sh -c laisse le shell développer JAVA_OPTS et PORT au démarrage.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
