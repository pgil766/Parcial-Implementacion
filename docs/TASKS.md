# Plan de implementación por fases

Trabaja UNA fase a la vez con Claude Code. No pases a la siguiente hasta que la
anterior compile, tenga tests pasando y tú la hayas revisado.

## Fase 0 — Setup del proyecto
- [x] Inicializar repo Git con `.gitignore` (Java + Node/frontend según aplique)
- [x] Crear estructura de paquetes Spring Boot (controller/service/repository/model/dto/security/exception/config)
- [x] Configurar `compose.yml` con servicio de BD (aún sin backend)
- [x] Verificar que `docker compose up -d` levanta la BD correctamente
- [x] Crear `.env.example`

## Fase 1 — Modelo de datos y BD
- [x] Definir entidades JPA: User, Role, Competitor, Team, TeamMember, Race, RaceRegistration, RaceResult, AuditLog
- [x] Definir enums: CompetitorType, CompetitorStatus, RaceType, RaceStatus, RegistrationStatus, ResultStatus (+ RoleName, TeamStatus)
- [x] Configurar relaciones, constraints únicos, índices
- [x] Diagrama entidad-relación editable en `docs/diagrama-er.dbml` y evidencia visual en `docs/evidence/Diagrama_ERD.png`
- [x] Seed de datos iniciales (1 admin, 1 organizer, 1 viewer, 5 dwarfs, 2 camels, 2 medium, 2 equipos, 3 carreras)

## Fase 2 — Autenticación y seguridad (Módulo 1)
- [x] Registro y login
- [x] Hashing de contraseñas (BCrypt)
- [x] Generación y validación de tokens (JWT access + refresh, con claim `type`)
- [x] Middleware/filtro de autorización por rol (`JwtAuthenticationFilter` + `@EnableMethodSecurity`)
- [x] Endpoints: /api/auth/register, /login, /refresh, /profile
- [x] Tests: 401 sin token (18 tests de auth pasando; el test de *permisos por rol*
      llega en Fase 3, cuando existan endpoints con `@PreAuthorize` que restringir)

## Fase 3 — Competidores (Módulo 2)
- [x] CRUD completo con DTOs
- [x] Reglas de negocio: nickname único y eliminación física protegida por resultados, inscripciones o membresías; la elegibilidad `ACTIVE` se valida al inscribir en Fase 6
- [x] Filtrado, paginación, ordenamiento
- [x] Tests: 21 tests de competidores; suite total de 40 tests pasando

## Fase 4 — Equipos (Módulo 3)
- [x] CRUD + gestión de miembros
- [x] Reglas de membresía: no duplicados, un solo equipo activo por competidor y máximo configurable; el mínimo antes de entrar a una carrera se valida al implementar carreras/inscripciones
- [x] Tests correspondientes (5 pruebas unitarias de TeamService)

## Fase 5 — Carreras (Módulo 4)
- [x] CRUD + transiciones de estado (PATCH status)
- [x] Reglas de negocio: fecha futura, deadline < inicio, no editar si COMPLETED; capacidad, tipo y organizador consistentes
- [x] Concurrencia: bloqueo pesimista en mutaciones y protección del historial oficial
- [x] Tests: 10 pruebas unitarias de RaceService; suite total de 80 tests pasando

## Fase 6 — Inscripciones (Módulo 5)
- [x] Endpoints de inscripción, aprobación, rechazo
- [x] Reglas de negocio: elegibilidad ACTIVE, no duplicados, tipo debe coincidir, posiciones de salida únicas y razón de rechazo
- [x] Tests: 10 pruebas unitarias de RegistrationService; suite total de 80 tests pasando

## Fase 7 — Resultados y clasificaciones (Módulo 6)
- [x] Registro de resultados + cálculo de puntos
- [x] Endpoint de standings (competidores y equipos)
- [x] Reglas de negocio: solo un ganador, no duplicar posiciones, solo en carreras IN_PROGRESS
- [x] Tests: 7 pruebas unitarias de ResultService; incluyen sincronización de estadísticas persistidas

## Fase 8 — Log de auditoría (Módulo 8)
- [x] Registro automático de acciones clave (login, registro de usuarios, cambios de competidores, cancelaciones, decisiones de inscripción y resultados)
- [x] Endpoint paginado de auditoría restringido a ADMIN

## Fase 9 — Manejo de errores global
- [x] Exception handler centralizado con formato estructurado del spec
- [x] Mapeo correcto de códigos HTTP (400/401/403/404/409)
- [x] Validaciones con anotaciones (@NotNull, @NotBlank, @Positive, etc.)

## Fase 10 — Frontend / GUI (Módulo 7)
- [x] Login + manejo de token
- [x] Dashboard conectado al REST API
- [x] Pantallas de competidores (lista, detalle operativo, crear/editar)
- [x] Pantallas de equipos (lista, detalle operativo y gestión de miembros)
- [x] Pantallas de carreras (lista, detalle operativo, crear/editar)
- [x] Gestión de inscripciones
- [x] Registro de resultados
- [x] Standings/leaderboard
- [x] Perfil + logout
- [x] Pantallas de acceso denegado / 404
- [x] Estados de carga, vacío y error en las listas principales
- [x] Ocultar/deshabilitar acciones según rol

> Frontend funcional en `frontend/`: React/Vite, cliente JWT, CRUD de
> competidores/carreras, gestión de equipos, inscripciones, resultados,
> standings, perfil y estados de acceso.
>
> **Verificación ejecutada:** `npm install --no-package-lock`, `npm run build` y
> smoke test con `npm run preview` pasan. Se verificaron las rutas autenticadas
> desconocidas (404) y las operaciones restringidas para `VIEWER` (403).
> La UI valida campos requeridos y valores positivos antes de llamar a la API;
> los errores 401 intentan renovar la sesión con el refresh token y, si falla,
> limpian la sesión y devuelven al login.

## Fase 11 — Dockerización completa
- [x] Dockerfile backend
- [x] Dockerfile frontend (si aplica)
- [x] `compose.yml` final con todos los servicios, red, volumen nombrado
- [ ] Verificar que `docker compose up -d` levanta TODO desde cero

> `docker compose config -q`, la validación sintáctica de ambos Dockerfiles,
> el empaquetado backend y un arranque/smoke test del stack con la configuración
> actual pasan. Falta repetirlo desde volumen vacío para cerrar el criterio
> "desde cero"; ese paso queda documentado para el usuario.

## Fase 12 — Testing (mínimo 15 tests significativos)
- [x] Revisar cobertura contra la lista de casos sugeridos en el spec
- [x] Agregar tests de integración si faltan
>
> **Resultado:** suite backend completa con PostgreSQL: 80 tests, 0 fallos,
> 0 errores, 0 omitidos. Se añadieron pruebas de integración para autorización
> de creación de carreras (viewer 403, admin 201), y reglas de reapertura dentro
> del deadline.

## Fase 13 — Documentación y entrega
- [x] README completo (ver spec sección 11)
- [x] Diagrama entidad-relación editable documentado en `docs/diagrama-er.dbml`
- [x] Colección API versionable en `docs/API_REQUESTS.http`
- [x] Reporte técnico en `docs/REPORTE_TECNICO.md`
- [x] Evidencia del flujo API mediante capturas del proceso de Postman en `docs/evidence/`

> Pendiente para el usuario: completar integrantes/metadatos, ejecutar Docker desde
> volumen vacío y realizar commit/push.
