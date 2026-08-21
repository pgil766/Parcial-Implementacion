# The Great EIA Camel vs. Dwarf Racing System

Backend Development Project — Universidad EIA
Instructor: Sebastián Zapata Ramírez

> Contexto narrativo (no técnico): el proyecto simula una liga ficticia de carreras
> "camello vs. enano". Todo el detalle técnico real está abajo.

## 1. Objetivo General

Desarrollar un sistema de información completo para la liga ficticia EIA Camel vs. Dwarf
Racing League. La solución debe incluir:
- Backend en Java expuesto como REST API.
- Interfaz gráfica de usuario que consuma esa API (nunca la BD directamente).
- Persistencia en base de datos real (no en memoria).
- Autenticación y autorización basada en roles.
- Validación de reglas de negocio.
- Manejo de errores consistente.
- Todo corriendo en contenedores Docker.

Temas que debe demostrar el proyecto: OOP, diseño REST, arquitectura en capas,
modelado de BD, auth y roles, GUI, integración cliente-servidor, validación de
entradas y manejo de excepciones, testing automatizado, Docker, Git/GitHub.

## 2. Requisitos Tecnológicos Obligatorios

### 2.1 Backend
- Java 21 o superior.
- Spring Boot recomendado (Spring Web, Spring Data JPA, Spring Validation, Spring Security).
- Otro framework Java requiere aprobación previa del instructor.

### 2.2 Base de Datos
- PostgreSQL, MySQL, MariaDB, MongoDB u otra aprobada.
- Debe correr en contenedor Docker.
- H2 solo permitido para tests automatizados o desarrollo local temporal — **no** como BD final.

### 2.3 Interfaz Gráfica
- Web, desktop o mobile — debe consumir el REST API, nunca acceder a la BD directamente.
- Opciones web recomendadas: React, Angular, Vue, Thymeleaf.
- Opciones desktop recomendadas: JavaFX, Swing.
- Debe mostrar estados de carga, éxito, vacío y error.
- Debe adaptar acciones visibles según el rol autenticado.
- **Una colección de Postman NO cuenta como interfaz gráfica.**

### 2.4 Containerización
- Dockerfile para el backend Java.
- Dockerfile para el cliente gráfico (si aplica).
- Un `compose.yml`.
- Contenedores: backend, base de datos, frontend (si es app separada), y cualquier
  contenedor adicional requerido por la solución de seguridad elegida.
- **La solución completa debe levantar con un solo comando:** `docker compose up -d`

## 3. Arquitectura Requerida

Backend en capas, con paquetes equivalentes a:
```
controller
service
repository
model (o entity)
dto
security
exception
config
```

**Responsabilidades backend:**
- `controller`: recibe requests HTTP, valida formato de entrada, invoca servicios,
  retorna respuestas HTTP. **No debe contener lógica de negocio.**
- `service`: implementa reglas de negocio, lógica de carreras, inscripciones,
  estadísticas y coordinación entre repositorios.
- `repository`: acceso y persistencia en BD.
- `dto`: separa contratos de API de las entidades de persistencia. Las entidades
  **no deben exponerse directamente**.
- `exception`: centraliza el manejo de errores y produce respuestas consistentes.

**Responsabilidades frontend:**
- Vistas/páginas, componentes reutilizables (forms, tablas, cards, dialogs, notificaciones).
- Capa de cliente API / servicio que se comunica con el backend.
- Manejo de estado (sesión, rol de usuario, datos de pantalla).
- Protección de rutas (usuarios no autorizados no pueden abrir páginas restringidas).
- Validación en frontend — mejora UX pero **no reemplaza** la validación del backend.

## 4. Módulos Funcionales Obligatorios (mínimo 7)

### Módulo 1 — Autenticación y Seguridad
Seguridad con Auth0, Keycloak, Amazon Cognito, JWT + Spring Security, u otro proveedor aprobado.

**Roles requeridos:**
| Rol | Permisos mínimos |
|---|---|
| Administrator | Gestiona usuarios, competidores, equipos, carreras, inscripciones, resultados y auditoría. |
| Race Organizer | Gestiona carreras, inscripciones y resultados; ve competidores y equipos. |
| Viewer | Solo lectura: info pública, calendarios, resultados y clasificaciones. |

**Requisitos de seguridad:**
- Login/logout de usuario.
- Almacenamiento seguro de contraseñas si la autenticación es manual (BCrypt u otro hash seguro).
- Autenticación basada en tokens con expiración.
- Autorización basada en roles en el backend.
- Protección de rutas y acciones en la interfaz gráfica.
- `401 Unauthorized` para autenticación faltante o inválida.
- `403 Forbidden` para usuarios autenticados sin permiso.
- Contraseñas, tokens y secretos **nunca** deben ser retornados por la API ni subidos a GitHub.
- Valores sensibles vía variables de entorno, Docker secrets o `.env` local excluido por `.gitignore`.

**Endpoints sugeridos:**
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
GET  /api/auth/profile
```

### Módulo 2 — Gestión de Competidores
Un competidor puede ser dwarf, camel, medium-sized u otra categoría ficticia aprobada por un admin.

**Datos requeridos:** id, nombre, nickname único, tipo de competidor, fecha de nacimiento
o edad aproximada, peso, altura, país/lugar de origen, estado actual, fecha de registro,
equipo opcional, victorias/derrotas/carreras completadas.

**Enumeraciones mínimas:**
```
CompetitorType: DWARF, CAMEL, MEDIUM, OTHER
CompetitorStatus: ACTIVE, INJURED, SUSPENDED, RETIRED
```

**Reglas de negocio:**
- Nombre no puede estar vacío.
- Peso y altura deben ser positivos.
- Nickname debe ser único.
- Solo competidores `ACTIVE` pueden inscribirse en nuevas carreras.
- Un competidor con resultados oficiales no puede eliminarse físicamente; debe retirarse o desactivarse.
- Un camello no debe registrarse como dwarf, sin importar cuánto lo crea.

**Endpoints sugeridos:**
```
POST   /api/competitors
GET    /api/competitors          (filtrado, paginación y orden obligatorios)
GET    /api/competitors/{id}
PUT    /api/competitors/{id}
PATCH  /api/competitors/{id}/status
DELETE /api/competitors/{id}
```

### Módulo 3 — Gestión de Equipos

**Datos requeridos:** id, nombre único, descripción, fecha de creación, estado, coach/responsable,
lista de competidores, victorias y derrotas.

**Reglas de negocio:**
- Un equipo debe tener al menos un competidor antes de entrar a una carrera.
- Un competidor no puede pertenecer a más de un equipo activo a la vez.
- Un equipo suspendido no puede entrar a una carrera.
- Un competidor no puede añadirse dos veces al mismo equipo.
- Un equipo con historial oficial de carreras no puede eliminarse; debe desactivarse.
- El número máximo de miembros debe ser configurable.

**Endpoints sugeridos:**
```
POST   /api/teams
GET    /api/teams
GET    /api/teams/{id}
PUT    /api/teams/{id}
DELETE /api/teams/{id}
POST   /api/teams/{teamId}/members/{competitorId}
DELETE /api/teams/{teamId}/members/{competitorId}
```

### Módulo 4 — Gestión de Carreras

**Datos requeridos:** id, nombre, descripción, fecha/hora programada, ubicación de
inicio/fin, distancia en metros, máximo de participantes, tipo, estado, organizador,
fecha límite de inscripción, timestamps de creación/modificación.

```
RaceType: INDIVIDUAL, TEAM, MIXED
RaceStatus: DRAFT, OPEN_FOR_REGISTRATION, CLOSED_FOR_REGISTRATION, IN_PROGRESS, COMPLETED, CANCELLED
```

**Reglas de negocio:**
- Distancia > 0.
- No se puede crear una carrera en el pasado.
- La fecha límite de inscripción debe ser anterior al inicio de la carrera.
- Una carrera `COMPLETED` no puede editarse.
- Una carrera `CANCELLED` no puede recibir inscripciones.
- Se requieren al menos 2 participantes válidos para iniciar.
- No se puede exceder la capacidad.
- Una carrera no puede completarse sin resultados oficiales.
- Una carrera completada no puede volver a `DRAFT`.

**Endpoints sugeridos:**
```
POST   /api/races
GET    /api/races
GET    /api/races/{id}
PUT    /api/races/{id}
PATCH  /api/races/{id}/status
DELETE /api/races/{id}
```

### Módulo 5 — Inscripción a Carreras

**Datos requeridos:** id, carrera, competidor o equipo, fecha y estado de inscripción,
carril/posición de salida asignada, notas de validación, usuario que registró la inscripción.

```
RegistrationStatus: PENDING, APPROVED, REJECTED, CANCELLED
```

**Reglas de negocio:**
- Solo se permite inscripción mientras la carrera está abierta y antes de la fecha límite.
- Un competidor/equipo no puede inscribirse dos veces en la misma carrera.
- Todos los competidores individuales y miembros de equipo deben ser elegibles.
- Un participante no puede competir simultáneamente como individuo y como miembro de equipo en la misma carrera.
- El tipo de inscripción debe coincidir con el tipo de carrera.
- Las posiciones de salida no pueden duplicarse.
- Las inscripciones rechazadas deben incluir una razón clara.

**Endpoints sugeridos:**
```
POST   /api/races/{raceId}/registrations
GET    /api/races/{raceId}/registrations
GET    /api/registrations/{id}
PATCH  /api/registrations/{id}/approve
PATCH  /api/registrations/{id}/reject
DELETE /api/registrations/{id}
```

### Módulo 6 — Resultados y Clasificaciones

**Datos requeridos:** carrera, participante, posición inicial y final, tiempo de
finalización, tiempo de penalización, estado del resultado, notas, usuario que registró
el resultado y timestamp.

```
ResultStatus: FINISHED, DISQUALIFIED, DID_NOT_FINISH, DID_NOT_START
```

**Reglas de negocio:**
- Los resultados solo pueden registrarse para una carrera `IN_PROGRESS`.
- Solo participantes aprobados pueden recibir resultados.
- Las posiciones finales no pueden duplicarse entre finalistas normales.
- El tiempo de finalización debe ser positivo.
- Un descalificado no puede ganar.
- Solo se permite un ganador oficial.
- Actualizar un resultado debe actualizar las estadísticas de forma consistente.
- El sistema debe evitar dos ganadores, tres segundos lugares, etc.

**Sistema de puntos:**
| Posición | Puntos |
|---|---|
| 1° | 10 |
| 2° | 7 |
| 3° | 5 |
| 4° | 3 |
| 5° | 1 |
| No finalizó | 0 |
| Descalificado | 0 |

**Endpoints sugeridos:**
```
POST /api/races/{raceId}/results
GET  /api/races/{raceId}/results
PUT  /api/results/{id}
GET  /api/results/{id}
GET  /api/standings
GET  /api/standings/competitors
GET  /api/standings/teams
```

### Módulo 7 — Interfaz Gráfica de Usuario

Es un módulo funcional obligatorio, no un accesorio decorativo.

**Pantallas mínimas requeridas:**
- Login.
- Dashboard principal (resumen de próximas carreras, competidores activos, resultados recientes).
- Lista de competidores (búsqueda, filtros, paginación, vista de detalle).
- Formulario de creación/edición de competidores (roles autorizados).
- Lista de equipos, detalle de equipo, gestión de miembros.
- Lista y detalle de carreras.
- Formulario de creación/edición de carreras.
- Gestión de inscripciones.
- Registro de resultados.
- Clasificaciones/leaderboard.
- Perfil de usuario y logout.
- Pantallas de acceso denegado y no encontrado (404).

**Comportamiento de la interfaz:**
- Botones/menús ocultos o deshabilitados si el rol no tiene permiso.
- El cliente debe almacenar y enviar el token de forma segura.
- Formularios con mensajes de validación a nivel de campo.
- Errores de API traducidos a mensajes entendibles para el usuario.
- Diálogo de confirmación para acciones destructivas.
- Listas con estados vacíos útiles, no pantallas en blanco.
- Indicador de carga en operaciones largas.
- Notificaciones de éxito/error visibles.
- No exponer stack traces, contraseñas ni IDs internos de BD innecesariamente.
- El usuario debe poder completar el flujo principal de demo sin usar Postman.

### Módulo 8 — Log de Auditoría

Debe registrar acciones importantes: login, creación de usuarios, cambios de competidores,
cancelación de carreras, decisiones de inscripción y modificaciones de resultados.

**Datos:** id, usuario, acción, tipo y id de entidad, fecha/hora, descripción opcional,
valores previos y nuevos (opcional).

Solo administradores pueden ver el log de auditoría completo.

## 5. Requisitos de Diseño REST API

- `GET` para leer, `POST` para crear, `PUT` para actualización completa, `PATCH` para
  actualizaciones parciales o transiciones de estado, `DELETE` para eliminar/desactivar.
- `201 Created` al crear un recurso.
- `204 No Content` cuando un delete es exitoso sin cuerpo de respuesta.
- `400` para requests inválidos, `401` autenticación faltante, `403` permisos
  insuficientes, `404` recurso no encontrado, `409` conflictos de negocio.
- Convención de nombres JSON consistente.
- Todos los endpoints deben ser testeables independientemente de la interfaz gráfica.

**Respuesta de error estructurada:**
```json
{
  "timestamp": "2026-08-15T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Competitor with ID 45 was not found",
  "path": "/api/competitors/45"
}
```

## 6. Requisitos de Validación

- Validación en backend es **obligatoria**. La validación en frontend es también
  obligatoria por usabilidad, pero nunca reemplaza al backend.
- Usar anotaciones como `@NotNull`, `@NotBlank`, `@Size`, `@Positive`, `@Email`, `@Past`, `@Future`.
- Mensajes de validación entendibles a nivel de campo.
- Nunca retornar stack traces crudos al usuario.
- Validar identificadores, transiciones de estado, permisos y reglas de negocio entre entidades.

## 7. Requisitos de Base de Datos

Entidades equivalentes a: `User`, `Role`, `Competitor`, `Team`, `TeamMember`, `Race`,
`RaceRegistration`, `RaceResult`, `AuditLog`.

- Llaves primarias y foráneas (o referencias equivalentes).
- Restricciones únicas.
- Campos requeridos.
- Índices apropiados.
- Integridad referencial.
- Volumen Docker persistente y nombrado.
- Diagrama entidad-relación.

**Datos iniciales mínimos (seed):**
- 1 administrador, 1 organizador, 1 viewer.
- 5 dwarfs, 2 camels, 2 medium-sized.
- 2 equipos.
- 3 carreras en distintos estados.
- Al menos 1 carrera completada con resultados.

## 8. Requisitos de Testing

Mínimo **15 tests automatizados significativos** (tests que solo verifican getters/setters no cuentan).

Casos sugeridos:
- Crear un competidor válido.
- Rechazar competidor con peso inválido.
- Rechazar nickname duplicado.
- Crear una carrera válida.
- Rechazar carrera programada en el pasado.
- Inscribir un competidor activo exitosamente.
- Rechazar un competidor suspendido.
- Rechazar una inscripción duplicada.
- Rechazar inscripción después de la fecha límite.
- Registrar un resultado válido.
- Rechazar dos ganadores en una carrera.
- Prevenir que un viewer cree una carrera.
- Permitir que un admin cree una carrera.
- Retornar 401 sin token válido.
- Retornar 404 para un recurso inexistente.

Tests de frontend son opcionales pero recomendados (mínimo: demostrar manejo de
respuestas exitosas, errores de validación y fallos de autorización).

## 9. Requisitos Docker

- Contenedor de aplicación.
- Contenedor de base de datos.
- Contenedor de frontend (si aplica).
- Volumen de BD nombrado.
- Red Docker aislada.
- Variables de entorno.
- Configuración de puertos.
- Arranque automático.
- Health checks recomendados.

**Variables de entorno esperadas:**
```
DB_HOST
DB_PORT
DB_NAME
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION
API_BASE_URL
```

El repo puede incluir un `.env.example`, pero **nunca** con credenciales reales.

## 10. Requisitos de Git y GitHub

- Proyecto completo en repositorio GitHub.
- Commits significativos de cada miembro del equipo.
- Uso de branches y pull requests.
- `.gitignore`.
- `README.md`.
- Diagrama de base de datos.
- Instrucciones de Docker.
- Instrucciones de testing.
- Historial de contribución claro.

**Branches sugeridos:**
```
main
develop
feature/security
feature/competitors
feature/races
feature/frontend
feature/results
```

⚠️ Un repo con un solo commit final llamado "final" será evaluado con el mismo
entusiasmo que un camello en una convención de JavaScript.

## 11. Requisitos del README

Debe incluir: nombre y descripción del proyecto, miembros del equipo, explicación de
arquitectura, tecnologías usadas, modelo de BD, estrategia de seguridad, roles y
permisos, instrucciones de instalación (backend y frontend), instrucciones de ejecución
Docker, variables de entorno, URLs y puertos de cada componente, instrucciones de
testing, usuarios de ejemplo, ejemplos de requests API, limitaciones conocidas, mejoras futuras.

## 12. Bonus Opcionales (no reemplazan requisitos obligatorios)

- Pipeline de GitHub Actions.
- Ejecución automática de tests y build de imagen Docker.
- Despliegue en la nube.
- Refresh tokens.
- Notificaciones por email.
- Actualizaciones en vivo con WebSockets.
- Cache con Redis.
- Rate limiting.
- Migraciones con Flyway o Liquibase.
- Testcontainers.
- Exportación de resultados CSV/PDF.
- Imágenes de perfil de competidores.
- Métricas y observabilidad.
- Soft delete.
- Optimistic locking.
- Idempotency keys.

## 13. Entregables

- Repositorio GitHub con código fuente completo.
- Backend, BD y GUI dockerizados.
- Diagrama entidad-relación.
- Colección Postman/Insomnia para verificar el backend.
- Reporte técnico (arquitectura, modelo de datos, seguridad, interfaz, reglas de negocio, testing, Docker).
- Video de demostración de 8 a 12 minutos.

**Requisitos del video:**
- Participación de todo el equipo.
- Mostrar login y restricciones de rol.
- Crear un competidor y un equipo.
- Crear una carrera e inscribir participantes.
- Registrar resultados y mostrar clasificaciones.
- Demostrar al menos 2 errores manejados correctamente.
- Mostrar la GUI ejecutando el flujo principal.
- Mostrar los contenedores corriendo.
- Mostrar los tests automatizados ejecutándose.
- El video no puede consistir únicamente en leer diapositivas.

## 14. Escenario de Demostración Sugerido

1. Un administrador inicia sesión en la GUI.
2. El admin crea un camello llamado "Byte".
3. Se crean 5 dwarfs: Null Pointer, Stack Overflow, Little Lambda, Captain Cache, Tiny Docker.
4. Los dwarfs se añaden al equipo "The Five Exceptions".
5. Un organizador crea una carrera mixta de 1 km.
6. Byte y The Five Exceptions se inscriben.
7. Un viewer intenta modificar la carrera y recibe un mensaje de acceso denegado.
8. El organizador inicia la carrera.
9. Se registran resultados a través de la interfaz.
10. Se actualizan las clasificaciones.
11. El log de auditoría registra las acciones importantes.

## 15. Criterios de Evaluación

| Criterio | Peso |
|---|---|
| Requisitos funcionales y reglas de negocio | 22% |
| Interfaz gráfica y experiencia de usuario | 15% |
| Diseño de API y organización del código | 13% |
| Autenticación y autorización | 15% |
| Diseño de BD y persistencia | 10% |
| Docker y entorno de ejecución | 10% |
| Testing automatizado | 8% |
| Documentación, flujo GitHub y participación del equipo | 4% |
| Demostración final y explicación | 3% |
| **Total** | **100%** |

**Condiciones críticas de evaluación (evitar a toda costa):**
- El proyecto no corre.
- Falta la interfaz gráfica o no puede completar el flujo principal.
- La interfaz accede a la BD directamente en vez de usar la API.
- La base de datos no es persistente.
- La aplicación no está dockerizada.
- Falta autenticación o aplicación de roles.
- Las reglas de negocio existen solo en diapositivas, no en código.
- Los errores retornan stack traces.
- El repo contiene contraseñas o secretos.
- No hay tests automatizados significativos.
- Solo un miembro del equipo entiende la aplicación.
- La aplicación funciona exclusivamente en la computadora de "el compañero que no vino hoy".
