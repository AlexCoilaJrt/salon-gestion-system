@echo off
cd /d "%~dp0"
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

:: 2. Auto-configurar archivo de entorno .env si no existe
if not exist ".env" (
    if exist ".env.example" (
        echo [INFO] Configurando entorno inicial por primera vez...
        copy .env.example .env > nul
        echo       [OK] Archivo .env generado automaticamente.
        echo.
    )
)

:: 3. Si es la primera vez, descargar imagenes listas desde Docker Hub (Ultra Rapido)
docker image inspect alexcoila/salon-backend:latest > nul 2>&1
if errorlevel 1 (
    echo [1/5] Primera instalacion detectada!
    echo       Descargando las imagenes oficiales desde Docker Hub...
    echo       Descarga inicial requerida solo la primera vez.
    echo.
    docker compose pull
    echo.
)

:: 4. Levantar contenedores
echo [2/5] Levantando contenedores (Base de Datos, Backend y Frontend)...
docker compose up -d

if errorlevel 1 (
    echo.
    echo [ERROR] Ocurrio un problema al levantar los contenedores.
    echo Revisa que los puertos 80, 4200, 8080 y 5432 no esten ocupados.
    echo.
    pause
    exit /b 1
)

:: 5. Esperar inicializacion de servicios (PostgreSQL + Spring Boot)
echo.
echo [3/5] Inicializando servicios y Base de Datos...
echo       Esperando unos segundos a que el Backend este listo...
powershell -NoProfile -Command "Start-Sleep -Seconds 10"

:: 6. Generar respaldo automatico de la Base de Datos
echo [4/5] Generando copia de seguridad automatica de la base de datos...
if not exist "backups" mkdir backups

for /f "tokens=*" %%a in ('powershell -NoProfile -Command "Get-Date -Format yyyy-MM-dd_HH-mm-ss"') do set FECHA_AUTO=%%a
set ARCHIVO_AUTO=backups\respaldo_auto_%FECHA_AUTO%.sql

docker exec -t salon_postgres pg_dump -U postgres salon_gestion_db > "%ARCHIVO_AUTO%" 2> nul

if exist "%ARCHIVO_AUTO%" (
    for %%F in ("%ARCHIVO_AUTO%") do (
        if %%~zF gtr 0 (
            echo       [OK] Respaldo automatico guardado: %ARCHIVO_AUTO%
        ) else (
            del "%ARCHIVO_AUTO%" > nul 2>&1
            echo       [INFO] Base de datos en arranque inicial, respaldo se creara en el proximo inicio.
        )
    )
) else (
    echo       [AVISO] No se pudo crear el respaldo en este encendido, continuando...
)

:: 7. Abrir el navegador
echo.
echo [5/5] Abriendo el sistema en tu navegador...
powershell -NoProfile -Command "Start-Sleep -Seconds 2"
start http://localhost

echo.
echo ==============================================================
echo          EL SISTEMA ESTA FUNCIONANDO CON EXITO!
echo ==============================================================
echo  - Frontend: http://localhost  (o http://localhost:4200)
echo  - Backend API: http://localhost:8080/swagger-ui.html
echo.
echo  Credenciales por defecto:
echo    * Administrador: admin@salon.com / admin123
echo    * Cajero:        caja@salon.com  / caja123
echo    * Barbero/Esp:   juan@salon.com  / juan123
echo.
echo  NOTA: Para actualizar a la ultima version cuando subas cambios,
echo        simplemente ejecuta 'actualizar.bat'.
echo.
echo  Puedes cerrar esta ventana, el sistema seguira funcionando
echo  en segundo plano.
echo ==============================================================
echo.
pause
