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

## Estado actual (última actualización: tras Fase 1)

- **Fase 0 y 1 completas, commiteadas y pusheadas** a `main`
  (`https://github.com/pgil766/Parcial-Implementacion.git`).
- Backend generado en `backend/` vía Spring Initializr, con Maven Wrapper
  (`./mvnw` / `mvnw.cmd`) — **no asumas Maven global instalado**, usa siempre el
  wrapper. JDK 21 (Temurin) sí quedó instalado en la máquina de desarrollo.
- Paquete base: `edu.eia.racing`.
- Estructura de paquetes obligatoria ya creada bajo
  `backend/src/main/java/edu/eia/racing/`:
  `controller/ service/ repository/ model/ model/enums/ dto/ security/ exception/ config/`.
  `controller`, `service`, `dto`, `security`, `exception` están **vacíos a propósito**
  (les toca en fases 2+).
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
  (una `COMPLETED` con resultados y ganador). Usa `BCryptPasswordEncoder`
  instanciado directo — **cuando se implemente Fase 2, refactorizar para inyectar
  el bean `PasswordEncoder` compartido en vez de duplicarlo aquí**.
- `application.yml` parametrizado 100% por variables de entorno
  (`DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD/JWT_SECRET/JWT_EXPIRATION/
  SERVER_PORT`), `ddl-auto=update` (no hay Flyway/Liquibase — no están en el
  alcance elegido, son bonus opcionales del spec §12).
- `compose.yml` en la raíz: **solo el servicio `db`** por ahora. Falta el backend
  (Dockerfile propio, Fase 11) y el frontend.
- Todo verificado contra Postgres real (no H2): `./mvnw test` pasa, tablas/FKs/
  constraints/índices confirmados con `psql`.
- Sin seguridad real todavía: Spring Boot está corriendo con su usuario en memoria
  autogenerado (verás "Using generated security password" en los logs). Es
  esperado — no es un bug, es que `security/` sigue vacío.
- Diagrama entidad-relación: **pendiente**, no generado aún.

## Estado objetivo (lo que falta — ver `docs/TASKS.md` para el detalle fase por fase)

Fases 2 a 13, en orden, una a la vez:

2. Auth (JWT + Spring Security, roles ADMIN/RACE_ORGANIZER/VIEWER, BCrypt).
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
