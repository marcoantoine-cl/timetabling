package cl.colegio.timetabling.domain;

/**
 * Ramo (asignatura) dictado a un Curso especifico por un Teacher especifico, en una
 * Sala especifica. La tupla (curso, profesor, ramo, sala) es fija durante todo el
 * semestre: no es algo que el solver decida, es un dato de entrada.
 * Lo que el solver SI decide es en que TimeSlot(s) se dicta cada una de las
 * "weeklyHours" sesiones de este ramo (ver SesionRamo).
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
    private Room sala;           // sala asignada a este ramo (obligatoria, se identifica por el color de su puerta)

    // Regla 5: preferencia por manana para ciertos ramos (ej. Lenguaje, Matematica).
    private boolean preferirManana;

    public Ramo() {
    }

    public Ramo(String id, String name, Curso curso, Teacher teacher, int weeklyHours, Room sala) {
        this(id, name, curso, teacher, weeklyHours, sala, false);
    }

    public Ramo(String id, String name, Curso curso, Teacher teacher, int weeklyHours, Room sala,
                boolean preferirManana) {
        this.id = id;
        this.name = name;
        this.curso = curso;
        this.teacher = teacher;
        this.weeklyHours = weeklyHours;
        this.sala = sala;
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

    public Room getSala() {
        return sala;
    }

    public boolean isPreferirManana() {
        return preferirManana;
    }

    @Override
    public String toString() {
        return name + "(" + curso + ")";
    }
}
