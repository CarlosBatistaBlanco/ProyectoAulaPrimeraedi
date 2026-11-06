# TECNISECURE - Inicializador del Proyecto (PowerShell)
$Host.UI.RawUI.WindowTitle = "TECNISECURE - Inicializador del Proyecto"
Set-Location -LiteralPath $PSScriptRoot

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "               TECNISECURE - SISTEMA DE VIGILANCIA                   " -ForegroundColor White
Write-Host "            Inicializador automático de entorno y servidor           " -ForegroundColor Gray
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Detectar PHP
$phpCmd = $null
if (Get-Command php -ErrorAction SilentlyContinue) {
    $phpCmd = "php"
} elseif (Test-Path "C:\xampp\php\php.exe") {
    $phpCmd = "C:\xampp\php\php.exe"
} elseif (Test-Path "D:\xampp\php\php.exe") {
    $phpCmd = "D:\xampp\php\php.exe"
}

if (-not $phpCmd) {
    Write-Host "[ERROR] No se encontró PHP instalado en el sistema." -ForegroundColor Red
    Write-Host "Por favor instala XAMPP o añade PHP al PATH." -ForegroundColor Yellow
    Pause
    exit 1
}

Write-Host "[1/4] PHP detectado: $phpCmd" -ForegroundColor Green

# 2. Verificar MySQL
$mysqlRunning = Get-Process mysqld -ErrorAction SilentlyContinue
if ($mysqlRunning) {
    Write-Host "[2/4] Módulo MySQL: En ejecución (OK)" -ForegroundColor Green
} else {
    Write-Host "[2/4] MySQL no está corriendo. Intentando iniciar..." -ForegroundColor Yellow
    if (Test-Path "C:\xampp\mysql_start.bat") {
        Start-Process "C:\xampp\mysql_start.bat" -WindowStyle Hidden
        Start-Sleep -Seconds 3
    } else {
        Write-Host "[AVISO] Por favor inicia el módulo MySQL desde el panel de XAMPP." -ForegroundColor Yellow
    }
}

# 3. Base de datos
Write-Host "[3/4] Comprobando conexión y tablas de la base de datos..." -ForegroundColor Cyan
& $phpCmd "php\inicializar_bd.php"

# 4. Servidor web local
$puerto = 8000
Write-Host "[4/4] Iniciando servidor web en http://localhost:$puerto" -ForegroundColor Green
Write-Host ""
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "  EL PROYECTO ESTÁ CORRIENDO EXITOSAMENTE:" -ForegroundColor Green
Write-Host "  - URL Principal: http://localhost:$puerto/index.php" -ForegroundColor White
Write-Host "  - Catálogo:      http://localhost:$puerto/pages/productos.php" -ForegroundColor White
Write-Host "  - Panel Admin:   http://localhost:$puerto/login.php" -ForegroundColor White
Write-Host ""
Write-Host "  (Para detener el servidor presiona Ctrl + C)" -ForegroundColor Gray
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host ""

# Abrir navegador
Start-Process "http://localhost:$puerto/index.php"

# Iniciar servidor PHP
& $phpCmd -S "localhost:$puerto"
