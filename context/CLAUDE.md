# CLAUDE.md — EIA Camel vs. Dwarf Racing System

## Qué es este proyecto
Sistema de información (REST API + GUI + BD + Docker) para una liga de carreras
ficticia. El detalle funcional completo, reglas de negocio, endpoints sugeridos y
criterios de evaluación están en `docs/PROJECT_SPEC.md` — léelo siempre antes de
implementar algo y trátalo como fuente de verdad.

## Stack tecnológico (decidido por el equipo)
- Backend: Java 21 + Spring Boot (Web, Data JPA, Validation, Security)
- Base de datos: PostgreSQL en contenedor Docker
- Frontend: React
- Seguridad: JWT + Spring Security (hashing con BCrypt)
- Testing: JUnit 5 + Mockito (mínimo 15 tests significativos, no solo getters/setters)
- Contenedores: Docker + Docker Compose (`docker compose up -d` debe levantar todo)

## Arquitectura obligatoria (backend)
Paquetes en capas — NO te saltes ninguno:
```
controller/   → recibe HTTP, valida formato, invoca services. SIN lógica de negocio.
service/      → reglas de negocio, lógica de carreras/inscripciones/estadísticas.
repository/   → acceso a datos.
model|entity/ → entidades JPA.
dto/          → contratos de API. Las entidades NUNCA se exponen directamente.
security/     → auth, JWT, filtros, config de roles.
exception/    → manejo centralizado de errores, respuestas consistentes.
config/       → configuración de Spring (CORS, beans, etc).
```

## Reglas no negociables
- La GUI consume el REST API — **nunca** toca la base de datos directamente.
- Los errores devuelven el formato estructurado del spec (timestamp, status, error,
  message, path) — nunca stack traces crudos.
- Contraseñas y secretos jamás se exponen en respuestas de API ni se commitean
  (usar `.env` + `.gitignore`, incluir `.env.example` sin credenciales reales).
- Toda regla de negocio del spec debe existir en código, no solo mencionarse en docs.
- 3 roles: `ADMIN`, `RACE_ORGANIZER`, `VIEWER` — permisos exactos en el spec, Módulo 1.

## Cómo quiero que trabajes en este repo
1. Antes de escribir código para un módulo nuevo, dime el plan (entidades, endpoints,
   reglas de negocio a validar) y espera mi aprobación.
2. Trabaja módulo por módulo (ver `docs/TASKS.md`), no todo de una vez.
3. Después de cada módulo: que compile, que corran los tests existentes, y confírmame
   antes de seguir al siguiente.
4. Sigue la convención de branches del spec (`feature/security`, `feature/competitors`, etc.)
   si estás usando git de forma autónoma.
5. Cuando termines un módulo, actualiza el checklist en `docs/TASKS.md`.

## Comandos útiles
```bash
# Backend
./mvnw spring-boot:run
./mvnw test

# Todo el stack
docker compose up -d
docker compose logs -f
docker compose down
```
