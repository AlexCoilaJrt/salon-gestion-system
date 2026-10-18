@echo off
title Logs del Sistema de Gestion de Salon

echo ==============================================================
echo        MOSTRANDO LOGS EN TIEMPO REAL (Ctrl + C para salir)
echo ==============================================================
echo.

docker compose logs -f
