# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn clean package -DskipTests

# Stage 2: Run with JRE
FROM eclipse-temurin:21-jre-jammy
COPY --from=build /home/app/target/exercicio-0.1.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Dmicronaut.server.port=8080", "-jar", "app.jar"]
