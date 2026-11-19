@echo off
chcp 65001 >nul
title TECNISECURE - Iniciar con Docker

cd /d "%~dp0"

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] No se encontro Docker.
    echo         Instala Docker Desktop: https://www.docker.com/products/docker-desktop/
    echo         Despues de instalarlo, reinicia el equipo.
    echo.
    pause
    exit /b 1
)

echo =====================================================================
echo             TECNISECURE - API REST + Sitio web (Docker)
echo =====================================================================
echo.
echo [1/2] Construyendo la imagen (la primera vez tarda unos minutos)...
echo.

docker compose up --build -d
if errorlevel 1 (
    echo.
    echo [ERROR] No se pudo arrancar. Asegurate de que Docker Desktop esta abierto
    echo         y muestra el estado "Engine running".
    echo.
    pause
    exit /b 1
)

echo.
echo [2/2] Esperando a que la aplicacion responda...
echo.

set /a intentos=0
:esperar
set /a intentos+=1
curl -s -f -o nul http://localhost:8080/actuator/health
if not errorlevel 1 goto listo
if %intentos% geq 40 goto lento
timeout /t 3 /nobreak >nul
goto esperar

:listo
echo.
echo =====================================================================
echo   Listo. Abriendo el navegador...
echo.
echo   Sitio web:  http://localhost:8080
echo   Swagger UI: http://localhost:8080/swagger-ui.html
echo   Consola H2: http://localhost:8080/h2-console
echo.
echo   Para detener:  docker compose down
echo =====================================================================
start "" http://localhost:8080
echo.
pause
exit /b 0

:lento
echo.
echo La aplicacion aun no responde. Mira los registros con:
echo   docker compose logs -f
echo.
pause
exit /b 0
