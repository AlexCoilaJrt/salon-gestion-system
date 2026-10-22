@echo off
cd /d "%~dp0"
title Restaurar Base de Datos - Sistema Salon
color 0E

echo ==============================================================
echo        RESTAURAR COPIA DE SEGURIDAD DE LA BASE DE DATOS
echo ==============================================================
echo.

:: 1. Verificar si Docker Desktop esta corriendo
docker info > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no esta ejecutandose.
    echo Inicia Docker Desktop y vuelve a ejecutar este archivo.
    echo.
    pause
    exit /b 1
)

:: 2. Verificar carpeta de backups
if not exist "backups" (
    echo [ERROR] No existe la carpeta 'backups'.
    echo.
    pause
    exit /b 1
)

:: 3. Obtener el archivo .sql mas reciente
set "ULTIMO_RESPALDO="
for /f "delims=" %%a in ('dir /b /o-d "backups\*.sql" 2^>nul') do (
    set "ULTIMO_RESPALDO=backups\%%a"
    goto :encontrado
)

echo [ERROR] No se encontraron archivos .sql en la carpeta 'backups'.
echo.
pause
exit /b 1

:encontrado
echo Se encontro el respaldo mas reciente:
echo   --^> %ULTIMO_RESPALDO%
echo.
set /p CONFIRMAR="Deseas restaurar este archivo en la base de datos? (S/N): "
if /i not "%CONFIRMAR%"=="S" (
    echo.
    echo [INFO] Restauracion cancelada por el usuario.
    echo.
    pause
    exit /b 0
)

echo.
echo Restaurando base de datos desde %ULTIMO_RESPALDO%...
docker exec -i salon_postgres psql -U postgres -d salon_gestion_db < "%ULTIMO_RESPALDO%"

if errorlevel 1 (
    echo.
    echo [ERROR] Hubo un problema al restaurar el respaldo.
    echo Asegurate de que el contenedor 'salon_postgres' este encendido.
    echo.
) else (
    echo.
    echo ==============================================================
    echo [OK] Base de datos restaurada con exito!
    echo ==============================================================
    echo Todos los datos del respaldo han sido aplicados correctamente.
)

echo.
pause
