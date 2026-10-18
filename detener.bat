@echo off
title Deteniendo Sistema de Gestion de Salon

echo ==============================================================
echo        DETENIENDO SISTEMA DE GESTION DE SALON
echo ==============================================================
echo.

docker compose down

echo.
echo Los contenedores han sido detenidos correctamente.
echo Los datos de la base de datos estan seguros en el volumen persistente.
echo.
pause
