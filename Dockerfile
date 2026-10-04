# Backend image (multi-stage). Build context: repository root.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY backend/pom.xml .
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN useradd --system --create-home bookforward && mkdir -p /data/uploads && chown -R bookforward /data
USER bookforward
WORKDIR /app
COPY --from=build /src/target/bookforward-backend-1.0.0.jar app.jar
ENV STORAGE_LOCAL_DIR=/data/uploads
EXPOSE 9090
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s CMD wget -qO- http://localhost:9090/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
