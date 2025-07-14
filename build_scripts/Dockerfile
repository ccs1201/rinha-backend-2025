# Build
FROM maven:3.9.10-eclipse-temurin-24-alpine AS build
WORKDIR /app
COPY ../pom.xml .
COPY ../src ./src
RUN mvn clean package -DskipTests


FROM container-registry.oracle.com/graalvm/jdk:24
WORKDIR /app
COPY --from=build /app/target/rinha-backend-2025-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]