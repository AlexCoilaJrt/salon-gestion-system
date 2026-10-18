@echo off
cd /d "%~dp0"
title Respaldo Manual de Base de Datos - Sistema Salon
color 0B

echo ==============================================================
echo        GENERANDO COPIA DE SEGURIDAD DE LA BASE DE DATOS
echo ==============================================================
echo.

if not exist "backups" mkdir backups

for /f "tokens=*" %%a in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HH-mm-ss"') do set FECHA_MANUAL=%%a
set ARCHIVO=backups\respaldo_manual_%FECHA_MANUAL%.sql

echo Generando volcado de PostgreSQL a %ARCHIVO%...
docker exec -t salon_postgres pg_dump -U postgres salon_gestion_db > "%ARCHIVO%"

if errorlevel 1 (
    echo.
    echo [ERROR] No se pudo realizar la copia de seguridad.
    echo Asegurate de que el contenedor 'salon_postgres' este encendido.
) else (
    echo.
    echo ==============================================================
    echo [OK] Copia de seguridad guardada con exito en:
    echo      %ARCHIVO%
    echo ==============================================================
)

echo.
pause
