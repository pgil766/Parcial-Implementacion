# Reporte técnico — EIA Camel vs. Dwarf Racing League

> **Fase 13 — Documentación y entrega**  
> Este documento describe la implementación que existe en el repositorio y toma `docs/PROJECT_SPEC.md` como especificación funcional de referencia.

## 1. Identificación del proyecto

Sistema de información para administrar una liga ficticia de carreras de camellos y enanos. La solución integra una API REST, una interfaz web, persistencia PostgreSQL, autenticación/autorización y ejecución containerizada.

**Equipo:**
- TODO — completar nombres, códigos y roles de cada integrante.
- TODO — completar repositorio GitHub, branch o enlace de entrega si el formato institucional lo exige.

## 2. Arquitectura de la solución

La aplicación está separada en tres servicios desplegables:

1. **Backend**: Java 21 y Spring Boot (`edu.eia.racing`), expone la API REST bajo `/api`.
2. **Frontend**: React 19 con Vite, cliente web que consume exclusivamente la API.
3. **Base de datos**: PostgreSQL persistente en Docker.

El backend aplica arquitectura por capas:

- `controller`: recibe HTTP, ejecuta validación de formato y devuelve códigos/respuestas.
- `service`: concentra reglas de negocio, transiciones de carreras, inscripciones, resultados, estadísticas y coordinación transaccional.
- `repository`: interfaces Spring Data JPA para acceso a PostgreSQL.
- `model`: entidades JPA y enumeraciones (`model.enums`).
- `dto`: contratos de entrada/salida (`*Request`, `*Response`); las entidades no se exponen directamente.
- `security`: `SecurityConfig`, `JwtService`, `JwtAuthenticationFilter`, `CustomUserDetailsService` y handlers 401/403.
- `exception`: `GlobalExceptionHandler`, `ApiError` y excepciones de dominio para respuestas consistentes.
- `config`: CORS y carga de datos iniciales (`CorsConfig`, `DataSeeder`).

La secuencia normal es **HTTP → controller → service → repository → PostgreSQL** y la respuesta vuelve mediante DTO. La GUI no tiene conexión ni credenciales de base de datos.

## 3. Modelo de datos

El modelo contiene las nueve entidades JPA requeridas:

| Entidad | Propósito y relaciones principales |
|---|---|
| `User` | Cuenta, correo único, `passwordHash`, estado habilitado y relación con `Role`; también identifica acciones registradas. |
| `Role` | Catálogo de roles (`ADMIN`, `RACE_ORGANIZER`, `VIEWER`). |
| `Competitor` | Nombre, nickname único, tipo, fecha de nacimiento, peso, altura, origen, estado y estadísticas de victorias/derrotas/carreras. |
| `Team` | Equipo con nombre único, descripción, coach, estado, límite configurable y estadísticas. |
| `TeamMember` | Asociación histórica entre `Team` y `Competitor`, con fechas y bandera `active`; restricción única por pareja. |
| `Race` | Carrera, horario, ruta, distancia, capacidad, tipo, estado, organizador y fecha límite de inscripción. |
| `RaceRegistration` | Inscripción a carrera de un competidor o equipo, estado, posición de salida, notas y usuario que la registra. |
| `RaceResult` | Resultado de una inscripción: posiciones, tiempos, penalización, estado, notas y usuario registrador. |
| `AuditLog` | Auditoría de usuario, acción, entidad afectada, timestamp, descripción y valores anteriores/nuevos. |

Las relaciones usan claves foráneas JPA (`@ManyToOne`) y carga `LAZY` en los vínculos principales. Hay restricciones únicas para nicknames, nombres de equipo, usuarios/correos, membresías, duplicidad de inscripción por carrera y posición de salida, y resultados por inscripción/posición final. También existen índices para búsquedas frecuentes de estado/tipo, organizador, usuario de auditoría y entidades auditadas.

Enumeraciones del dominio: `CompetitorType`, `CompetitorStatus`, `RaceType`, `RaceStatus`, `RegistrationStatus`, `ResultStatus`, `RoleName` y `TeamStatus`.

El `DataSeeder` prepara las cuentas iniciales (un administrador, un organizador y un
viewer), competidores de los tipos requeridos, equipos y carreras en distintos
estados, incluyendo datos para resultados. Las contraseñas de ejemplo son
únicamente las cuentas seed locales documentadas en las pruebas; los secretos de
despliegue se inyectan por entorno.

El diagrama entidad-relación editable está en `docs/diagrama-er.dbml` y su versión
visual está disponible en `docs/evidence/Diagrama_ERD.png`.

## 4. Seguridad y matriz de roles

La autenticación es manual con **BCrypt** para contraseñas y **JWT + Spring Security**. El flujo es:

1. El cliente envía credenciales a `POST /api/auth/login` (o crea cuenta con `POST /api/auth/register`).
2. El backend valida la cuenta habilitada y la contraseña BCrypt.
3. Se devuelven access token y refresh token JWT; ambos incluyen el claim `type` para distinguir su uso.
4. El frontend conserva los tokens en `localStorage` y envía el access token como `Authorization: Bearer ...`.
5. `JwtAuthenticationFilter` valida el token y carga la identidad/rol antes de invocar el controlador.
6. Si el access token expira, el cliente intenta `POST /api/auth/refresh`; si falla, limpia la sesión y retorna al login.
7. La autorización del backend se aplica con `@EnableMethodSecurity` y `@PreAuthorize`; no depende solo de ocultar botones.

Endpoints de autenticación: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh` y `GET /api/auth/profile`.

| Rol | Lectura | Escritura/operación |
|---|---|---|
| **ADMIN** | Competidores, equipos, carreras, inscripciones, resultados, standings y auditoría | CRUD de competidores y equipos; membresías; CRUD/transiciones de carreras; inscripciones; resultados; lectura completa de auditoría y administración prevista de usuarios. |
| **RACE_ORGANIZER** | Información de competidores/equipos, carreras, inscripciones, resultados y standings | Crear/editar/eliminar/transicionar carreras; crear, aprobar, rechazar y eliminar inscripciones; registrar/editar resultados. No administra competidores, equipos ni auditoría. |
| **VIEWER** | Lectura de información de liga, carreras, inscripciones/resultados publicados y standings | No puede ejecutar operaciones protegidas. La API responde `403 Forbidden` al intentar crear carreras u otras mutaciones restringidas. |

La ausencia de credenciales devuelve `401 Unauthorized`; autenticación válida sin permiso devuelve `403 Forbidden`. El perfil y las respuestas de autenticación no retornan el hash de contraseña. `DB_PASSWORD`, `JWT_SECRET` y demás valores sensibles se configuran mediante `.env`/variables de entorno y no deben versionarse.

## 5. API REST y manejo de errores

Los controladores implementados son `AuthController`, `CompetitorController`, `TeamController`, `RaceController`, `RegistrationController`, `ResultController` y `AuditLogController`. Los recursos siguen la semántica HTTP: `GET` consulta, `POST` crea (`201`), `PUT` actualiza completamente, `PATCH` cambia estado/decisión y `DELETE` elimina cuando la integridad lo permite (`204`).

Principales recursos:

- `/api/competitors`: CRUD, estado y consulta paginada con filtros de nombre/nickname/tipo/estado.
- `/api/teams`: CRUD y `/api/teams/{teamId}/members/{competitorId}` para membresías.
- `/api/races`: CRUD y transición en `/status`.
- `/api/races/{raceId}/registrations` y `/api/registrations/{id}`: inscripción, consulta, aprobación y rechazo.
- `/api/races/{raceId}/results` y `/api/results/{id}`: resultados.
- `/api/standings`, `/api/standings/competitors`, `/api/standings/teams`: clasificaciones.
- `/api/audit-logs`: consulta paginada exclusiva de `ADMIN`.

`GlobalExceptionHandler` normaliza validaciones y errores de negocio en un objeto con `timestamp`, `status`, `error`, `message` y `path`. Se usan `400`, `401`, `403`, `404` y `409` según el tipo de fallo; no se exponen stack traces.

## 6. Interfaz web

`frontend/src/App.jsx` define una shell de navegación y vistas para dashboard, calendario de carreras, competidores, equipos, standings, inscripciones, resultados y perfil. `frontend/src/api.js` centraliza `fetch`, el encabezado Bearer, renovación de sesión y traducción básica de errores mediante `ApiError`.

Comportamiento observable:

- Login con validación de campos y mensajes de error.
- Dashboard con próximas carreras, competidores activos, líder y resultados/standings.
- Búsqueda, filtros y paginación de competidores; formularios de alta/edición para roles autorizados.
- Gestión de equipos y miembros, calendario y formularios de carreras.
- Cola de inscripciones con aprobación/rechazo y razón; mesa de resultados; leaderboard.
- Perfil y logout seguro.
- Rutas desconocidas muestran pantalla `404`; operaciones no autorizadas muestran pantalla `403`.
- Acciones de escritura se ocultan o deshabilitan según rol; el backend sigue siendo la autoridad.
- Las listas muestran estados de carga, vacío y error; la UI valida campos requeridos y valores positivos antes de llamar a la API.

## 7. Reglas de negocio implementadas

- Competidores: nickname único; nombre no vacío; peso/altura positivos; tipo y estado enumerados; solo `ACTIVE` es elegible para inscripción; un registro con resultados, inscripciones o membresías protegidas no se elimina físicamente.
- Equipos: nombre único; límite máximo configurable; sin duplicar miembros; un competidor no pertenece a más de un equipo activo; al retirar se conserva el histórico; la eliminación se protege si existe historial de carreras.
- Carreras: fecha futura al crear; deadline anterior al inicio; distancia y capacidad positivas; organizador válido; transiciones controladas; no se edita una carrera completada; una carrera cancelada no recibe inscripciones; iniciar requiere al menos dos participantes aprobados; completar requiere resultados oficiales; se usa bloqueo pesimista en mutaciones/historial.
- Inscripciones: solo carrera abierta y antes del deadline; sin duplicados; competidor activo; tipo de inscripción compatible con tipo de carrera; equipo elegible; posiciones de salida únicas; se evita competir simultáneamente como individuo y miembro de equipo; rechazo con razón.
- Resultados: carrera `IN_PROGRESS`; inscripción aprobada; tiempo positivo para finalizados; posiciones finales no duplicadas; descalificado no gana; un ganador oficial; resultados no finalizados no reciben posición inválida. Los puntos son 10/7/5/3/1 para puestos 1–5 y 0 para no finalizado/descalificado. Al actualizar, se sincronizan las estadísticas persistidas.
- Auditoría: se registran login, alta de usuarios, cambios de competidores, cancelaciones, decisiones de inscripción y modificaciones de resultados; solo `ADMIN` consulta el log completo.

## 8. Evidencia de testing

Según el resultado consignado en `docs/TASKS.md`, la suite backend completa ejecutada contra PostgreSQL terminó con **80 tests, 0 fallos, 0 errores y 0 omitidos**. Usa JUnit 5, Mockito y pruebas de controlador con Spring Boot/MockMvc.

La cobertura significativa incluye autenticación (login, BCrypt, refresh, tokens inválidos, `401`, no exposición del hash), competidores (validación, nickname duplicado, filtros/paginación, conflictos y eliminación protegida), equipos (capacidad, membresía histórica y equipos activos), carreras (fechas, transiciones, capacidad, organizador, concurrencia y resultados), inscripciones (elegibilidad, deadline, duplicados, tipo y posición), resultados (puntos, posiciones y estadísticas), auditoría y autorización. También se verificó explícitamente que `VIEWER` recibe `403` y `ADMIN` puede crear una carrera (`201`).

Frontend: `docs/TASKS.md` registra `npm install --no-package-lock`, `npm run build` y smoke test con `npm run preview` exitosos, incluyendo ruta 404, restricción de `VIEWER`, estados y renovación ante `401`.

## 9. Docker y ejecución

`compose.yml` define tres servicios en la red aislada `racing-league-net`:

| Servicio | Imagen/build | Puerto publicado | Puerto interno |
|---|---|---:|---:|
| `db` | PostgreSQL fijado por digest | **6969** | **5432** |
| `backend` | `backend/Dockerfile` | **8080** | **8080** |
| `frontend` | `frontend/Dockerfile` + Nginx | **5173** | **80** |

La base de datos usa el volumen nombrado `racing-league-db-data`, healthcheck
`pg_isready` y el backend espera `db` saludable. El frontend recibe `VITE_API_URL`
en build y por defecto llama a `http://localhost:8080/api`. La configuración del
backend usa `DB_HOST=db`, `DB_PORT=5432`, `SERVER_PORT=8080`, expiraciones JWT y
orígenes CORS desde variables de entorno.

Comando previsto: `docker compose up -d`. Se registran como verificaciones
realizadas `docker compose config -q`, validación sintáctica de Dockerfiles,
empaquetado del backend y smoke test del stack. Queda como verificación operativa
final repetir el levantamiento desde volumen vacío para demostrar el criterio
“desde cero”.

El diagrama ER editable y su evidencia PNG están disponibles en
`docs/diagrama-er.dbml` y `docs/evidence/Diagrama_ERD.png`. La colección
versionable `docs/API_REQUESTS.http` y las capturas de `docs/evidence/` documentan
el proceso de verificación mediante Postman.

## 10. Limitaciones y mejoras futuras

- La configuración usa `localStorage` para tokens; una evolución de seguridad puede
  migrar a cookies `HttpOnly`/`Secure` con protección CSRF y añadir rotación/revocación
  de refresh tokens.
- Mejoras posibles: migraciones Flyway/Liquibase, Testcontainers, rate limiting,
  observabilidad, exportación CSV/PDF, soft delete explícito, optimistic locking,
  idempotency keys y pruebas frontend automatizadas.
## 11. Verificación final a cargo del usuario

Antes de entregar, el usuario debe completar y registrar:

- [ ] Nombres, códigos, roles y participación de todos los integrantes.
- [ ] URL del repositorio y evidencia de branches/commits/PRs requeridos.
- [x] Diagrama entidad-relación editable en `docs/diagrama-er.dbml` y evidencia
      visual en `docs/evidence/Diagrama_ERD.png`.
- [x] Colección API `docs/API_REQUESTS.http` y capturas del proceso de Postman en
      `docs/evidence/`.
- [ ] `cp .env.example .env`, definir valores reales locales y ejecutar
      `docker compose up -d` desde un volumen limpio.
- [ ] Confirmar `http://localhost:5173` (GUI), `http://localhost:8080` (API) y
      PostgreSQL en `localhost:6969`.
- [x] Ejecutar el flujo funcional en la GUI: login, competidor/equipo, carrera,
      inscripción, resultado y standings.
- [x] Mostrar restricciones de `ADMIN`, `RACE_ORGANIZER` y `VIEWER`, incluyendo
      respuestas 401/403.
- [x] Mostrar errores de negocio/validación y sus mensajes.
- [x] Ejecutar la suite backend de 80 pruebas y verificar build/smoke del frontend.
