FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

COPY target/rinha-backend-2025-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]