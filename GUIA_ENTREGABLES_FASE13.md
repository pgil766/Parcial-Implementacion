# Guía de entregables — Fase 13

Esta guía consolida los entregables documentales y las verificaciones finales del
proyecto.

## Estado actual

Ya están implementados y verificados en el repositorio:

- Backend Spring Boot y frontend React.
- PostgreSQL, backend y frontend dockerizados.
- Host PostgreSQL publicado en `6969`.
- Suite backend: 80 tests, 0 fallos, 0 errores, 0 omitidos.
- Build frontend exitoso.
- Smoke test del stack Docker exitoso.
- README base en la raíz.
- Reporte técnico en `docs/REPORTE_TECNICO.md`.
- Colección de requests compatible con JetBrains HTTP Client en `docs/API_REQUESTS.http`.

## 1. Completar el README

Archivo: `README.md`.

El usuario debe:

1. Reemplazar los integrantes `TODO` por nombres y correos reales.
2. Revisar que los puertos y credenciales seed coincidan con la demostración.
3. Ajustar cualquier limitación o mejora futura acordada por el equipo.
4. Confirmar que las instrucciones funcionan desde un clon limpio.

El README ya contiene arquitectura, tecnologías, modelo de datos, roles, puertos,
variables, Docker, testing, usuarios seed, endpoints, limitaciones y mejoras futuras.

## 2. Diagrama entidad-relación

El modelo editable está disponible en:

```text
docs/diagrama-er.dbml
```

La evidencia visual final está disponible en:

```text
docs/evidence/Diagrama_ERD.png
```

El PNG representa las nueve entidades del sistema:
`users`, `roles`, `competitors`, `teams`, `team_members`, `races`,
`race_registrations`, `race_results` y `audit_logs`, junto con sus relaciones,
claves foráneas y restricciones principales.

## 3. Entregar la colección de API

La alternativa implementada en el repositorio es:

```text
docs/API_REQUESTS.http
```

Es compatible con el cliente HTTP integrado de JetBrains:

- IntelliJ IDEA.
- PhpStorm.
- WebStorm.
- PyCharm Professional.

### Uso con IntelliJ IDEA

1. Abrir `docs/API_REQUESTS.http`.
2. Levantar el stack con `docker compose up -d`.
3. Ejecutar primero el request **Login admin**.
4. Copiar `accessToken` y `refreshToken` de la respuesta.
5. Reemplazar `@accessToken` y `@refreshToken` en el archivo.
6. Ejecutar los requests en orden: recursos, inscripciones, resultados y standings.
7. Repetir el login como `viewer` y ejecutar el request de error esperado.

Las capturas disponibles en `docs/evidence/` corresponden al proceso de verificación
realizado mediante Postman.

Para esta entrega se presentan `docs/API_REQUESTS.http` y las capturas porque quedan
versionados junto al código y cubren autenticación, CRUD, inscripciones, resultados,
standings, auditoría y errores.

## 4. Revisar el reporte técnico

Archivo: `docs/REPORTE_TECNICO.md`.

El usuario debe:

1. Completar los nombres de integrantes si aparecen como `TODO`.
2. Confirmar que los resultados de testing y Docker reflejan la última ejecución.
3. Añadir decisiones específicas del equipo que no estén en el repositorio.
4. Revisar ortografía y formato antes de entregar.

El reporte debe cubrir arquitectura, modelo de datos, seguridad, interfaz, reglas de
negocio, testing, Docker, limitaciones y pasos finales de verificación.

## 5. Verificación final de Docker desde cero

Este paso puede borrar la base persistida. Ejecutarlo solo si no se necesitan los
datos actuales:

```bash
docker compose down -v
docker compose up -d --build
docker compose ps
docker compose logs --tail=200 db backend frontend
```

Resultado esperado:

- PostgreSQL `healthy`.
- Backend y frontend `running`.
- Frontend en `http://localhost:5173`.
- Backend en `http://localhost:8080`.
- PostgreSQL publicado en `localhost:6969`.

Después, comprobar login admin/viewer desde la GUI y ejecutar algunos requests de
`docs/API_REQUESTS.http`.

## 6. Revisión de Git antes de entregar

El usuario debe ejecutar:

```bash
git status
git diff --check
git diff
```

Confirmar que no se incluyan:

- `.env`.
- Contraseñas reales.
- JWT o refresh tokens.
- Dumps de base de datos.
- `node_modules/` o artefactos de build innecesarios.

Luego crear el commit y hacer push según la estrategia del equipo. El usuario realiza
estas acciones; no se ejecutan automáticamente.

## Checklist de entrega

- [ ] Integrantes reales agregados al README y reporte.
- [ ] README revisado desde un clon limpio.
- [x] Diagrama ER disponible en `docs/evidence/Diagrama_ERD.png`.
- [x] Código DBML editable conservado en `docs/diagrama-er.dbml`.
- [x] `docs/API_REQUESTS.http` y el flujo de Postman documentados.
- [x] Evidencia de login, roles, errores y endpoints principales en `docs/evidence/`.
- [x] Reporte técnico revisado y enlazado desde el README.
- [ ] Stack probado desde volumen vacío.
- [ ] Git revisado sin secretos.
- [ ] Commit y push realizados por el usuario.
