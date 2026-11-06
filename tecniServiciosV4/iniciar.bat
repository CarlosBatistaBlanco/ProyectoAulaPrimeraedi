@echo off
chcp 65001 >nul
title TECNISECURE - Inicializador del Proyecto

:: Asegurar que el directorio de trabajo sea la carpeta del script
cd /d "%~dp0"

echo =====================================================================
echo                TECNISECURE - SISTEMA DE VIGILANCIA
echo             Inicializador automatico de entorno y servidor
echo =====================================================================
echo.

:: 1. DETECTAR PHP
set "PHP_CMD="
where php >nul 2>&1
if %errorlevel% equ 0 (
    set "PHP_CMD=php"
    goto PHP_LISTO
)

if exist "C:\xampp\php\php.exe" (
    set "PHP_CMD=C:\xampp\php\php.exe"
    goto PHP_LISTO
)

if exist "D:\xampp\php\php.exe" (
    set "PHP_CMD=D:\xampp\php\php.exe"
    goto PHP_LISTO
)

if exist "E:\xampp\php\php.exe" (
    set "PHP_CMD=E:\xampp\php\php.exe"
    goto PHP_LISTO
)

:PHP_LISTO
if "%PHP_CMD%"=="" (
    echo [ERROR] No se encontro PHP en el sistema ni en C:\xampp\php.
    echo Por favor instala XAMPP o anade PHP a tu variable de entorno PATH.
    echo.
    pause
    exit /b 1
)

echo [1/4] PHP detectado: %PHP_CMD%

:: 2. VERIFICAR / INICIAR MYSQL
tasklist /fi "imagename eq mysqld.exe" 2>nul | find /i "mysqld.exe" >nul
if %errorlevel% equ 0 (
    echo [2/4] Modulo MySQL: En ejecucion (OK)
) else (
    echo [2/4] MySQL no esta corriendo. Intentando iniciar servicio de XAMPP...
    if exist "C:\xampp\mysql_start.bat" (
        start /b "" "C:\xampp\mysql_start.bat" >nul 2>&1
        timeout /t 3 /nobreak >nul
    ) else (
        echo [AVISO] Por favor abre XAMPP Control Panel e inicia el modulo MySQL.
    )
)

:: 3. VERIFICAR Y PREPARAR LA BASE DE DATOS
echo [3/4] Comprobando conexion y tablas de la base de datos...
"%PHP_CMD%" "php\inicializar_bd.php"
if %errorlevel% neq 0 (
    echo.
    echo [AVISO] Hubo un problema al verificar la base de datos.
    echo Asegurate de que MySQL este iniciado en XAMPP y presiona una tecla para continuar.
    pause
)

:: 4. INICIAR EL SERVIDOR WEB LOCAL
set "PUERTO=8000"
echo [4/4] Iniciando servidor web en http://localhost:%PUERTO%
echo.
echo =====================================================================
echo   EL PROYECTO ESTA CORRIENDO EXITOSAMENTE:
echo   - URL Principal: http://localhost:%PUERTO%/index.php
echo   - Catalogo:      http://localhost:%PUERTO%/pages/productos.php
echo   - Panel Admin:   http://localhost:%PUERTO%/login.php
echo.
echo   (Para detener el servidor en cualquier momento presiona Ctrl + C)
echo =====================================================================
echo.

:: Abrir navegador automaticamente
start http://localhost:%PUERTO%/index.php

:: Arrancar servidor integrado de PHP
"%PHP_CMD%" -S localhost:%PUERTO%
