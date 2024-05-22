@echo off

REM Navigate to the project directory (optional, if needed)

REM Run the Maven command
mvn compile exec:java

REM Check if the command was successful
IF %ERRORLEVEL% EQU 0 (
    echo Application ran successfully.
) ELSE (
    echo Failed to run the application.
    exit /b 1
)