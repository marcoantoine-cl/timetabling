-- DDL - Sistema de Timetabling Escolar (Actualizado)

CREATE TABLE sala (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    es_especializada BOOLEAN NOT NULL DEFAULT FALSE,
    es_accesible BOOLEAN NOT NULL DEFAULT FALSE,
    color VARCHAR(7) NOT NULL
);

CREATE TABLE asignatura (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    horas_semanales INT NOT NULL,
    sala_requerida_id BIGINT,
    CONSTRAINT fk_asignatura_sala FOREIGN KEY (sala_requerida_id) REFERENCES sala(id) ON DELETE SET NULL
);

CREATE TABLE curso (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    nivel VARCHAR(50) NOT NULL,
    tiene_movilidad_reducida BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE profesor (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    max_horas_semanales INT NOT NULL
);

CREATE TABLE disponibilidad_diaria (
    id BIGSERIAL PRIMARY KEY,
    profesor_id BIGINT NOT NULL,
    dia_semana VARCHAR(20) NOT NULL,
    bloque_inicio INT NOT NULL,
    bloque_fin INT NOT NULL,
    CONSTRAINT fk_disponibilidad_profesor FOREIGN KEY (profesor_id) REFERENCES profesor(id) ON DELETE CASCADE
);

CREATE TABLE bloque_horario (
    id BIGSERIAL PRIMARY KEY,
    dia_semana VARCHAR(20) NOT NULL,
    orden INT NOT NULL,
    es_protegido BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE clase (
    id BIGSERIAL PRIMARY KEY,
    curso_id BIGINT NOT NULL,
    asignatura_id BIGINT NOT NULL,
    profesor_id BIGINT NOT NULL,
    sala_id BIGINT NOT NULL,
    bloque_horario_id BIGINT NOT NULL,
    CONSTRAINT fk_clase_curso FOREIGN KEY (curso_id) REFERENCES curso(id) ON DELETE CASCADE,
    CONSTRAINT fk_clase_asignatura FOREIGN KEY (asignatura_id) REFERENCES asignatura(id) ON DELETE CASCADE,
    CONSTRAINT fk_clase_profesor FOREIGN KEY (profesor_id) REFERENCES profesor(id) ON DELETE CASCADE,
    CONSTRAINT fk_clase_sala FOREIGN KEY (sala_id) REFERENCES sala(id) ON DELETE CASCADE,
    CONSTRAINT fk_clase_bloque FOREIGN KEY (bloque_horario_id) REFERENCES bloque_horario(id) ON DELETE CASCADE
);