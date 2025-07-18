# Build
FROM maven:3.9.10-amazoncorretto-24-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests


FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
COPY --from=build /app/target/rinha-backend-2025-0.0.1-SNAPSHOT.jar app.jar
COPY compile-commands.txt ./compile-commands.txt
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar > ./jit.log 2>&1"]
