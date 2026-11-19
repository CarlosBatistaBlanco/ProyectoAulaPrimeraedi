@echo off
chcp 65001 >nul
title TECNISECURE - Iniciar API (Spring Boot)

cd /d "%~dp0backend"
set "JAVA_HOME=C:\Program Files\Java\jdk-22"
set "PATH=C:\Program Files\Java\jdk-22\bin;C:\Users\X1605ZA-MB013W\.maven\maven-3.9.16\bin;%PATH%"
set "MAVEN_HOME=C:\Users\X1605ZA-MB013W\.maven\maven-3.9.16"

echo =====================================================================
echo                 TECNISECURE - API REST (Spring Boot)
echo =====================================================================
echo.

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] No se encontro Java. Instala JDK 21 y agregalo al PATH.
    echo         https://adoptium.net/temurin/releases/?version=21
    echo.
    pause
    exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] No se encontro Maven. Instala Apache Maven y agregalo al PATH.
    echo         https://maven.apache.org/download.cgi
    echo.
    pause
    exit /b 1
)

echo [1/2] Compilando y arrancando la aplicacion...
echo       (la primera vez descarga dependencias, puede tardar unos minutos)
echo.
echo =====================================================================
echo   Sitio web:    http://localhost:8080
echo   Swagger UI:   http://localhost:8080/swagger-ui.html
echo   Consola H2:   http://localhost:8080/h2-console
echo   (Ctrl + C para detener)
echo =====================================================================
echo.

start "" http://localhost:8080

call mvn -B spring-boot:run

echo.
echo La aplicacion se detuvo.
pause
