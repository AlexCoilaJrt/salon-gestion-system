# 💇‍♀️ Sistema de Gestión de Salón (Salon Gestion System)

Un sistema integral diseñado para administrar eficientemente las operaciones de un salón de belleza o spa. Este proyecto está dividido en dos partes principales: un **Backend** desarrollado en Java con Spring Boot y un **Frontend** desarrollado en Angular con PrimeNG.

## 🚀 Arquitectura y Tecnologías

El sistema sigue una arquitectura cliente-servidor con separación clara entre el backend (API REST) y el frontend (Single Page Application).

### 🛠 Backend (`Backend-Salon/salon-gestion-api`)
- **Framework:** Java 21 + Spring Boot 3.x
- **Base de Datos:** PostgreSQL
- **Seguridad:** Spring Security + JWT (JSON Web Tokens basados en RSA Keys)
- **Persistencia:** Spring Data JPA / Hibernate
- **Documentación API:** Springdoc OpenAPI (Swagger)
- **Otras dependencias:** WebSocket, Lombok, Validation

### 🎨 Frontend (`FrontEnd-Salon`)
- **Framework:** Angular 19
- **Estilos y Componentes UI:** PrimeNG 19, TailwindCSS
- **Iconos:** PrimeIcons
- **Gráficos:** Chart.js
- **Alertas:** SweetAlert2

## 📦 Módulos Principales (Features)

El sistema está organizado por dominios de negocio (implementados tanto en el backend como en el frontend):

- 📊 **Analítica & Dashboard:** Reportes visuales, estadísticas del negocio y cuadros de mando.
- 📋 **Catálogo:** Gestión de productos, servicios y categorías.
- 🏢 **Empresa / Sistema:** Configuración general de la sucursal o salón.
- 🎁 **Fidelización:** Gestión de clientes, puntos y recompensas.
- 💰 **Finanzas:** Control de ingresos, egresos, caja y reportes financieros.
- ⚙️ **Operaciones:** Gestión de citas, ventas, caja diaria e inventario.
- 👥 **RRHH (Recursos Humanos):** Gestión de empleados, comisiones, incentivos y control de asistencias.
- 🔒 **Seguridad & Auth:** Autenticación de usuarios, roles, permisos y control de acceso al sistema.

---

## ⚙️ Requisitos Previos

Antes de ejecutar el proyecto, asegúrate de tener instalado en tu entorno de desarrollo:
- **Java Development Kit (JDK) 21**
- **Node.js** (v18 o superior recomendada)
- **Angular CLI** (`npm install -g @angular/cli`)
- **PostgreSQL** (Servidor corriendo localmente o en red)
- **Maven** (Opcional, el backend incluye el wrapper `mvnw`)

---

## 🚀 Instalación y Configuración

### 1. Configuración de la Base de Datos (PostgreSQL)
1. Abre tu gestor de base de datos PostgreSQL (ej. pgAdmin, DBeaver o consola).
2. Crea una nueva base de datos vacía llamada: `salon_gestion_db`
3. Por defecto, el sistema intentará conectarse con las siguientes credenciales (puedes cambiarlas en el archivo `application.properties`):
   - **Usuario:** `postgres`
   - **Contraseña:** `123456`
   - **Puerto:** `5432`

> **Nota:** Hibernate (JPA) está configurado en el backend con `spring.jpa.hibernate.ddl-auto=update`, por lo que las tablas, relaciones y esquemas de la base de datos se generarán y actualizarán automáticamente la primera vez que arranques el backend.

### 2. Ejecutar el Backend (Spring Boot)
1. Abre una terminal y navega a la carpeta principal del backend:
   ```bash
   cd Backend-Salon/salon-gestion-api
   ```
2. *(Seguridad JWT)*: El proyecto utiliza cifrado asimétrico para los tokens JWT, por lo que requiere de un par de llaves RSA (`private_key.pem` y `public_key.pem`) ubicadas en la ruta `src/main/resources/`. *(Si clonas el proyecto, asegúrate de que estas llaves existan o generalas)*.
3. Compila y ejecuta el proyecto utilizando el Maven Wrapper incluido:
   ```bash
   # En Linux / Mac
   ./mvnw spring-boot:run
   
   # En Windows
   mvnw.cmd spring-boot:run
   ```
4. El backend se iniciará exitosamente en el puerto 8080: `http://localhost:8080`

**Documentación de la API (Swagger):** 
Una vez que el backend esté en ejecución, puedes explorar y probar todos los endpoints REST directamente desde la interfaz gráfica de Swagger visitando: 
👉 `http://localhost:8080/swagger-ui.html`

### 3. Ejecutar el Frontend (Angular)
1. Abre una nueva terminal y navega a la carpeta del frontend:
   ```bash
   cd FrontEnd-Salon
   ```
2. Instala todas las dependencias necesarias de Node.js:
   ```bash
   npm install
   ```
3. Inicia el servidor de desarrollo de Angular:
   ```bash
   ng serve
   ```
   *(También puedes usar `npm start`)*
4. Abre tu navegador web favorito y dirígete a: 
👉 `http://localhost:4200`

---

## 👨‍💻 Estructura del Código

- **Frontend (`src/app/`)**: Sigue una estructura basada en "Features" (módulos). Cada módulo de negocio tiene su propio directorio dentro de `src/app/features/` (ej. `/rrhh`, `/finanzas`). Los servicios transversales y de configuración están en `core/` (interceptores, guards) y los componentes visuales reutilizables en `shared/`.
- **Backend (`src/main/java/pe/com/salon/salongestionapi/`)**: Adopta una estructura modular por dominio (ej. `rrhh`, `ventas`, `analitica`). Dentro de cada módulo, se sigue el patrón de diseño por capas de Spring: `Controller`, `Service`, `Repository`, `Entity` y `DTO`.

## 📄 Licencia
Este proyecto es de uso privativo / [Añadir licencia del proyecto aquí, si aplica].
