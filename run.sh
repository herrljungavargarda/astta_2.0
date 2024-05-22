#!/bin/bash

# Navigate to the project directory (optional, if needed)

# Run the Maven command
mvn compile exec:java

# Check if the command was successful
if [ $? -eq 0 ]; then
  echo "Application ran successfully."
else
  echo "Failed to run the application."
  exit 1
fi