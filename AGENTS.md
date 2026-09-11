# AGENTS.md — EIA Camel vs. Dwarf Racing System

Guía de orientación rápida para cualquier agente (Claude Code u otro) que entre a
trabajar en este repo sin contexto previo. No duplica el detalle funcional completo
— para eso están los documentos fuente listados abajo. Léelos en este orden:

1. `context/CLAUDE.md` — reglas de colaboración con el usuario (cómo debe trabajar
   el agente: plan antes de código, módulo por módulo, confirmar antes de avanzar).
2. `docs/PROJECT_SPEC.md` — **fuente de verdad funcional**: reglas de negocio,
   endpoints sugeridos, criterios de evaluación. Es un proyecto académico (Universidad
   EIA) evaluado con rúbrica; sección 15 tiene los pesos exactos.
3. `docs/TASKS.md` — checklist de las 14 fases del plan. Marca `[x]` al terminar cada
   ítem verificado (compila + tests pasan), no antes.

## Estado actual

**Finalizado con lo solicitado (abierto a cambios post etapa final).**

Las fases funcionales, frontend, auditoría, manejo global de errores,
dockerización, testing y documentación están implementadas. El frontend React/Vite
se encuentra en `frontend/`; la API y la base PostgreSQL se ejecutan mediante
`compose.yml`.

La suite backend ejecuta **80 pruebas, 0 fallos, 0 errores y 0 omitidos** con
PostgreSQL 16 disponible en `DB_PORT=6969`. El build del frontend y la validación
de configuración Compose también fueron verificados.

El diagrama editable está en `docs/diagrama-er.dbml` y la evidencia visual en
`docs/evidence/Diagrama_ERD.png`. Las demás capturas de `docs/evidence/`
corresponden al proceso de verificación realizado mediante Postman.

Antes de nada, revisa `git status`. El usuario hace los commits y push él mismo;
no toques git/GitHub salvo que lo pida explícitamente.

## Qué es el proyecto

Sistema de información (REST API + BD + GUI, todo dockerizado) para una liga
ficticia de carreras "camello vs. enano". El envoltorio narrativo es cosmético; el
contenido técnico (auth por roles, CRUD con reglas de negocio, testing, Docker) es
real y se evalúa en serio.

## Stack tecnológico (ya decidido — no renegociar sin el usuario)

| Capa | Elección |
|---|---|
| Backend | Java 21 + Spring Boot 4.1.1 (Web/MVC, Data JPA, Validation, Security) |
| Base de datos | PostgreSQL 16 (contenedor Docker, volumen nombrado `racing-league-db-data`) |
| Frontend | React 19 + Vite + Nginx |
| Seguridad | JWT + Spring Security, hashing con BCrypt |
| Testing | JUnit 5 + Mockito (80 pruebas verificadas) |
| Contenedores | Docker + Docker Compose (`compose.yml`) |

La implementación completa se encuentra en `backend/` y `frontend/`, con tres
servicios definidos en `compose.yml`: PostgreSQL, backend y frontend.
- Backend generado en `backend/` vía Spring Initializr, con Maven Wrapper
  (`./mvnw` / `mvnw.cmd`) — **no asumas Maven global instalado**, usa siempre el
  wrapper. JDK 21 (Temurin) sí quedó instalado en la máquina de desarrollo.
- Paquete base: `edu.eia.racing`.
- Estructura de paquetes obligatoria ya creada bajo
  `backend/src/main/java/edu/eia/racing/`:
  `controller/ service/ repository/ model/ model/enums/ dto/ security/ exception/ config/`.
- `model/`: 9 entidades JPA — `Role, User, Competitor, Team, TeamMember, Race,
  RaceRegistration, RaceResult, AuditLog` — con validaciones Bean Validation,
  constraints únicos, FKs, e índices explícitos en toda columna FK (Postgres no
  indexa FKs automáticamente, solo PK y UNIQUE — ya se corrigió este vacío una vez,
  no lo repitas).
- `model/enums/`: `RoleName, CompetitorType, CompetitorStatus, TeamStatus, RaceType,
  RaceStatus, RegistrationStatus, ResultStatus`.
- `repository/`: 9 interfaces Spring Data JPA (Competitor y Race con
  `JpaSpecificationExecutor` para el filtrado/paginación que exige el spec).
- `config/DataSeeder.java`: `CommandLineRunner` idempotente (se salta si ya hay
  roles) que carga el seed mínimo del spec (sección 7): 1 admin/1 organizer/1
  viewer, 5 dwarfs/2 camels/2 medium, 2 equipos, 3 carreras en distintos estados
  (una `COMPLETED` con resultados y ganador). Credenciales del seed:
  `admin/admin123`, `organizer/organizer123`, `viewer/viewer123`.
- `config/CorsConfig.java`: `CorsConfigurationSource` para que el React de Fase 10
  pueda llamar la API (orígenes vía `CORS_ALLOWED_ORIGINS`).
- **Fase 2 (auth) implementada:**
  - `security/`: `JwtService` (access + refresh, claim `type` para que no sean
    intercambiables), `JwtAuthenticationFilter`, `CustomUserDetailsService`
    (mapea `RoleName` → `ROLE_*`), `SecurityConfig` (stateless, `@EnableMethodSecurity`
    listo para `@PreAuthorize`), `JwtAuthenticationEntryPoint` (401) y
    `JwtAccessDeniedHandler` (403).
  - `dto/`: `RegisterRequest`, `LoginRequest`, `RefreshRequest`, `AuthResponse`,
    `UserProfileResponse` — ninguno expone `passwordHash`.
  - `service/AuthService` + `controller/AuthController`:
    `POST /api/auth/register|login|refresh`, `GET /api/auth/profile`.
  - Registro público crea siempre `VIEWER`; login usa el mismo mensaje para usuario
    inexistente y password mala.
  - No hay endpoint de logout: con JWT stateless el cliente descarta los tokens.
- **Fase 3 (competidores) implementada:**
  - `CompetitorController` expone CRUD, cambio de estado y lecturas autenticadas.
  - `CompetitorService` normaliza y asegura nickname único, filtra/pagina/ordena,
    devuelve `teamId` opcional desde membresía activa y protege eliminaciones con
    resultados, inscripciones o membresías.
  - Mutaciones restringidas a `ADMIN`; respuestas usan DTOs y errores estructurados.
  - La regla de elegibilidad `ACTIVE` queda en el flujo de inscripciones de Fase 6.
- **Fase 5 (carreras) implementada:**
  - `RaceController` y `RaceService` exponen CRUD, transiciones estrictas de
    `RaceStatus`, validación de fechas/capacidad y respuestas DTO.
  - Mutaciones restringidas a `ADMIN`/`RACE_ORGANIZER`; cambios y borrados bloquean
    la fila con pesimismo para preservar invariantes ante concurrencia.
  - No permite reducir capacidad bajo inscripciones aprobadas ni cambiar el tipo
    incompatiblemente; carreras completadas y con historial oficial no se editan
    ni eliminan.
- **Fase 6 (inscripciones) implementada:**
  - `RegistrationController` y `RegistrationService` cubren creación, consulta,
    aprobación, rechazo, elegibilidad, posiciones y eliminación protegida.
  - La transición a `IN_PROGRESS` revalida competidores, equipos y miembros activos.
  - Inscripciones con resultados oficiales no pueden eliminarse; los duplicados
    respetan las constraints de persistencia y no se permiten reintentos en la misma
    carrera.
- **Fase 7 (resultados y standings) implementada:**
  - `ResultController` y `ResultService` cubren registro, actualización y consulta.
  - Solo acepta inscripciones aprobadas en carreras `IN_PROGRESS`; aplica posiciones
    únicas, estados válidos y puntos 10/7/5/3/1.
  - Expone standings generales, de competidores y de equipos.
La suite completa ejecuta 80 pruebas y pasa con PostgreSQL 16 levantado mediante
las variables de entorno documentadas en `README.md`. `application.yml` usa
`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`,
`JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `CORS_ALLOWED_ORIGINS` y `SERVER_PORT`.
El frontend usa `VITE_API_URL`.

El diagrama entidad-relación editable está en `docs/diagrama-er.dbml`; su PNG y
las capturas del proceso de Postman están en `docs/evidence/`.

## Estado posterior a la etapa final

El estado del proyecto es: **Finalizado con lo solicitado (abierto a cambios post
etapa final)**. Cualquier modificación posterior debe conservar la separación
frontend/API, la configuración por variables de entorno y la suite de pruebas
existente.

## Reglas no negociables (repetidas de `context/CLAUDE.md`, por si no se lee)

- La GUI **nunca** toca la BD directamente, solo consume el REST API.
- Las entidades JPA **nunca** se exponen directamente en la API — siempre DTOs.
- Errores de API siempre en el formato estructurado del spec (timestamp, status,
  error, message, path) — nunca stack traces crudos.
- Secretos nunca se commitean (`.env` está gitignored, `.env.example` no tiene
  credenciales reales) ni se retornan en respuestas de API.
- Toda regla de negocio del spec debe existir en **código**, no solo en docs.
- Antes de codear un módulo nuevo: plantear el plan (entidades/endpoints/reglas) y
  esperar aprobación del usuario. Trabajar módulo por módulo. Después de cada
  módulo: compilar, correr tests, y confirmar con el usuario antes de seguir.
  Actualizar el checklist de `docs/TASKS.md` solo con lo verificado.

## Particularidades del entorno de desarrollo (Windows)

- Shell principal: PowerShell. El PATH de Java (`JAVA_HOME` = Eclipse Temurin 21)
  se instaló vía `winget` a nivel de máquina, pero **cada proceso nuevo de
  PowerShell no lo hereda automáticamente** dentro de esta sesión de agente — si
  `java`/`mvnw` no se reconoce, refrescar con:
  `$env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")`
- Spring Boot **no** lee archivos `.env` automáticamente. Antes de correr
  `./mvnw test` o `spring-boot:run` fuera de Docker, hay que exportar las
  variables del `.env` a la sesión (ver ejemplo en el historial de comandos, o
  simplemente usar Docker Compose que sí las inyecta vía `env_file`/`environment`).
- Docker Desktop en esta máquina no arranca solo — si `docker compose` falla con
  error de pipe/daemon, hay que lanzar `Docker Desktop.exe` y esperar a que el
  engine esté listo antes de reintentar.
- No hay Maven global instalado (deliberado, ver tabla de stack) — usar siempre
  `./mvnw` (Linux/Mac/Git Bash) o `mvnw.cmd` (PowerShell/CMD), nunca `mvn` a secas.

## Trampas de Spring Boot 4 / Spring Security 7 (verificadas en este repo)

Este proyecto usa Spring Boot **4.1.1**, no 3.x. Varias cosas que la mayoría de
tutoriales dan por sentado no compilan aquí:

- **Jackson 3, no Jackson 2.** El paquete es `tools.jackson.databind.ObjectMapper`.
  `com.fasterxml.jackson.*` **no está en el classpath**.
- **`@AutoConfigureMockMvc` se movió** a
  `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
  (ya no `org.springframework.boot.test.autoconfigure.web.servlet`).
- **`org.springframework.lang.NonNull` está deprecado** en Spring 7. No lo uses;
  simplemente omite la anotación.
- El starter web es **`spring-boot-starter-webmvc`**, no `spring-boot-starter-web`,
  y los starters de test vienen partidos (`spring-boot-starter-webmvc-test`, etc.).
- **Surefire solo corre clases `*Test`/`*Tests`.** Una clase llamada `...IT` se
  ignora en `./mvnw test` (necesitaría failsafe en la fase `verify`). Por eso los
  tests de integración de este repo se llaman `*Test`.
- **`Keys.hmacShaKeyFor` exige ≥32 bytes** para HS256: un `JWT_SECRET` corto revienta
  la app al arrancar. El default de `application.yml` ya es largo por eso.

## Convenciones de código ya establecidas (seguir el mismo patrón)

- Entidades: Lombok (`@Getter @Setter @NoArgsConstructor @AllArgsConstructor
  @Builder`), `@PrePersist`/`@PreUpdate` para timestamps automáticos, Bean
  Validation (`@NotBlank`, `@Positive`, `@Past`, etc.) — se valida automáticamente
  vía Hibernate+Bean Validation, no hace falta invocarlo a mano.
  Sin `@EqualsAndHashCode` custom (se usa identidad por defecto, suficiente para
  el alcance de este proyecto).
- IDs: `Long` con `GenerationType.IDENTITY` (nativo de Postgres).
- Toda FK debe llevar su propio `@Index` explícito en `@Table(indexes = ...)`
  además de las `uniqueConstraints` que ya cubren algunas por ser líderes de un
  índice compuesto — no asumir que Postgres las crea solo.
- Repositorios: interfaces `JpaRepository`, sin implementación custom salvo que
  se necesite lógica de filtrado dinámico (usar `JpaSpecificationExecutor`, ya
  aplicado en `CompetitorRepository`/`RaceRepository`).
