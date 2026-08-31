# CLAUDE.md

Este archivo le da contexto a Claude Code cada vez que trabaja en este repo.
Vive en la raíz del proyecto — Claude Code lo lee automáticamente al arrancar
en esta carpeta, no hace falta pegarlo ni mencionarlo.

## Qué es este proyecto

Motor de asignación horaria de profesores para un colegio. Recibe cursos,
profesores, ramos y salas, y genera (o valida/edita) el horario semanal
respetando restricciones duras (sin choques, disponibilidad de profesores,
etc.) y optimizando restricciones blandas (balance de carga, preferencias).

Repo hermano: `timetabling-frontend` (Angular) — consume la API REST de este
backend en `http://localhost:8080`.

## Stack y por qué

- **Java 11 + Spring Boot 2.7.18** — fijado por normativa de la empresa
  (Java 11 obligatorio). Spring Boot 3.x exige Java 17, por eso quedamos en
  la última serie 2.x.
- **OptaPlanner 8.44.0.Final** (no Timefold) — Timefold es el sucesor de
  OptaPlanner pero exige Java 17+. OptaPlanner 8.x es la última versión
  compatible con Java 11. Si algún día la empresa habilita Java 17+, migrar
  a Timefold es mecánico (API casi idéntica, cambia el paquete raíz de
  `org.optaplanner` a `ai.timefold.solver`).
- **Persistencia en memoria** (`InMemoryRepository`, sin base de datos) — así
  se definió el proyecto desde el inicio. Si se agrega una BD real, ese es el
  único lugar a tocar; los controllers no cambian.

## Cómo correr

```
mvn spring-boot:run
```

Backend queda en `localhost:8080`. Al arrancar, `DatosIniciales` precarga
datos de ejemplo en los repositorios CRUD (no hace falta cargar nada a mano
para probar).

## Modelo de dominio — decisiones importantes, NO cuestionar sin preguntar primero

- La terna `<Curso, Profesor, Ramo>` es un **dato de entrada fijo**. El
  solver NUNCA decide qué profesor dicta qué ramo — eso ya viene dado.
- La **sala SÍ es una variable de planificación** (`SesionRamo.sala`), igual
  que el horario (`SesionRamo.timeslot`). Un mismo ramo puede terminar en
  salas distintas según el día. Esto fue un cambio deliberado — antes la sala
  era fija por ramo, se corrigió a pedido explícito.
- Un profesor puede dictar varios ramos distintos a un mismo curso (ej.
  Historia + Orientación + PAES) — no hay restricción de unicidad sobre
  (curso, profesor).
- `SesionRamo` es la entidad de planificación: una hora suelta de un ramo
  (bloques sueltos, no agrupados en franjas — decisión explícita por
  legibilidad).

## Restricciones (`TimetableConstraintProvider`)

Antes de agregar una restricción nueva, revisar si ya existe algo similar.
Lista actual (duras primero, luego blandas) — mantener esta lista
actualizada en este archivo cada vez que se agregue/quite una:

- Duras: choque de profesor, choque de curso, choque de sala, disponibilidad
  del profesor, horario fijo respetado, ventana de contrato del profesor,
  hora de salida máxima del curso.
- Blandas: balance de carga diaria por curso, evitar horas seguidas
  excesivas, mantener horario/sala original al editar (estabilidad),
  preferencia de ramos en la mañana.

## Endpoints principales

- `POST /solve` — resuelve desde cero.
- `POST /verificar` — solo calcula el score de un horario ya armado
  (`ScoreManager`, no invoca al solver). Requiere `sesionesActuales`
  completas (día, bloque Y sala) en cada ramo.
- `POST /mover-sesion` — mueve una sesión puntual, la ancla (`@PlanningPin`)
  y re-resuelve tocando lo mínimo posible.
- CRUD estándar en `/api/profesores`, `/api/cursos`, `/api/salas`,
  `/api/ramos`, `/api/config`.

## Convenciones

- Todo el código (nombres de clases, variables, comentarios, mensajes de
  error) en **español** — así se definió desde el inicio del proyecto.
- Mensajes de error de validación deben ser específicos y accionables (decir
  QUÉ dato falta y en qué entidad), no genéricos.
- Al tocar `TimetableRequestMapper`, recordar: los campos de hora opcionales
  deben tratarse como "no informados" tanto si vienen `null` como si vienen
  `""` (string vacío) — usar el helper `esVacio()`, no `!= null` solo.

## Qué NO hacer

- No asumir que se puede subir a Java 17 — está fijado por la empresa.
- No agregar una base de datos sin que se pida explícitamente — es
  intencional que todo sea en memoria por ahora.
- No mover la sala de vuelta a ser un campo fijo de `Ramo` — fue un cambio
  deliberado a variable de planificación, ya se revirtió una vez un intento
  de complicarlo con una feature de "mover sala por curso" que no era lo que
  se pedía.
