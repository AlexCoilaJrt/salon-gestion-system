@echo off
title Publicando Imagenes a Docker Hub
color 0B

echo ==============================================================
echo       PUBLICAR SISTEMA DE GESTION DE SALON A DOCKER HUB
echo ==============================================================
echo  Usuario: alexcoila
echo  Repositorios:
echo    - alexcoila/salon-frontend:latest
echo    - alexcoila/salon-backend:latest
echo ==============================================================
echo.

:: 1. Verificar si Docker Desktop esta corriendo
docker info > nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no esta en ejecucion.
    echo Inicia Docker Desktop y vuelve a ejecutar este archivo.
    echo.
    pause
    exit /b 1
)

:: 2. Construir imagenes con los ultimos cambios de codigo
echo [1/3] Compilando y construyendo imagenes Docker con las ultimas actualizaciones...
echo       (Esto puede tardar un par de minutos)...
echo.
docker compose build

if errorlevel 1 (
    echo.
    echo [ERROR] Hubo un error durante la construccion de las imagenes.
    echo Revisa los mensajes de error arriba.
    echo.
    pause
    exit /b 1
)

:: 3. Subir Frontend
echo.
echo [2/3] Subiendo alexcoila/salon-frontend:latest a Docker Hub...
docker push alexcoila/salon-frontend:latest
if errorlevel 1 (
    echo.
    echo [ADVERTENCIA] Fallo al subir el Frontend.
    echo Si no has iniciado sesion, escribe 'docker login' en una terminal.
    echo.
    pause
    exit /b 1
)

:: 4. Subir Backend
echo.
echo [3/3] Subiendo alexcoila/salon-backend:latest a Docker Hub...
docker push alexcoila/salon-backend:latest
if errorlevel 1 (
    echo.
    echo [ADVERTENCIA] Fallo al subir el Backend.
    echo Si no has iniciado sesion, escribe 'docker login' en una terminal.
    echo.
    pause
    exit /b 1
)

echo.
echo ==============================================================
echo    FELICITACIONES! IMAGENES PUBLICADAS EXITOSAMENTE EN DOCKER HUB
echo ==============================================================
echo  - Frontend: https://hub.docker.com/r/alexcoila/salon-frontend
echo  - Backend:  https://hub.docker.com/r/alexcoila/salon-backend
echo.
echo  Cualquier persona o servidor ahora podra descargar la ultima
echo  version simplemente ejecutando:
echo    docker compose pull
echo ==============================================================
echo.
pause
