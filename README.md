# EIA Camel vs. Dwarf Racing League

Sistema de información para administrar una liga de carreras ficticia. Incluye una
API REST en Java/Spring Boot, persistencia PostgreSQL, autenticación JWT, autorización
por roles, reglas de negocio, auditoría, pruebas automatizadas y una interfaz React.

## Integrantes

- Pablo Gil Macia
- Emanuel Quintero Franco

## Funcionalidades

- Registro, login, refresh token y perfil de usuario.
- Roles `ADMIN`, `RACE_ORGANIZER` y `VIEWER`.
- CRUD de competidores y equipos.
- Gestión de miembros de equipos.
- CRUD y transiciones de estado de carreras.
- Inscripciones, aprobación, rechazo y posiciones de salida.
- Registro de resultados y cálculo de puntos.
- Clasificaciones generales, de competidores y de equipos.
- Auditoría de acciones importantes, visible solo para administradores.
- Errores estructurados (`timestamp`, `status`, `error`, `message`, `path`).
- GUI con estados de carga, vacío, error, 403 y 404.

## Arquitectura

```text
frontend/ React/Vite
    │ HTTP + JWT
    ▼
backend/src/main/java/edu/eia/racing/
    controller → service → repository → PostgreSQL
        │          │
        │          ├── reglas de negocio
        │          └── DTOs de respuesta
        ├── security: JWT, filtro y roles
        ├── exception: errores API estructurados
        ├── model: entidades JPA
        └── config: seed y CORS
```

Las entidades JPA nunca se exponen directamente: los controladores retornan DTOs.
La GUI solo consume la API y no accede a PostgreSQL.

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, Validation, Security |
| Seguridad | JWT con access/refresh token y BCrypt |
| Base de datos | PostgreSQL 16 |
| Frontend | React 19, Vite 7, Nginx |
| Testing | JUnit 5, Mockito, Spring MockMvc |
| Contenedores | Docker y Docker Compose |

## Modelo de datos

Las nueve entidades principales son `User`, `Role`, `Competitor`, `Team`, `TeamMember`,
`Race`, `RaceRegistration`, `RaceResult` y `AuditLog`.

Relaciones principales:

- Un `User` tiene un `Role` y puede registrar acciones.
- Un `Team` contiene miembros mediante `TeamMember`.
- Un `Competitor` puede tener membresías históricas y una membresía activa.
- Una `Race` pertenece a un usuario organizador.
- Una `Race` tiene `RaceRegistration` y `RaceResult`.
- Un `RaceResult` referencia una inscripción y registra el usuario que lo creó.
- `AuditLog` referencia opcionalmente al usuario y a la entidad afectada.

El diagrama entidad-relación editable está en `docs/diagrama-er.dbml` y la versión
visual está disponible en `docs/evidence/Diagrama_ERD.png`.

## Roles y permisos

| Acción | ADMIN | RACE_ORGANIZER | VIEWER |
|---|:---:|:---:|:---:|
| Consultar información | Sí | Sí | Sí |
| Administrar competidores | Sí | No | No |
| Administrar equipos | Sí | No | No |
| Crear/editar carreras | Sí | Sí | No |
| Gestionar inscripciones | Sí | Sí | No |
| Registrar resultados | Sí | Sí | No |
| Consultar auditoría | Sí | No | No |

La autorización se valida en backend mediante `@PreAuthorize`; la GUI también oculta
o deshabilita acciones no permitidas.

## Puertos y variables

| Componente | URL/puerto |
|---|---|
| Frontend Docker | `http://localhost:5173` |
| Backend Docker | `http://localhost:8080` |
| PostgreSQL publicado en host | `localhost:6969` |
| PostgreSQL dentro de Compose | `db:5432` |

Crear variables locales sin subir secretos:

```bash
cp .env.example .env
```

Variables principales:

```dotenv
DB_HOST=localhost
DB_PORT=6969
DB_NAME=racing_league
DB_USERNAME=racing_user
DB_PASSWORD=<valor-local>
JWT_SECRET=<mínimo-32-caracteres>
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
VITE_API_URL=http://localhost:8080/api
SERVER_PORT=8080
```

Dentro de Docker, el backend usa `DB_HOST=db` y `DB_PORT=5432`, porque ese es el
puerto interno del servicio PostgreSQL. El puerto publicado para el host es 6969.

## Compartir el proyecto con otro integrante

El archivo `.env` contiene configuración local y secretos; **no debe subirse a
GitHub ni enviarse por chat**. El repositorio solo debe incluir `.env.example`,
que contiene placeholders. Cada integrante debe crear su propio `.env` a partir
de ese archivo:

```bash
cp .env.example .env
```

Luego debe editar como mínimo:

```dotenv
DB_PASSWORD=<contraseña-local-o-del-entorno>
JWT_SECRET=<secreto-aleatorio-de-al-menos-32-caracteres>
```

Para generar una clave JWT segura en Linux/macOS:

```bash
openssl rand -hex 32
```

Los valores deben compartirse mediante un gestor de contraseñas o canal seguro,
nunca mediante un commit, issue, screenshot, correo sin cifrar o archivo `.env`.
El compañero no debe copiar tu `.env` al repositorio: debe crear uno propio.

### Qué puede y qué no puede publicarse

- `VITE_API_URL` puede aparecer en la configuración del frontend porque es una
  URL pública; no debe contener credenciales.
- `DB_PASSWORD`, `JWT_SECRET`, tokens JWT, refresh tokens y contraseñas reales
  son secretos y deben permanecer fuera de GitHub.
- No se deben subir `.env`, dumps de PostgreSQL, logs con headers
  `Authorization` ni capturas que muestren tokens.
- Las credenciales `admin/admin123`, `organizer/organizer123` y
  `viewer/viewer123` son únicamente para demo local. Deben cambiarse o
  deshabilitarse antes de producción.

### Ejecutar en otra máquina con Docker

Después de clonar el repositorio e instalar Docker Compose v2:

```bash
cp .env.example .env
# Editar .env: cambiar DB_PASSWORD y JWT_SECRET
docker compose config -q
docker compose up -d --build
docker compose ps
```

En esa máquina, las URLs locales son:

- GUI: `http://localhost:5173`
- API: `http://localhost:8080`
- PostgreSQL: `localhost:6969`

El backend dentro de Compose usa automáticamente `DB_HOST=db` y
`DB_PORT=5432`; no se debe cambiar a `localhost` dentro del servicio backend.
`DB_PORT=6969` solo corresponde al puerto publicado en el host.

Si la GUI se abre desde otro equipo de la red, `VITE_API_URL` debe apuntar a la
IP o dominio accesible del equipo que ejecuta el backend, por ejemplo
`http://192.168.1.20:8080/api`, y `CORS_ALLOWED_ORIGINS` debe incluir el origen
real de la GUI. Como `VITE_*` se incorpora al bundle del navegador, nunca se
deben poner secretos en variables `VITE_*`. Después de cambiar
`VITE_API_URL`, reconstruir el frontend:

```bash
docker compose up -d --build frontend
```

### Ejecutar sin Docker

Para el backend local, PostgreSQL debe estar disponible en `localhost:6969`.
Desde la raíz del repositorio, carga el `.env` en la sesión actual sin poner los
secretos directamente en un comando:

```bash
set -a
source .env
set +a
cd backend
./mvnw spring-boot:run
```

En otra terminal:

```bash
cd frontend
npm install --no-package-lock
npm run dev
```

## Ejecución con Docker

Requisitos: Docker Engine/Desktop y Docker Compose v2.

```bash
cp .env.example .env
# Editar .env y cambiar DB_PASSWORD y JWT_SECRET

docker compose config -q
docker compose build
docker compose up -d

docker compose ps
docker compose logs --tail=200 db backend frontend
```

El servicio `db` tiene healthcheck y el backend espera a que PostgreSQL esté saludable.
La persistencia usa el volumen nombrado `racing-league-db-data`.

Para detener sin borrar datos:

```bash
docker compose down
```

Para recrear desde cero, eliminando los datos de PostgreSQL:

```bash
docker compose down -v
docker compose up -d --build
```

El último comando es destructivo para la base de datos y debe ejecutarse solo cuando
no se necesiten los datos persistidos.

## Ejecución local del backend

Con PostgreSQL disponible en `localhost:6969`:

```bash
cd backend
DB_PORT=6969 DB_PASSWORD=<valor-local> ./mvnw spring-boot:run
```

El backend queda en `http://localhost:8080`.

## Ejecución local del frontend

```bash
cd frontend
npm install --no-package-lock
npm run dev
```

La GUI queda normalmente en `http://localhost:5173`. Para probar el bundle compilado:

```bash
npm run build
npm run preview
```

## API REST

Base URL local: `http://localhost:8080/api`.

### Autenticación

```text
POST /auth/register
POST /auth/login
POST /auth/refresh
GET  /auth/profile
```

Ejemplo de login:

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

Guardar el `accessToken` retornado y enviarlo así:

```bash
-H "Authorization: Bearer $ACCESS_TOKEN"
```

### Recursos

```text
GET|POST                 /competitors
GET|PUT|PATCH|DELETE     /competitors/{id}
GET|POST                 /teams
GET|PUT|DELETE           /teams/{id}
POST|DELETE              /teams/{teamId}/members/{competitorId}
GET|POST                 /races
GET|PUT|PATCH|DELETE     /races/{id}
GET|POST                 /races/{raceId}/registrations
GET|PATCH|DELETE         /registrations/{id}
GET|POST                 /races/{raceId}/results
GET|PUT                  /results/{id}
GET                      /standings
GET                      /standings/competitors
GET                      /standings/teams
GET                      /audit-logs
```

Los endpoints protegidos responden `401` sin autenticación y `403` cuando el rol no
permite la operación. Los conflictos de negocio responden `409` y los recursos
inexistentes `404`.

## Usuarios seed

| Usuario | Contraseña | Rol |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `organizer` | `organizer123` | RACE_ORGANIZER |
| `viewer` | `viewer123` | VIEWER |

Estas credenciales son para desarrollo/demo local. No reutilizarlas en producción.

## Testing

Los tests de integración (`RaceControllerTest` y el contexto principal) necesitan
una instancia PostgreSQL disponible. La forma reproducible es levantar solo la
base de datos mediante Compose y cargar el `.env` antes de ejecutar Maven:

```bash
docker compose up -d db
set -a
source .env
set +a
cd backend
./mvnw test
```

Resultado verificado: **80 tests, 0 fallos, 0 errores y 0 omitidos**.


Build frontend:

```bash
cd frontend
npm install --no-package-lock
npm run build
```

Smoke test Docker:

```bash
docker compose up -d --build
docker compose ps
curl -I http://localhost:5173/
curl -i http://localhost:8080/api/races
```

El segundo `curl` debe devolver `401` sin token; eso confirma que el backend está
activo y protegido.

## Limitaciones y mejoras futuras

- No existe administración de usuarios mediante endpoints dedicados; el seed y el
  registro público cubren el alcance actual.
- La configuración usa `ddl-auto=update`; una entrega productiva debería migrar a
  Flyway o Liquibase.
- No se incluye despliegue cloud, CI/CD, observabilidad ni notificaciones.

Las capturas de `docs/evidence/` corresponden al proceso de verificación mediante
Postman. La colección API y el diagrama ER están versionados en `docs/`.

## Entregables de Fase 13

Los artefactos de entrega están disponibles en:

- `README.md`.
- `docs/diagrama-er.dbml` y `docs/evidence/Diagrama_ERD.png`.
- `docs/API_REQUESTS.http` y las capturas de `docs/evidence/`.
- `docs/REPORTE_TECNICO.md`.
- `GUIA_ENTREGABLES_FASE13.md`.

La guía documenta la ejecución del stack, la configuración de variables de entorno
y la verificación restante de Docker desde un volumen vacío.
