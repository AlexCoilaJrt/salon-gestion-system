@echo off
title Actualizando Sistema de Gestion de Salon
color 0B

echo ==============================================================
echo      ACTUALIZANDO SISTEMA DE GESTION DE SALON (DOCKER HUB)
echo ==============================================================
echo.

:: 1. Verificar si Docker Desktop esta corriendo
docker info > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no esta ejecutandose.
    echo Por favor, abre el programa Docker Desktop y espera a que este listo.
    echo Luego vuelve a ejecutar este archivo.
    echo.
    pause
    exit /b 1
)

:: 2. Auto-configurar archivo .env si no existiera
if not exist ".env" (
    if exist ".env.example" (
        copy .env.example .env > nul
    )
)

:: 3. Descargar las ultimas versiones desde Docker Hub
echo [1/3] Descargando las ultimas actualizaciones desde Docker Hub...
echo       (Repositorios: alexcoila/salon-frontend y alexcoila/salon-backend)...
echo.
docker compose pull

if errorlevel 1 (
    echo.
    echo [ERROR] No se pudieron descargar las actualizaciones de Docker Hub.
    echo Revisa tu conexion a internet.
    echo.
    pause
    exit /b 1
)

:: 4. Reiniciar contenedores aplicando la nueva version
echo.
echo [2/3] Aplicando actualizaciones y reiniciando contenedores...
docker compose up -d

:: 5. Limpieza de versiones antiguas en desuso
echo.
echo [3/3] Limpiando versiones anteriores para liberar espacio en disco...
docker image prune -f > nul 2>&1

echo.
echo ==============================================================
echo       EL SISTEMA HA SIDO ACTUALIZADO CON EXITO!
echo ==============================================================
echo  Tus datos y registros de la base de datos se mantuvieron intactos.
echo.
echo  Puedes ingresar al sistema en: http://localhost
echo ==============================================================
echo.
pause
