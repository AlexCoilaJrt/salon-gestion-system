# 🐳 GUÍA COMPLETA DE COMANDOS DOCKER & DOCKER COMPOSE
### Sistema de Gestión de Salón de Belleza y Spa

Esta guía está diseñada para que comprendas de forma clara, práctica y didáctica el funcionamiento de **Docker** y **Docker Compose**, tanto para este proyecto como para cualquier aplicación moderna.

---

## 📌 1. Conceptos Fundamentales (En Términos Sencillos)

| Concepto | ¿Qué es en la vida real? | Definición Técnica |
| :--- | :--- | :--- |
| **Imagen (Image)** | La *receta* o *plano* de una casa. | Plantilla de solo lectura que contiene el código, runtime, librerías y configuración necesaria para ejecutar la app. |
| **Contenedor (Container)** | La *casa construida y habitada*. | Instancia viva y aislada en ejecución generada a partir de una Imagen. |
| **Volumen (Volume)** | La *caja fuerte* de la casa. | Espacio de disco persistente fuera del contenedor. Si borras el contenedor, tus datos (PostgreSQL) siguen intactos. |
| **Red (Network)** | El *cableado* y *portero eléctrico*. | Red virtual privada donde `salon_frontend`, `salon_backend` y `salon_postgres` se comunican por sus nombres. |
| **Docker Compose** | El *director de orquesta*. | Herramienta que lee `docker-compose.yml` para levantar y coordinar múltiples contenedores a la vez. |

---

## ⚡ 2. Atajos Rápidos del Proyecto (Archivos .bat)

En la raíz de este proyecto tienes scripts listos para usar sin tocar la consola:

| Archivo | ¿Qué comando ejecuta por dentro? | ¿Para qué sirve? |
| :--- | :--- | :--- |
| `iniciar.bat` | `docker compose up -d` + `pg_dump` | Levanta los contenedores en 2 segundos, genera una copia de seguridad automática en `backups/` y abre el navegador. |
| `detener.bat` | `docker compose down` | Apaga y remueve los contenedores de forma segura (sin borrar datos). |
| `ver_logs.bat` | `docker compose logs -f` | Muestra en tiempo real lo que está imprimiendo la consola del sistema. |
| `respaldar_bd.bat` | `docker exec ... pg_dump` | Genera una copia de seguridad manual `.sql` con fecha y hora en `backups/`. |
| `publicar_docker_hub.bat` | `docker compose build` + `docker push` | Compila las últimas versiones y las sube a tu cuenta de Docker Hub. |

---

## 🚀 3. Comandos de Docker Compose (Los más Importantes)

Docker Compose se ejecuta siempre desde la carpeta donde se encuentra tu archivo `docker-compose.yml`.

### Iniciar y Detener Servicios

```bash
# Levantar todos los contenedores en segundo plano (detached mode)
docker compose up -d

# Levantar forzando la reconstrucción de las imágenes (detecta cambios en código)
docker compose up -d --build

# Detener y apagar todos los contenedores del proyecto
docker compose down

# Detener los contenedores borrando también los volúmenes (¡CUIDADO! Borra la BD)
docker compose down -v

# Reiniciar todos los servicios
docker compose restart

# Reiniciar un solo servicio en específico (ej. solo el frontend o solo el backend)
docker compose restart frontend
docker compose restart backend
```

### Monitoreo y Diagnóstico

```bash
# Ver el estado actual de los contenedores del proyecto (Running, Exited, etc.)
docker compose ps

# Ver el uso de memoria RAM y CPU de los contenedores en tiempo real
docker compose stats

# Ver los logs combinados de todos los contenedores en vivo
docker compose logs -f

# Ver los logs únicamente del backend
docker compose logs -f backend

# Ver los logs únicamente del frontend
docker compose logs -f frontend

# Ver los últimos 100 logs de la base de datos
docker compose logs --tail=100 postgres
```

---

## 📦 4. Publicación y Descarga desde Docker Hub

Tu cuenta configurada en `docker-compose.yml` es **`alexcoila`**:
- `alexcoila/salon-frontend:latest`
- `alexcoila/salon-backend:latest`

### Flujo Completo de Publicación:

```bash
# 1. Iniciar sesión en Docker Hub desde la terminal (solo la primera vez)
docker login

# 2. Compilar las imágenes locales con tu etiqueta de Docker Hub
docker compose build

# 3. Subir las imágenes a la nube de Docker Hub
docker push alexcoila/salon-frontend:latest
docker push alexcoila/salon-backend:latest

# 4. Cerrar sesión (opcional)
docker logout
```

### ¿Cómo instalar o actualizar en otra computadora?
En una nueva computadora (de un cliente o local de belleza), no necesitas tener el código fuente:
```bash
# 1. Descargar la última versión desde Docker Hub
docker compose pull

# 2. Levantar los servicios
docker compose up -d
```

---

## 🛠️ 5. Comandos de Contenedores Individuales (`docker`)

Cuando necesites interactuar con un contenedor específico:

```bash
# Listar contenedores activos
docker ps

# Listar TODOS los contenedores (incluso los apagados o con error)
docker ps -a

# Entrar a la terminal interactiva dentro de un contenedor en ejecución
docker exec -it salon_backend sh
docker exec -it salon_frontend sh

# Ejecutar un comando puntual sin entrar a la terminal interactiva
docker exec salon_frontend nginx -s reload

# Copiar un archivo desde tu PC hacia adentro del contenedor
docker cp ./mi-archivo.txt salon_frontend:/usr/share/nginx/html/

# Copiar un archivo desde el contenedor hacia tu PC
docker cp salon_postgres:/var/log/postgresql.log ./log-bd.txt
```

---

## 💾 6. Gestión de Volúmenes y Datos (Base de Datos)

Tus clientes, ventas y configuraciones se guardan en el volumen `salon_postgres_data`.

```bash
# Listar todos los volúmenes existentes en la PC
docker volume ls

# Ver detalles del volumen de la base de datos (dónde se guarda en disco)
docker volume inspect salon_postgres_data

# Realizar un backup manual de la base de datos mediante pg_dump
docker exec -t salon_postgres pg_dump -U postgres salon_gestion_db > backup_manual.sql

# Restaurar una copia de seguridad en caso de emergencia
cat backup_manual.sql | docker exec -i salon_postgres psql -U postgres -d salon_gestion_db
```

---

## 🧹 7. Mantenimiento y Limpieza de Disco

Con el tiempo, Docker puede acumular imágenes viejas y capas intermedias que ocupan espacio en disco:

```bash
# Ver cuánto espacio en disco está usando Docker
docker system df

# Eliminar contenedores detenidos, redes sin uso e imágenes huérfanas
docker system prune

# Limpieza profunda: borra también todas las imágenes no utilizadas actualmente
docker system prune -a

# Borrar una imagen específica de tu disco
docker rmi alexcoila/salon-frontend:latest
```

---

## 🚨 8. Solución de Problemas Frecuentes

### 1. "Ports are not available: listen tcp 0.0.0.0:80: bind: address already in use"
> **Causa:** Tienes otra aplicación (como IIS de Windows, Skype o XAMPP/Apache) usando el puerto 80.  
> **Solución:** Cierra Skype o XAMPP, o detén el servicio de publicación World Wide Web de Windows (`net stop w3svc`).

### 2. "Docker is not running" o "error during connect"
> **Causa:** El programa Docker Desktop está cerrado o aún no ha terminado de encender su motor.  
> **Solución:** Abre Docker Desktop desde el menú Inicio de Windows y espera a que el ícono de la ballena en la barra de tareas se ponga en verde.

### 3. Hice cambios en el código pero en el navegador no se ven
> **Causa:** El navegador tiene en caché los archivos viejos o el contenedor no se reconstruyó.  
> **Solución:**
> 1. En el navegador presiona `Ctrl + Shift + R` (o `Ctrl + F5`) para recargar sin caché.
> 2. Reconstruye el contenedor ejecutando:
>    ```bash
>    docker compose up -d --build
>    ```

---

*Documentación generada para el Sistema de Gestión de Salón de Belleza & Spa Diana.*
