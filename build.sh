#!/bin/bash

# Build script for Drifter Java application
set -e

echo "Building Drifter Java application..."

# Clean and compile
mvn clean compile

# Run tests
echo "Running tests..."
mvn test

# Package the application
echo "Packaging application..."
mvn package

echo "Build complete! JAR file created at: target/drifter-1.0.0.jar"
echo ""
echo "To run the application:"
echo "  java -jar target/drifter-1.0.0.jar -c config.json -f table"
echo ""
echo "Or use Maven:"
echo "  mvn exec:java -Dexec.mainClass=\"com.example.drifter.DrifterApplication\" -Dexec.args=\"-c config.json -f table\""
