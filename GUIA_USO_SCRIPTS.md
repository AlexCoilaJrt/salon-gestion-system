# 🚀 Guía Práctica de Uso: Scripts Rápidos (.bat)
### Sistema de Gestión de Salón de Belleza & Spa

Esta guía te explica de forma clara y ordenada cómo usar los archivos de inicio rápido (`.bat`) para instalar, usar, actualizar y mantener el sistema en cualquier computadora sin necesidad de escribir comandos en la consola.

---

## 🧭 1. Resumen de los Scripts Disponibles

| Archivo | ¿Quién lo usa? | ¿Cuándo se usa? | ¿Qué hace por dentro? |
| :--- | :--- | :--- | :--- |
| **`iniciar.bat`** | Usuario / Cliente | Al encender la PC por las mañanas (o primera vez tras clonar). | Auto-crea `.env`, descarga imágenes de Docker Hub si no existen, enciende los contenedores, respalda la BD y abre el navegador. |
| **`actualizar.bat`** | Usuario / Cliente | Cuando el desarrollador avisa que hay una nueva versión. | Descarga los cambios de Docker Hub y reinicia el sistema en 15 segundos sin tocar los datos de la BD. |
| **`detener.bat`** | Usuario / Cliente | Al finalizar la jornada o antes de apagar la PC. | Apaga los contenedores de forma segura para no corromper la base de datos. |
| **`respaldar_bd.bat`** | Usuario / Admin | Semanalmente o antes de hacer mantenimientos. | Genera un archivo `.sql` con fecha y hora dentro de la carpeta `backups/`. |
| **`ver_logs.bat`** | Administrador / Soporte | Si algo no responde o se quiere ver la consola en vivo. | Muestra la actividad del Backend, Frontend y Base de Datos en tiempo real. |
| **`publicar_docker_hub.bat`** | Solo Tú (Programador) | Cuando terminas de programar una nueva mejora. | Compila el código nuevo y lo sube a la nube de Docker Hub (`alexcoila`). |

---

## 🖥️ 2. Guía Paso a Paso por Situación

---

### 🟢 Situación A: Instalar por Primera Vez en Otra PC (Cliente o Sucursal)

> **Requisito previo:** La PC solo debe tener instalado [Docker Desktop](https://www.docker.com/products/docker-desktop/) y estar en ejecución (ícono de la ballena verde).

1. **Obtener el proyecto en la nueva PC:**
   Clona el repositorio desde GitHub o copia la carpeta del proyecto a la PC:
   ```bash
   git clone https://github.com/AlexCoilaJrt/salon-gestion-system.git
   ```
2. **Ejecutar el inicio rápido:**
   Entra a la carpeta del proyecto y haz doble clic en:
   👉 **`iniciar.bat`**
3. **¿Qué sucederá automáticamente?**
   - Detectará que no existe `.env` y lo creará a partir de `.env.example`.
   - Detectará que es la primera instalación y descargará tus imágenes de **Docker Hub** en ~1 minuto (sin compilar nada localmente).
   - Levantará los contenedores de PostgreSQL, Backend (Spring Boot) y Frontend (Angular).
   - El Backend creará las tablas y datos iniciales en la base de datos automáticamente.
   - En unos segundos **se abrirá el navegador** en `http://localhost`.
4. **Iniciar sesión:**
   - **Admin:** `admin@salon.com` / `admin123`
   - **Cajero:** `caja@salon.com` / `caja123`
   - **Especialista:** `juan@salon.com` / `juan123`

---

### ☕ Situación B: Rutina Diaria de Trabajo en el Salón

#### Por la mañana (Abrir el local):
1. Asegúrate de que **Docker Desktop** esté abierto en la PC principal.
2. Haz doble clic en **`iniciar.bat`**.
3. En pocos segundos el sistema estará listo y se abrirá tu navegador.
4. *¡Listo para agendar citas y cobrar!*

#### Durante el día (Uso en Tablets o Celulares):
- Desde cualquier teléfono o tablet conectado al mismo Wi-Fi del salón, ingresa a la IP de la computadora (ejemplo: `http://192.168.1.50`).

#### Por la noche (Cerrar el local):
1. Antes de apagar la computadora, haz doble clic en:
   👉 **`detener.bat`**
2. Esto apaga los contenedores con seguridad asegurando que no queden transacciones a medias en PostgreSQL.

---

### 🔄 Situación C: Cómo Publicar y Recibir Actualizaciones (Flujo Completo)

Cuando tú como programador agregas una nueva función, corriges un bug o mejoras el diseño:

#### Paso 1: En tu PC de Desarrollo (Tú)
1. Realizas los cambios en el código (Java o Angular).
2. Haz doble clic en **`publicar_docker_hub.bat`**.
   *(Este script compilará tus cambios y subirá las imágenes actualizadas a tu cuenta `alexcoila` en Docker Hub)*.
3. Subes tu código fuente a GitHub:
   ```bash
   git add .
   git commit -m "Mejora: descripcion del cambio"
   git push
   ```

#### Paso 2: En la PC del Salón / Cliente (La otra PC)
1. Le dices al usuario o tú mismo en su máquina haces doble clic en:
   👉 **`actualizar.bat`**
2. **¿Qué hace `actualizar.bat`?**
   - Descarga la nueva versión desde Docker Hub (~15 segundos).
   - Reinicia los contenedores reemplazando el Backend y Frontend por los nuevos.
   - **IMPORTANTE:** Toda la base de datos (citas, ventas, clientes registrados) **se mantiene 100% intacta** porque está guardada en el volumen persistente `salon_postgres_data`.
3. Al terminar, el sistema ya tiene las nuevas funciones listas para usar.

---

### 💾 Situación D: Respaldos de Seguridad de la Base de Datos

Aunque los datos no se borran al reiniciar o apagar la PC, es buena práctica tener copias de seguridad externas:

1. Haz doble clic en **`respaldar_bd.bat`**.
2. Se generará un archivo dentro de la carpeta `backups/` con el nombre:
   `respaldo_manual_YYYY-MM-DD_HH-mm-ss.sql`
3. Recomendación: Copia ese archivo `.sql` a una memoria USB o súbelo a Google Drive una vez por semana.

---

### 🔍 Situación E: ¿Qué hacer si algo falla? (`ver_logs.bat`)

Si por alguna razón la página no abre o muestra error de conexión:
1. Haz doble clic en **`ver_logs.bat`**.
2. Se abrirá una ventana mostrando en tiempo real los mensajes del sistema.
3. Si el Backend está terminando de iniciar, verás mensajes de Spring Boot (`Started SalonGestionApiApplication`).
4. Si ves algún error en rojo (por ejemplo, puerto ocupado), sabrás qué programa cerrar.

---

## ❓ Preguntas Frecuentes (FAQ)

### ¿Se necesita internet para usar el sistema todos los días?
**NO.** Internet solo se necesita la primera vez para descargar las imágenes (`iniciar.bat`) y cuando quieras aplicar una actualización (`actualizar.bat`). Durante el día a día, el sistema corre 100% local en la computadora sin consumir internet.

### ¿Se pierden mis clientes o ventas al ejecutar `actualizar.bat` o `detener.bat`?
**NO.** La información no está dentro de los contenedores de código, sino en un disco virtual persistente de Docker llamado `salon_postgres_data`. Solo se actualizarán el código del Backend y Frontend.

### ¿Qué pasa si abro `iniciar.bat` cuando el sistema ya estaba encendido?
No pasa nada malo. Docker detectará que los contenedores ya están corriendo, generará una copia de seguridad rápida y te abrirá la ventana del navegador.
