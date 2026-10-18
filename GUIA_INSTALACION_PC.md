# 📋 Guía Rápida de Instalación en Otra PC (con Docker)

Esta guía explica paso a paso cómo instalar y ejecutar el **Sistema de Gestión de Salón** en cualquier otra computadora con Windows sin necesidad de instalar Java, Node.js ni PostgreSQL por separado.

---

## 💰 ¿Nos va a costar algo de dinero?

**NO, el costo es $0 (TOTALMENTE GRATIS).**

* **Docker Desktop:** Es gratuito para uso personal, educación y pequeñas empresas (menos de 250 empleados y menos de $10 millones de dólares en ingresos anuales). Para un salón de belleza aplica 100% la licencia gratuita.
* **PostgreSQL:** Base de datos open-source y gratuita.
* **Java (Eclipse Temurin) & Spring Boot:** Gratuitos y de código abierto.
* **Angular & Nginx:** Gratuitos y de código abierto.

*¿Cuándo costaría dinero?* Únicamente si deciden no usar una PC física en el local y prefieren pagar un servidor en la nube (VPS en internet) para que esté online las 24 horas (eso costaría unos $4 a $6 USD al mes). Pero si se usa en la PC del salón, **no gastan ni un solo centavo**.

---

## 🖥️ Requisitos de la PC destino

* **Sistema Operativo:** Windows 10 o Windows 11 (64 bits).
* **Memoria RAM:** Mínimo 8 GB recomendados.
* **Espacio en Disco:** Mínimo 10 GB libres.
* **Virtualización activada:** Debe estar habilitada en la BIOS (la gran mayoría de PCs ya la traen activada de fábrica).

---

## 🚀 Pasos para Instalar en la Nueva PC

### Paso 1: Instalar Docker Desktop (Solo se hace una vez)
1. Descarga el instalador oficial de Docker para Windows:
   👉 **[https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)**
2. Ejecuta el instalador descargado.
3. Asegúrate de dejar marcada la opción **"Use WSL 2 instead of Hyper-V (recommended)"**.
4. Haz clic en **OK** y espera que termine la instalación.
5. Reinicia la computadora si el instalador te lo solicita.
6. Abre **Docker Desktop** desde el menú de inicio y acepta los términos de servicio (selecciona uso personal / small business). Espera a que el icono de Docker en la esquina inferior izquierda se ponga en color verde (*Engine running*).

---

### Paso 2: Copiar el Proyecto a la PC
1. Copia toda esta carpeta `salon-gestion-system` a la nueva PC (puedes pasarla por memoria USB, comprimida en ZIP o clonándola con Git).
2. Guárdala en una ubicación cómoda (por ejemplo, en `C:\salon-gestion-system` o en el Escritorio).

---

### Paso 3: Iniciar el Sistema (1 solo clic)
1. Entra a la carpeta del proyecto.
2. Haz doble clic en el archivo:
   👉 **`iniciar.bat`**
3. La primera vez tardará unos minutos mientras descarga las imágenes y compila el backend y frontend.
4. Al finalizar, **se abrirá automáticamente tu navegador** en:
   👉 **`http://localhost`** (o `http://localhost:4200`)

---

### Paso 4: Credenciales de Acceso
El sistema ya viene con datos iniciales listos para trabajar:

* **Administrador:**
  * **Usuario:** `admin@salon.com`
  * **Contraseña:** `admin123`
* **Cajero:**
  * **Usuario:** `caja@salon.com`
  * **Contraseña:** `caja123`
* **Especialista / Barbero:**
  * **Usuario:** `juan@salon.com`
  * **Contraseña:** `juan123`

---

## 🛠️ Scripts Útiles Incluidos

En la carpeta principal encontrarás varios accesos directos por comodidad:

| Archivo | ¿Para qué sirve? |
| :--- | :--- |
| `iniciar.bat` | Enciende la base de datos, backend y frontend, y abre el sistema en el navegador. |
| `detener.bat` | Apaga los contenedores de forma segura (los datos de las citas y ventas **no se borran**). |
| `ver_logs.bat` | Abre una consola para ver qué está pasando dentro del sistema en tiempo real. |
| `respaldar_bd.bat` | Genera una copia de seguridad (`.sql`) de toda la base de datos dentro de la carpeta `backups/`. |

---

## 📱 ¿Cómo usar el sistema desde una Tablet o Celular en el mismo salón?

Si la PC donde corre Docker y los demás dispositivos están conectados a la misma red Wi-Fi del salón:
1. En la PC abre la consola (`cmd`) y escribe `ipconfig`.
2. Busca la **Dirección IPv4** (por ejemplo: `192.168.1.50`).
3. En la tablet o celular abre el navegador y entra a:
   👉 `http://192.168.1.50`
4. Podrán agendar citas, cobrar o registrar asistencias desde cualquier dispositivo conectado al Wi-Fi del local.

---

## 🔒 ¿Qué pasa con los datos al apagar la PC?
Los datos de las citas, clientes, ventas y empleados están guardados en un volumen persistente de Docker llamado `salon_postgres_data`. **No se pierden al apagar la computadora ni al ejecutar `detener.bat`**.

Para mayor seguridad de tu negocio, es recomendable hacer doble clic en `respaldar_bd.bat` una vez por semana y guardar ese archivo en una memoria USB o Google Drive.
