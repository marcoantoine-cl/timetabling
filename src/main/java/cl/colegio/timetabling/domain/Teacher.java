package cl.colegio.timetabling.domain;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Profesor. La disponibilidad se modela de DOS formas complementarias:
 * 1) unavailableTimeSlots: bloques puntuales/dias completos en que NO puede (contrato irregular).
 * 2) horaIngreso/horaSalida: ventana horaria diaria de su contrato (regla 3). Si son null,
 *    no hay restriccion de ventana horaria (compatibilidad hacia atras).
 *
 * maxHorasSemanales (regla 2) NO es una restriccion del solver: la carga de un profesor es
 * la suma de horasSemanales de los ramos que ya tiene asignados (dato fijo de entrada, el
 * solver no elige que profesor dicta que ramo). Se valida al cargar el horario, no aqui.
 */
public class Teacher {

    private String id;
    private String name;
    private Set<TimeSlot> unavailableTimeSlots = new HashSet<>();

    private LocalTime horaIngreso; // null = sin restriccion de ventana horaria
    private LocalTime horaSalida;

    private Integer maxHorasSemanales; // null = sin tope

    public Teacher() {
    }

    public Teacher(String id, String name, Set<TimeSlot> unavailableTimeSlots) {
        this.id = id;
        this.name = name;
        if (unavailableTimeSlots != null) {
            this.unavailableTimeSlots = unavailableTimeSlots;
        }
    }

    public Teacher(String id, String name, Set<TimeSlot> unavailableTimeSlots,
                    LocalTime horaIngreso, LocalTime horaSalida, Integer maxHorasSemanales) {
        this(id, name, unavailableTimeSlots);
        this.horaIngreso = horaIngreso;
        this.horaSalida = horaSalida;
        this.maxHorasSemanales = maxHorasSemanales;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<TimeSlot> getUnavailableTimeSlots() {
        return unavailableTimeSlots;
    }

    public boolean isAvailableAt(TimeSlot timeSlot) {
        return !unavailableTimeSlots.contains(timeSlot);
    }

    public LocalTime getHoraIngreso() {
        return horaIngreso;
    }

    public LocalTime getHoraSalida() {
        return horaSalida;
    }

    public Integer getMaxHorasSemanales() {
        return maxHorasSemanales;
    }

    /** true si el timeslot cae fuera de la ventana horaria de contrato (cuando hay una definida). */
    public boolean estaFueraDeVentanaContrato(TimeSlot timeSlot) {
        if (horaIngreso == null || horaSalida == null) {
            return false; // sin ventana definida, no restringe
        }
        if (timeSlot.getHoraInicio() == null || timeSlot.getHoraFin() == null) {
            return false; // el timeslot no trae horas de reloj, no se puede evaluar
        }
        return timeSlot.getHoraInicio().isBefore(horaIngreso) || timeSlot.getHoraFin().isAfter(horaSalida);
    }

    @Override
    public String toString() {
        return name;
    }
}
