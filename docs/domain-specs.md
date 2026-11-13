# Especificación del Dominio: Sistema de Timetabling Escolar (`domain-spec.md`)

## 1. Entidades Principales (Modelo de Datos)

### 1.1. `Curso`
Representa un grupo de estudiantes agrupados por nivel y letra.
*   `id`: Long (Identificador único)
*   `nombre`: String (Ej. "1ro Medio A", "5to Básico B")
*   `nivel`: Enum / String (Ej. "PRIMARIA", "SECUNDARIA" — utilizado para asociar reglas de almuerzo diferenciadas).
*   `tieneMovilidadReducida`: Boolean (Indica si el curso cuenta con alumnos que presentan movilidad reducida, requiriendo asignación en salas accesibles).
*   `tieneCierreTemprano`: Boolean (Indica si el curso finaliza sus clases de manera anticipada, se requiere entonces planificar antes de horario de corte)

### 1.2. `Asignatura`
Materia de estudio que un curso debe cursar de forma obligatoria durante la semana.
Una asignatura es impartida por solo un profesor por curso, durante el scope del período académico (semestral).
Historia de 2do medio A es otra instancia de Asignatura que Historia de 4to medio B.
*   `id`: Long
*   `nombre`: String (Ej. "Matemáticas", "Historia")
*   `profesorId`: Long (El ID del profesor que imparte esta asignatura)
*   `horasSemanales`: Integer (**Restricción Dura:** Número exacto de bloques requeridos por semana).
*   `salaRequeridaId`: Long (Opcional: ID de la sala temática/especializada prioritaria, ej. Laboratorio de Computación).
*   `esPrioritariaCierreTemprano`: Boolean (Si un curso tiene cierre temprano, esta asignatura debe priorizarse impartirse antes de corte horario)

### 1.3. `Profesor`
Persona contratada para impartir una o más asignaturas.
*   `id`: Long
*   `nombre`: String
*   `maxHorasSemanales`: Integer (**Restricción Dura:** Tope máximo contractual. Puede tener menos, pero nunca más).
*   `disponibilidades`: List<`DisponibilidadDiaria`> (Ventanas horarias de entrada y salida permitidas para cada día de la semana).

### 1.4. `DisponibilidadDiaria` (Objeto de Valor)
*   `diaSemana`: Enum (LUNES, MARTES, MIERCOLES, JUEVES, VIERNES)
*   `bloqueInicio`: Integer (Primer bloque habilitado para el profesor en el día, inclusive)
*   `bloqueFin`: Integer (Último bloque habilitado para el profesor en el día, inclusive)

### 1.5. `Sala`
Espacio físico donde se imparten las clases.
*   `id`: Long
*   `nombre`: String (Ej. "Sala 101", "Laboratorio 2", "Gimnasio")
*   `esEspecializada`: Boolean (Indica si está vinculada de forma preferente a una asignatura).
*   `esAccesible`: Boolean (Indica si la sala cuenta con facilidades de acceso para personas con movilidad reducida).
*   `color`: String (Indica el color de la sala; se utiliza como referencia visual principal en el frontend, Ej. "#4CAF50").

### 1.6. `BloqueHorario`
Unidad mínima temporal de la jornada escolar.
*   `id`: Long
*   `diaSemana`: Enum (LUNES a VIERNES)
*   `orden`: Integer (Número secuencial del bloque en el día, ej. 1 al 10).
*   `esProtegido`: Boolean (True si corresponde a recreo, almuerzo de menores o almuerzo de mayores. Los cursos de dintintos niveles no usan el mismo horario protegido).
*   `nivel`: Si `esProtegido = true` indica que solo es utilizado por cursos de este nivel.
*   `horaInicio`: Time (Ej. 8:15 am).
*   `horaFin`: Time (Ej. 10:00 am).

### 1.7. `Clase` (Entidad Planeada / Variable de Planificación)
Representa cada bloque individual de clase que el motor debe ubicar en el horario.
*   `id`: Long
*   `curso`: Curso (Variable de planificación en OptaPlanner)
*   `asignatura`: Asignatura, ha de tener coherencia con curso (Variable de planificación en OptaPlanner)
*   `sala`: Sala (Variable de planificación en OptaPlanner)
*   `bloqueHorario`: BloqueHorario (Variable de planificación en OptaPlanner)

### 1.8. `ResultadoHorario` (Entidad Resultado / Resultado de Planificación)
Representa el horario resultado de una planificación, persistido para continuar una sesión anterior
sin necesitar planificar nuevamente. Un snapshot es inmutable, si se carga un snapshot almacenado, se crea uno nuevo. 
Para fines de historial, mantener multiples resultados, con guardado bajo
acción directa del usuario.
*   `id`: Long (Identificador único)
*   `clases`: List<Clase> (Resultado de la planificación por OptaPlanner)
*   `fecha`: Date (Fecha de la planificación)
*   `estado`: Enum (BORRADOR, ACEPTADO, CONFIRMADO, ELIMINADO)
*   `comentarios`: String (Comentarios de la sesión de planificación)

### 1.9. `Configuracion` (Objeto de Valor / Configurable por usuario)
Representa los parametros de ajuste para la sesión de planificación. Tiene alcance global por establecimiento.
*   `horarioCorte`: Time (Horario de corte para los cursos que tienen cierre temprano)


---

## 2. Definición de Restricciones (Constraints)

### 2.1. Restricciones Duras (Hard Constraints)
Invariantes absolutos. Su incumplimiento invalida la planificación horaria.

1.  **Carga Curricular Exacta:** Cada curso debe tener exactamente la cantidad de clases semanales definidas por su malla para cada asignatura.
2.  **Tope Contractual Docente:** La suma de bloques asignados a un profesor no puede superar su `maxHorasSemanales`.
3.  **Ventana Horaria Docente:** Ninguna clase de un profesor puede asignarse fuera de su `DisponibilidadDiaria` (sus rangos de entrada y salida por día).
4.  **Exclusividad de Profesor:** Un profesor no puede dictar dos clases en el mismo `BloqueHorario`.
5.  **Exclusividad de Sala:** Una sala no puede albergar más de un curso/clase en el mismo `BloqueHorario`.
6.  **Exclusividad de Curso:** Un curso no puede estar en dos salas ni recibir dos asignaturas distintas en el mismo `BloqueHorario`.
7.  **Bloques Protegidos:** Queda estrictamente prohibido asignar clases en bloques donde `esProtegido = true` (recreos y almuerzos diferenciados).
8.  **Accesibilidad por Movilidad Reducida:** Si un curso tiene `tieneMovilidadReducida = true`, todas las clases de dicho curso deben ser asignadas exclusivamente a salas donde `esAccesible = true`. 


### 2.2. Restricciones Blandas (Soft Constraints)
Criterios de optimización para maximizar la calidad del horario.

1.  **Minimización de Subutilización Docente:** Penalizar de forma lineal la diferencia entre el máximo de horas contractuales y las horas efectivamente asignadas a un profesor.
2.  **Afinidad de Sala:** Bonificar fuertemente cuando una asignatura se imparte en su sala especializada correspondiente.
3.  **Compacidad de la Jornada Docente:** Penalizar la aparición de "huecos" u horas libres intermedias en la disponibilidad diaria de un profesor.
4.  **Distribución de Carga Diaria:** Penalizar la concentración excesiva de bloques de una misma asignatura en un solo día para un curso (evitar fatiga cognitiva).
5.  **Cierre temprano de Año:** Penalizar fuertemente la asignación de clases en bloques horarios posteriores a Configuracion.horarioCorte.


## Dataset ajustado — prueba a escala real

Pensado para verificar que el motor sigue siendo factible y resuelve en un
tiempo razonable a la escala real del colegio.

**Parámetros:**

| Parámetro      | Valor | Por qué |
|----------------|---|---|
| Días           | 5 | Lunes a viernes |
| Bloques/día    | 10 | Con horario real: recreo de 15 min entre bloque 4 y 5, almuerzo de 45 min entre bloque 7 y 8 |
| Cursos         | 24 | 12 niveles (1°-8° Básico, I-IV Medio) × 2 secciones (A/B) |
| Profesores     | 31 | Ver nota abajo — quedó en 31, no 30 exacto |
| Salas          | 28 | 24 aulas (una por curso) + 2 gimnasios + laboratorio + sala de computación |
| Asignaturas    | 264 | 11 asignaturas × 24 cursos |
| Score objetivo |`0hard/-Nsoft`|Todas las reglas duras deben cumplirse|
| Tiempo Máximo  | 10 minutos| Planificación no es un proceso de alta demanda por lo que puede realizarse con tiempos altos de procesamiento.|