package cl.colegio.timetabling.domain;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Representa un bloque horario concreto: un dia de la semana + un numero de bloque,
 * con su hora de inicio/fin de reloj (regla 1: cada bloque tiene hora de comienzo y fin).
 * Es un "problem fact" (no cambia durante la resolucion), se genera todo el universo
 * de TimeSlots posibles a partir de los parametros del colegio (dias x bloques por dia
 * x tabla de horarios de bloque).
 *
 * NOTA: horaInicio/horaFin NO participan en equals/hashCode a proposito. Un TimeSlot
 * se identifica por (dia, bloque); esto permite comparar timeslots "livianos" (ej. un
 * horario fijo predefinido que solo trae dia/bloque) contra los timeslots reales del
 * universo (que si traen horaInicio/horaFin) sin que la igualdad se rompa.
 */
public class TimeSlot {

    private int dayOfWeek;   // 1 = Lunes ... 5 = Viernes (parametrizable)
    private int block;       // 1..N bloques por dia (parametrizable)

    private LocalTime horaInicio; // puede ser null si no se informo la tabla de horarios de bloque
    private LocalTime horaFin;

    // Regla 5: si este timeslot cae en horario de "manana" segun el corte parametrizable
    // del colegio (ver TimetableRequest.horaCorteManana). Se calcula al construir el
    // universo de TimeSlots (mapper/demo generator), no aqui.
    private boolean manana;

    public TimeSlot() {
    }

    public TimeSlot(int dayOfWeek, int block) {
        this.dayOfWeek = dayOfWeek;
        this.block = block;
    }

    public TimeSlot(int dayOfWeek, int block, LocalTime horaInicio, LocalTime horaFin) {
        this.dayOfWeek = dayOfWeek;
        this.block = block;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public int getBlock() {
        return block;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public boolean isManana() {
        return manana;
    }

    public void setManana(boolean manana) {
        this.manana = manana;
    }

    /** Util para restricciones de "horas seguidas": mismo dia y bloques consecutivos. */
    public boolean isConsecutiveTo(TimeSlot other) {
        return this.dayOfWeek == other.dayOfWeek && Math.abs(this.block - other.block) == 1;
    }

    public boolean isSameDay(TimeSlot other) {
        return this.dayOfWeek == other.dayOfWeek;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot)) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return dayOfWeek == timeSlot.dayOfWeek && block == timeSlot.block;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dayOfWeek, block);
    }

    @Override
    public String toString() {
        return "Dia" + dayOfWeek + "-Bloque" + block;
    }
}
