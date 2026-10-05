# SGAT-OIT | Sistema de Gestión de Activos Tecnológicos

Plataforma web desarrollada para la Oficina de Infraestructura Tecnológica (OIT) del Ministerio de Cultura, orientada al control de inventario, solicitudes, préstamos, mantenimientos y trazabilidad de activos.

## 1. Stack Tecnológico

* **Backend:** Java 17 + Spring Boot 3 (Spring Web, Spring Data JPA, Spring Security)
* **Base de Datos:** PostgreSQL 16
* **Frontend:** HTML5, CSS3, JavaScript
* **Despliegue:** Docker + Oracle Cloud Infrastructure (OCI)

## 2. Estructura de Paquetes del Backend

* `config/`: Configuración de seguridad (Spring Security, CORS y cifrado BCrypt)
* `controller/`: Exposición de endpoints REST por módulo
* `service/`: Lógica de negocio, validaciones y registro de trazabilidad
* `repository/`: Interfaces `JpaRepository` conectadas a PostgreSQL
* `model/entity/`: Clases de entidad mapeadas a las 14 tablas del esquema

## 3. Configuración y Despliegue con Docker

Variables de entorno requeridas en `.env`:

```env
DB_URL=jdbc:postgresql://db:5432/sgat_oit
DB_USER=postgres
DB_PASSWORD=sgat_secret
SERVER_PORT=8080
```

Comando de ejecución:

```bash
docker compose up -d --build
```

## 4. Endpoints Principales (Versión 1)

| Método | Endpoint | Rol | Función |
| :--- | :--- | :--- | :--- |
| POST | `/api/auth/login` | General | Inicio de sesión seguro |
| GET | `/api/activos` | Colaborador / Admin | Consulta del catálogo de activos |
| POST | `/api/activos` | Administrador | Registro de nuevo activo tecnológico |
| POST | `/api/solicitudes` | Colaborador | Registro de solicitud de equipo |
| PUT | `/api/solicitudes/{id}` | Administrador | Aprobación o rechazo de solicitud |
| POST | `/api/asignaciones` | Administrador | Registro de préstamo y devolución |
| POST | `/api/mantenimientos` | Técnico | Registro de mantenimiento y estado |