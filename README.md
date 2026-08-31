# Motor de asignación horaria de profesores (Spring Boot + OptaPlanner)

> **Nota sobre el motor:** originalmente este proyecto usaba Timefold Solver,
> pero Timefold exige Java 17+ (y Spring Boot 3, que también exige Java 17+).
> Como el entorno corporativo está fijado en Java 11, se migró a
> **OptaPlanner 8.44.0.Final** (el proyecto del cual Timefold es sucesor) +
> **Spring Boot 2.7.18**, ambos compatibles con Java 11. La API de Constraint
> Streams (`ConstraintProvider`, `Joiners`, `ConstraintCollectors`, etc.) es
> prácticamente idéntica — solo cambia el paquete raíz de `ai.timefold.solver`
> a `org.optaplanner`. Si en el futuro la empresa habilita Java 17+, migrar de
> vuelta a Timefold es mecánico (Timefold provee incluso un comando de
> migración automática `timefold-migration`).

## Endpoints CRUD (datos en memoria)

Los elementos del dominio ahora se pueden gestionar via CRUD estandar, en vez
de mandar todo el JSON a mano cada vez. Guardado en memoria (se pierde al
reiniciar la app — es el mismo criterio de "todo en memoria" del resto del
proyecto; si mas adelante se necesita persistencia real, solo hay que
reemplazar `InMemoryRepository` por JPA/JDBC, los controllers no cambian).

- `GET/POST /api/profesores`, `GET/PUT/DELETE /api/profesores/{id}`
- `GET/POST /api/cursos`, `GET/PUT/DELETE /api/cursos/{id}`
- `GET/POST /api/salas`, `GET/PUT/DELETE /api/salas/{id}` (el universo de
  salas entre las que el solver elige — no se referencian desde el ramo)
- `GET/POST /api/ramos`, `GET/PUT/DELETE /api/ramos/{id}` (valida que
  `cursoId`/`profesorId` existan; el ramo NO tiene `salaId`, la sala la
  decide el solver por sesión)
- `GET/PUT /api/config`: configuracion global (dias, bloquesPorDia, bloques,
  horaCorteManana) — es un singleton, no una lista.

Al arrancar la app (`DatosIniciales`), estos repositorios se precargan con el
mismo dataset de ejemplo que `DemoDataGenerator`, asi que `/api/timetable/actual/solve`
funciona de inmediato sin tener que cargar nada a mano primero.

## Endpoints de horario

- `GET /api/timetable/actual`: arma el `TimetableRequest` completo a partir de
  lo cargado via CRUD (profesores/cursos/salas/ramos/config), SIN resolver.
  Util para que el frontend lo use como base para `/solve`, `/verificar` o
  `/mover-sesion` sin tener que reconstruir el JSON a mano.
- `GET /api/timetable/actual/solve`: arma el request desde CRUD y lo resuelve
  de una — el equivalente "real" a `/demo/solve` pero con los datos que el
  usuario cargo via las paginas CRUD.
- `GET /api/timetable/demo/solve`: resuelve el dataset de ejemplo hardcodeado
  en `DemoDataGenerator` (independiente del CRUD, útil para pruebas rápidas).
- `POST /api/timetable/solve`: recibe cursos/profesores/ramos en JSON, resuelve
  DESDE CERO (ignora cualquier posición previa) y devuelve el horario,
  incluyendo la sala que el solver eligió para cada sesión. Campos nuevos
  relevantes en el request: `bloques` (regla 1), `horaCorteManana` (regla 5),
  `Profesor.maxHorasSemanales/horaIngreso/horaSalida` (reglas 2 y 3),
  `Curso.horaSalidaMaxima` (regla 4), `Ramo.preferirManana` (regla 5). Todos
  opcionales — si se omiten, el comportamiento es igual al de antes de estas
  reglas. **`salas` es obligatorio** (al menos una): es el universo de
  opciones para la variable de planificación `sala`.
- `POST /api/timetable/verificar`: recibe un horario **ya armado** (cada ramo
  con `sesionesActuales` completas — dia, bloque **y salaId** de cada sesión)
  y **solo calcula el score** — no mueve nada. Sirve para precargar un
  horario existente (ej. migrado desde otro sistema) y saber de inmediato si
  es factible, y si no, exactamente qué restricciones se violan (`detalle`:
  nombre de la restricción, score, cantidad de ocurrencias). Usa
  `ScoreManager` de OptaPlanner, no el solver — es prácticamente instantáneo.
- `POST /api/timetable/mover-sesion`: recibe el horario actual (con
  `sesionesActuales`) más `{ ramoId, indiceSesion, nuevoSlot: {dia, bloque,
  salaId} }`. `salaId` es **opcional** en `nuevoSlot`: si se omite, la sesión
  mantiene la sala que ya tenía y solo cambia de horario. Ancla esa sesión en
  su nueva posición completa (`@PlanningPin`) y vuelve a resolver — un
  re-solve corto que solo toca lo estrictamente necesario para arreglar los
  choques que ese cambio haya provocado (ver las restricciones blandas
  "Mantener horario original" y "Mantener sala original" en
  `TimetableConstraintProvider`). La respuesta marca `"movida": true` en cada
  sesión que terminó en una posición (horario o sala) distinta a la que
  tenía antes de este cambio.

## Cómo correr

```bash
mvn spring-boot:run
```

Luego:

```bash
curl http://localhost:8080/api/timetable/demo/solve
```

Esto resuelve el dataset de ejemplo (`DemoDataGenerator`) y devuelve el horario
resultante junto al score (`0hard/-Nsoft` significa: todas las restricciones
duras cumplidas, con N puntos de penalización blanda).

## Modelo de dominio

- `TimeSlot`: (día, bloque). El universo de slots se genera parametrizando
  días de la semana y bloques por día — hoy 5×8, cambiar en `DemoDataGenerator`
  (en producción vendría de configuración del colegio, no hardcodeado).
- `Teacher`: guarda el conjunto de `TimeSlot` en que **no** está disponible
  (por contrato). Vacío = disponible siempre.
- `Room`: sala física, identificada por el color de su puerta (`color`,
  hexadecimal `#RRGGBB`).
- `Curso`, `Ramo`: la terna **`<Curso, Profesor, Ramo>`** es un dato de entrada
  fijo, **no** algo que el solver decida. Un mismo profesor puede dictar varios
  ramos distintos a un mismo curso (ej. Historia, Orientación Vocacional y
  PAES); no hay restricción de unicidad sobre (curso, profesor).
- `SesionRamo` (`@PlanningEntity`): una hora suelta de un ramo. Un ramo de 6
  horas semanales genera 6 `SesionRamo` independientes. Tiene **DOS** variables
  de planificación que el solver decide de forma independiente por sesión:
  `timeslot` (cuándo) y `sala` (dónde). La sala **no** es fija por ramo — un
  mismo ramo puede terminar con sesiones en salas distintas según el día (ej.
  lunes en Sala 101, jueves en Sala 102); el solver la elige libremente entre
  todas las salas cargadas, igual que elige el horario.
- `Timetable` (`@PlanningSolution`): contenedor de todo — problem facts +
  entidades de planificación + score. La lista de salas (`roomList`) es tanto
  problem fact como `@ValueRangeProvider` — el universo de opciones entre las
  que el solver elige para la variable `sala`.

## Restricciones implementadas (`TimetableConstraintProvider`)

**Duras (hard, no negociables):**
1. Un profesor no puede tener dos sesiones al mismo tiempo.
2. Un curso no puede tener dos ramos al mismo tiempo.
3. Dos sesiones no pueden coincidir en la misma sala al mismo tiempo (la sala
   es una variable de planificación por sesión, no un dato fijo del ramo).
4. Una sesión no puede caer en un slot donde el profesor no está disponible.
5. Los ramos con horario obligatorio predefinido (ej. Orientación jueves
   bloque 1) deben respetarlo exactamente.
6. La sesión debe caer dentro de la ventana horaria de contrato del profesor
   (`horaIngreso`/`horaSalida`), si está definida.
7. La sesión no puede terminar después de la hora de salida máxima del curso
   (`horaSalidaMaxima`, ej. IV medios en ciertos periodos), si está definida.

**Blandas (soft, se optimizan pero no bloquean una solución):**
8. Repartir las sesiones de cada curso de forma pareja entre los días
   (evita "todo lenguaje el lunes").
9. Evitar que un profesor tenga demasiadas horas seguidas el mismo día
   (umbral configurable, `MAX_HORAS_SEGUIDAS_DESEABLE`).
10. Al editar un horario ya cargado, preferir no mover una sesión de su
    posición original salvo que sea necesario (usado por `/mover-sesion`).
11. Dar preferencia a que ciertos ramos (`Ramo.preferirManana = true`, ej.
    Lenguaje, Matemática) se dicten en horario de mañana, según el corte
    parametrizable `horaCorteManana` (default 13:00).

**Validado al cargar los datos (no es un constraint del solver):**
- Que ningún profesor supere su `maxHorasSemanales` de contrato, sumando las
  `horasSemanales` de todos los ramos que ya tiene asignados. No es algo que
  el solver pueda "arreglar" moviendo horarios — la asignación profesor↔ramo
  es un dato de entrada fijo — así que se rechaza con `400` al cargar el
  horario si se supera.

Agregar una restricción nueva es agregar un método más al arreglo que
devuelve `defineConstraints` — no toca el resto del motor.

## Hora de inicio/fin por bloque (regla 1)

`TimetableRequest.bloques` es una lista opcional `{numero, horaInicio, horaFin}`
(formato `"HH:mm"`), una entrada por cada bloque de 1 a `bloquesPorDia`, igual
para los 5 días. Si se omite, se genera un horario por defecto (bloques de
45 min consecutivos desde las 08:00, sin recreos) para no romper datasets
antiguos — para un colegio real conviene informarla siempre, con los recreos
reales entre bloques.

## Re-solve dinámico (profesor deja de estar disponible)

`TimetableService.reemplanificarPorAusenciaProfesor(...)` bloquea todos los
timeslots restantes para ese profesor y vuelve a correr el solver. Para el
tamaño típico de un colegio esto resuelve en segundos (ver
`termination.spent-limit` en `application.yml`), así que un cold-restart es
suficiente. Si en el futuro el volumen de datos crece mucho, Timefold soporta
`ProblemChange` sobre un `SolverJob` vivo para reoptimizar de forma incremental
sin recalcular todo desde cero — quedó documentado en el código como próximo
paso, no hacía falta complicar el motor con eso todavía.

## Por qué Timefold y no OR-Tools/CP-SAT puro

Ver la conversación previa: la combinación de (a) restricciones duras+blandas
mezcladas, (b) necesidad de re-resolver dinámicamente, y (c) integración
nativa con Spring Boot hicieron que Timefold fuera la opción con menos
fricción de ingeniería, sin sacrificar calidad de la solución.

## Qué falta para producción (no incluido a propósito, para no adelantarme)

- Persistencia real (hoy en memoria vía `InMemoryRepository`; para agregar
  una BD, ese es el único lugar a reemplazar por JPA/JDBC).
- Autenticación/autorización en los endpoints CRUD.
- Borrado en cascada o advertencia al eliminar un curso/profesor/sala que
  todavía tiene ramos referenciándolo (hoy solo se detecta al construir el
  horario, con un mensaje 400 claro).
