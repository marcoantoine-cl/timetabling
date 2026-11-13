# Arquitectura del Sistema - Timetabling Escolar (`architecture.md`)

## 1. Vista General
Sistema web modular compuesto por un Backend en Spring Boot 2.7 (Java 11) con motor de optimización OptaPlanner 8 y un Frontend en Angular.


### 2. Arquitectura por Capas (NON-NEGOTIABLE)
**MUST follow "package by feature by layer" architecture.**

## 2.2 Estructura del Backend (Spring Boot)
- **`controller/`**: Endpoints REST basados estrictamente en el contrato `openapi.yaml`. No debe haber lógica de negocio en la capa controller.
- **`controller/dto/{recurso}Dto`**: DTOs basados estrictamente en el contrato `openapi.yaml`. No debe haber lógica de negocio en la capa controller.
- **`controller/mapper/{recurso}Mapper`**: Endpoints REST basados estrictamente en el contrato `openapi.yaml`. No debe haber lógica de negocio en la capa controller.
- **`service/`**: Lógica de negocio y orquestación del Solver.
- **`domain/`**: Entidades de negocio y clases anotadas de planificación (*PlanningEntity*, *PlanningSolution*).
- **`repository/`**: Anti-corruption layer abstracting external resources. Interface in root, implementations in datastore (Redis, Cosmos, SQL, Mongo, Sybase, PostgreSQL)
- **`repository/datastore`**: Implemetacion para el ltipo de base de datos escogida.
- **`repository/datastore/model`**: Entidades JPA.
- **`repository/datastore/mapper`**: Mappers .
- **`solver/`**: Implementación de restricciones duras y blandas mediante *ConstraintStreams*.

## 3. Estructura del Frontend (Angular)
- **`components/`**: Vistas de administración (CRUDs de entidades) y grilla de horarios interactiva.
- **`services/`**: Clientes HTTP para consumo de la API REST.
- **`models/`**: Interfaces TypeScript alineadas con los esquemas del backend.

## 4. Flujo Operativo
1. El usuario administra maestros (cursos, profesores, salas, asignaturas) en Angular.
2. Se solicita la optimización mediante `POST /api/timetables/solve`.
3. El backend procesa las restricciones y retorna la solución óptima evaluada (*Hard/Soft Score*).
4. El backend almacena la solucion en base de datos.
4. El frontend renderiza la matriz horaria utilizando los códigos de color definidos por sala.

## 4. Flujo Operativo - Importacion desde excel
1. El usuario sube archivo Excel con horarios ya construidos en Angular.
2. Se solicita la optimización mediante `POST /api/timetables/import-xls`.
3. El backend procesa el archivo y realiza las modificaciones necesarias en la base de datos. Tanto para maestros como para la solución.
4. El frontend renderiza la matriz horaria utilizando los códigos de color definidos por sala.