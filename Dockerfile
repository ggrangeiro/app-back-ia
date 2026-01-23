# Stage 1: Build with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . /home/app
WORKDIR /home/app
RUN mvn clean package -DskipTests

# Stage 2: Run with JRE
FROM eclipse-temurin:21-jre-jammy

# Install system dependencies for Playwright/Chromium
RUN apt-get update && apt-get install -y \
    libnss3 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /home/app/target/exercicio-0.1.jar app.jar
EXPOSE 8080

# Playwright will download browsers on first run, or we can pre-download them
# To pre-download, we need the maven build to run a playwright command or use a layer
# For now, we allow it to download, but it might be slow on first request.
# Better: Run playwright install during build if possible, but we need the jar.
# Let's add a command to install browsers in the final image if needed.

ENTRYPOINT ["java", "-Dmicronaut.server.host=0.0.0.0", "-jar", "app.jar"]
