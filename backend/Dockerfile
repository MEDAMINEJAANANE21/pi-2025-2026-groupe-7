# Image du backend seul, utilisée derrière NGINX par docker-compose.yml.

FROM maven:3-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY pom.xml ./
COPY backend/pom.xml ./backend/
COPY backend/lombok.config ./backend/
COPY backend/rest-api/pom.xml ./backend/rest-api/
COPY backend/server/pom.xml ./backend/server/
RUN mvn -B dependency:go-offline
COPY backend/rest-api/src ./backend/rest-api/src
COPY backend/server/src ./backend/server/src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=backend-build /app/backend/server/target/server-*.jar app.jar
EXPOSE 8080
ENV PORT=8080 \
    JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=$PORT -jar app.jar"]
