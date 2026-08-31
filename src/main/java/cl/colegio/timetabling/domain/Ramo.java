package cl.colegio.timetabling.domain;

/**
 * Ramo (asignatura) dictado a un Curso especifico por un Teacher especifico.
 * La terna (curso, profesor, ramo) es fija durante todo el semestre: no es algo
 * que el solver decida, es un dato de entrada.
 * Lo que el solver SI decide es EN QUE TimeSlot y EN QUE Sala se dicta cada una
 * de las "weeklyHours" sesiones de este ramo (ver SesionRamo) — la sala NO es
 * fija por ramo: un mismo ramo puede dictarse en salas distintas segun el dia
 * (ej. lunes en Sala 101, jueves en Sala 102), el solver decide libremente.
 *
 * Un mismo profesor puede dictar varios ramos distintos a un mismo curso (ej. Historia,
 * Orientacion Vocacional y PAES); no hay restriccion de unicidad sobre (curso, profesor).
 */
public class Ramo {

    private String id;
    private String name;         // ej. "Lenguaje"
    private Curso curso;
    private Teacher teacher;
    private int weeklyHours;     // cantidad de bloques de 45 min a la semana

    // Regla 5: preferencia por manana para ciertos ramos (ej. Lenguaje, Matematica).
    private boolean preferirManana;

    public Ramo() {
    }

    public Ramo(String id, String name, Curso curso, Teacher teacher, int weeklyHours) {
        this(id, name, curso, teacher, weeklyHours, false);
    }

    public Ramo(String id, String name, Curso curso, Teacher teacher, int weeklyHours,
                boolean preferirManana) {
        this.id = id;
        this.name = name;
        this.curso = curso;
        this.teacher = teacher;
        this.weeklyHours = weeklyHours;
        this.preferirManana = preferirManana;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Curso getCurso() {
        return curso;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public int getWeeklyHours() {
        return weeklyHours;
    }

    public boolean isPreferirManana() {
        return preferirManana;
    }

    @Override
    public String toString() {
        return name + "(" + curso + ")";
    }
}
