FROM maven:3.9.6-eclipse-temurin-17 AS build

LABEL maintainer="adeutou"

# Set working directory inside container
WORKDIR /app

# Copy source code
COPY . .

# Package the application using Maven
RUN mvn clean install

# Stage 2 - runtime image
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy built jar from build stage
COPY --from=build /app/target/*-jar-with-dependencies.jar app.jar


EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
