package cl.colegio.timetabling.domain;

import java.time.LocalTime;

public class Curso {

    private String id;
    private String name; // ej. "IIA"

    // Regla 4: cursos superiores (ej. IV medio) pueden tener una hora de salida distinta
    // en ciertos periodos del año. null = sin tope de salida para este curso.
    // El sistema no es consciente de fechas: para modelar el cambio "en los ultimos meses
    // del año", se vuelve a resolver con este parametro distinto para ese periodo.
    private LocalTime horaSalidaMaxima;

    public Curso() {
    }

    public Curso(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Curso(String id, String name, LocalTime horaSalidaMaxima) {
        this.id = id;
        this.name = name;
        this.horaSalidaMaxima = horaSalidaMaxima;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalTime getHoraSalidaMaxima() {
        return horaSalidaMaxima;
    }

    @Override
    public String toString() {
        return name;
    }
}
