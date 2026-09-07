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

## Próximo paso inmediato

**Empezar la Fase 3 (Competidores, Módulo 2 del spec).** Antes de codear: plantear
el plan (entidades ya existen de Fase 1, faltan DTOs/service/controller/reglas de
negocio/tests) y esperar aprobación — así lo pide `context/CLAUDE.md`. No asumas
que el usuario ya aprobó nada de Fase 3 solo porque este archivo lo liste como
"siguiente"; sigue siendo una fase nueva que requiere plan + luz verde explícita.

**Antes de nada, revisa `git status`.** La Fase 2 (todo `security/`, `dto/`,
`exception/`, `controller/`, `service/`, más los ajustes de `DataSeeder`,
`application.yml`, `.env.example`) está terminada y verificada pero **todavía sin
commitear** a fecha de este párrafo — el usuario dijo explícitamente que él hace
los commits/push, no tú. Si en tu sesión ya aparece commiteada, ignora esta nota;
si no, no la recommitees tú tampoco, solo continúa trabajando sobre ese estado.

## Checklist real verificado (no confíes en "está casi listo" — cuenta tú mismo)

`docs/TASKS.md` tiene 61 ítems `[ ]`/`[x]` en total. Estado verificado por conteo
directo (no por impresión general):

| Fase | Items | Fase | Items |
|---|---|---|---|
| 0 — Setup | ✅ 5/5 | 7 — Resultados | ❌ 0/4 |
| 1 — Modelo de datos | 4/5 (falta diagrama ER) | 8 — Auditoría | ❌ 0/2 |
| 2 — Auth | ✅ 6/6 | 9 — Errores globales | ❌ 0/3 |
| 3 — Competidores | ❌ 0/4 | 10 — Frontend | ❌ 0/12 |
| 4 — Equipos | ❌ 0/3 | 11 — Docker completo | ❌ 0/4 |
| 5 — Carreras | ❌ 0/3 | 12 — Testing final | ❌ 0/2 |
| 6 — Inscripciones | ❌ 0/3 | 13 — Documentación | ❌ 0/5 |

Solo existen dos controllers/services en todo el código: ninguno — literalmente
solo `AuthController`/`AuthService`. No hay `CompetitorController`, `TeamController`,
`RaceController`, etc. No tomes atajos asumiendo que "ya debe estar" algo de las
fases 3-13: no está, cuenta los archivos si tienes dudas
(`find backend/src/main/java -name "*Controller.java"`).

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
| Frontend | React (aún no iniciado — planeado para Fase 10, no antes) |
| Seguridad | JWT + Spring Security, hashing con BCrypt (dependencia `io.jsonwebtoken:jjwt` ya en el pom) |
| Testing | JUnit 5 + Mockito (mínimo 15 tests significativos) |
| Contenedores | Docker + Docker Compose (`compose.yml`, no `docker-compose.yml`) |

## Estado actual (última actualización: tras Fase 2)

- **Fases 0, 1 y 2 completas.** Fases 0-1 ya están en `main`
  (`https://github.com/pgil766/Parcial-Implementacion.git`); la Fase 2 está hecha y
  verificada pero **sin commitear** — el usuario hace los commits y push él mismo,
  no toques git/GitHub salvo que te lo pida explícitamente.
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
  - `exception/`: `ApiError` (formato del spec §5), `DuplicateResourceException` (409),
    `InvalidCredentialsException` (401) y `GlobalExceptionHandler`. **Ojo:** el handler
    tiene un `@ExceptionHandler(AccessDeniedException.class)` explícito; sin él, el
    catch-all de `Exception` convertiría los 403 de `@PreAuthorize` en 500.
  - Registro público crea **siempre VIEWER** (si dejara elegir rol, cualquiera se
    haría ADMIN). Login usa el mismo mensaje para usuario inexistente y password
    mala, para no permitir enumerar usuarios.
  - **No hay endpoint de logout**: con JWT stateless el cliente simplemente descarta
    los tokens (lo hará la GUI en Fase 10).
- `application.yml` parametrizado 100% por variables de entorno
  (`DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD/JWT_SECRET/JWT_EXPIRATION/
  SERVER_PORT`), `ddl-auto=update` (no hay Flyway/Liquibase — no están en el
  alcance elegido, son bonus opcionales del spec §12).
- `compose.yml` en la raíz: **solo el servicio `db`** por ahora. Falta el backend
  (Dockerfile propio, Fase 11) y el frontend.
- Todo verificado contra Postgres real (no H2): `./mvnw test` pasa con **19 tests**
  (18 significativos + 1 smoke), tablas/FKs/constraints/índices confirmados con `psql`,
  y los endpoints de auth probados end-to-end con `curl` contra la app corriendo.
- Diagrama entidad-relación: **pendiente**, no generado aún.

## Estado objetivo (lo que falta — ver `docs/TASKS.md` para el detalle fase por fase)

Fases 3 a 13, en orden, una a la vez (0-2 ya están hechas):

3. Competidores (CRUD + reglas de negocio + filtrado/paginación).
4. Equipos (CRUD + gestión de miembros vía `TeamMember`).
5. Carreras (CRUD + máquina de estados `RaceStatus`).
6. Inscripciones (`RaceRegistration` + aprobación/rechazo).
7. Resultados y clasificaciones (`RaceResult` + sistema de puntos del spec §Módulo 6).
8. Log de auditoría (`AuditLog`, solo lectura para ADMIN).
9. Manejo de errores global (`exception/`, formato estructurado del spec §5).
10. Frontend React (todas las pantallas del spec §Módulo 7).
11. Dockerización completa (Dockerfiles backend+frontend, `compose.yml` final).
12. Testing (mínimo 15 tests significativos, cubrir casos sugeridos del spec §8).
13. Documentación y entrega (README, diagrama ER, Postman, video demo).

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
