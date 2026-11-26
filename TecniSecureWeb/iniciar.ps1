# ---------------------------------------------------------------------
# TECNISECURE · Iniciar la API REST (Spring Boot)
# Uso:  .\iniciar.ps1
# ---------------------------------------------------------------------
$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot 'backend')

Write-Host '=====================================================================' -ForegroundColor Cyan
Write-Host '                TECNISECURE - API REST (Spring Boot)' -ForegroundColor Cyan
Write-Host '=====================================================================' -ForegroundColor Cyan

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    Write-Host '[ERROR] No se encontró Java. Instala JDK 21: https://adoptium.net' -ForegroundColor Red
    exit 1
}

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host '[ERROR] No se encontró Maven: https://maven.apache.org/download.cgi' -ForegroundColor Red
    exit 1
}

Write-Host ''
Write-Host ' Sitio web:  http://localhost:8080' -ForegroundColor Green
Write-Host ' Swagger UI: http://localhost:8080/swagger-ui.html' -ForegroundColor Green
Write-Host ' Consola H2: http://localhost:8080/h2-console' -ForegroundColor Green
Write-Host ' (Ctrl + C para detener)' -ForegroundColor DarkGray
Write-Host ''

Start-Process 'http://localhost:8080'
mvn -B spring-boot:run
