package cl.colegio.timetabling.domain;

/**
 * Ramo (asignatura) dictado a un Curso especifico por un Teacher especifico.
 * La terna (curso, ramo, profesor) es fija durante todo el semestre: no es algo
 * que el solver decida, es un dato de entrada.
 * Lo que el solver SI decide es en que TimeSlot(s) se dicta cada una de las
 * "weeklyHours" sesiones de este ramo (ver SesionRamo).
 */
public class Ramo {

    private String id;
    private String name;         // ej. "Lenguaje"
    private Curso curso;
    private Teacher teacher;
    private int weeklyHours;     // cantidad de bloques de 45 min a la semana
    private Room requiredRoom;   // null si se dicta en la sala base del curso; no-null si compite por un recurso compartido (ej. gimnasio)

    // Regla 5: preferencia por manana para ciertos ramos (ej. Lenguaje, Matematica).
    private boolean preferirManana;

    public Ramo() {
    }

    public Ramo(String id, String name, Curso curso, Teacher teacher, int weeklyHours, Room requiredRoom) {
        this(id, name, curso, teacher, weeklyHours, requiredRoom, false);
    }

    public Ramo(String id, String name, Curso curso, Teacher teacher, int weeklyHours, Room requiredRoom,
                boolean preferirManana) {
        this.id = id;
        this.name = name;
        this.curso = curso;
        this.teacher = teacher;
        this.weeklyHours = weeklyHours;
        this.requiredRoom = requiredRoom;
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

    public Room getRequiredRoom() {
        return requiredRoom;
    }

    public boolean isPreferirManana() {
        return preferirManana;
    }

    @Override
    public String toString() {
        return name + "(" + curso + ")";
    }
}
