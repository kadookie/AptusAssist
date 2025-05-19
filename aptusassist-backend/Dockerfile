# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/data && chmod -R 777 /app/data
COPY --from=build /app/target/aptusassist-backend-1.0.0.jar app.jar
EXPOSE ${SERVER_PORT:-9090}
ENTRYPOINT ["java", "-jar", "app.jar"]