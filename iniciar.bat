@echo off
title Iniciando Sistema de Gestion de Salon
color 0A

echo ==============================================================
echo        INICIANDO SISTEMA DE GESTION DE SALON (DOCKER)
echo ==============================================================
echo.

:: 1. Verificar si Docker Desktop esta corriendo
docker info > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no esta ejecutandose.
    echo Por favor, abre el programa Docker Desktop y espera a que este listo.
    echo Luego vuelve a hacer doble clic en este archivo.
    echo.
    pause
    exit /b 1
)

:: 2. Levantar contenedores
echo [1/4] Levantando contenedores (Base de Datos, Backend y Frontend)...
docker compose up -d

if errorlevel 1 (
    echo.
    echo [ERROR] Ocurrio un problema al levantar los contenedores.
    echo Revisa que los puertos 80, 4200, 8080 y 5432 no esten ocupados.
    echo.
    pause
    exit /b 1
)

:: 3. Esperar que la base de datos este lista
echo.
echo [2/4] Verificando estado de la Base de Datos...
timeout /t 3 /nobreak > nul

:: 4. Generar respaldo automatico de la Base de Datos
echo [3/4] Generando copia de seguridad automatica de la base de datos...
if not exist "backups" mkdir backups

for /f "tokens=*" %%a in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HH-mm-ss"') do set FECHA_AUTO=%%a
set ARCHIVO_AUTO=backups\respaldo_auto_%FECHA_AUTO%.sql

docker exec -t salon_postgres pg_dump -U postgres salon_gestion_db > "%ARCHIVO_AUTO%" 2> nul

if exist "%ARCHIVO_AUTO%" (
    echo       [OK] Respaldo automatico guardado: %ARCHIVO_AUTO%
) else (
    echo       [AVISO] No se pudo crear el respaldo en este encendido, continuando...
)

:: 5. Abrir el navegador
echo.
echo [4/4] Abriendo el sistema en tu navegador...
timeout /t 2 /nobreak > nul
start http://localhost

echo.
echo ==============================================================
echo          EL SISTEMA ESTA FUNCIONANDO CON EXITO!
echo ==============================================================
echo  - Frontend: http://localhost  (o http://localhost:4200)
echo  - Backend API: http://localhost:8080/swagger-ui.html
echo  - Respaldo BD: Guardado automaticamente en carpeta 'backups'
echo.
echo  Por favor inicia sesion con tu cuenta de usuario asignada.
echo  (Por seguridad, nunca compartas tus contraseñas).
echo.
echo  Puedes cerrar esta ventana, el sistema seguira funcionando
echo  en segundo plano.
echo ==============================================================
echo.
pause
