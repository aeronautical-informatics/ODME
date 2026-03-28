#!/bin/bash

# Function to check if Java is installed
check_java() {
    if ! command -v java &>/dev/null; then
        echo "Java is not installed. Installing Java..."
        sudo apt-get update
        sudo apt-get install -y openjdk-17-jre
    fi
}

# Check if Java is installed
check_java

# Run the JAR file
java -jar SESEditor-1.0-SNAPSHOT-jar-with-dependencies.jar
