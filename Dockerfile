FROM ubuntu:latest
LABEL authors="adeutou"

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the current working directory (GitHub Actions workspace) to the container
COPY *.jar app.jar

# Expose the port your application will listen on (if applicable)
EXPOSE 8080

# Run the Java application
CMD ["java", "-jar", "app.jar"]