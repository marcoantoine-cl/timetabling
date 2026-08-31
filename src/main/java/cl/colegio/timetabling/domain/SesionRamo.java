package cl.colegio.timetabling.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

/**
 * Entidad de planificacion: UNA hora/bloque semanal de un Ramo.
 * Un ramo con weeklyHours=4 genera 4 instancias de SesionRamo (bloques sueltos,
 * no agrupados en franjas), cada una con indice 0..3 solo para trazabilidad.
 *
 * El solver decide DOS variables de planificacion independientes por sesion:
 * "timeslot" (cuando) y "sala" (donde). Un mismo ramo puede terminar con sesiones
 * en salas distintas segun el dia (ej. lunes en Sala 101, jueves en Sala 102) —
 * la sala NO es un dato fijo, es parte de lo que el solver optimiza, igual que el horario.
 *
 * Si "fixedTimeSlot" viene informado (ramo con horario obligatorio predefinido,
 * ej. Orientacion jueves bloque 1), esta sesion queda anclada en el TIEMPO (ver
 * ConstraintProvider, regla horarioFijoRespetado) pero la sala se sigue decidiendo libremente.
 */
@PlanningEntity
public class SesionRamo {

    @PlanningId
    private String id;

    private Ramo ramo;
    private int indiceSesion; // solo informativo, para distinguir sesiones del mismo ramo

    // Si no es null, esta sesion tiene horario obligatorio predefinido (hard constraint).
    private TimeSlot fixedTimeSlot;

    // Snapshots "de referencia" (el horario ya cargado / lo ultimo que el usuario dejo asi
    // a proposito). Se usan SOLO por las restricciones blandas de estabilidad: si estan
    // seteados, el solver prefiere no mover la sesion de ahi (ni de horario ni de sala)
    // salvo que sea necesario para resolver un choque. Son null en el flujo de generacion
    // desde cero (POST /solve), por eso no interfieren ahi.
    private TimeSlot timeslotOriginal;
    private Room salaOriginal;

    // Si esta en true, el solver NUNCA mueve esta sesion (ni horario ni sala) — se usa para
    // anclar el cambio manual que el usuario acaba de pedir en POST /mover-sesion.
    @PlanningPin
    private boolean pinned;

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeslot;

    @PlanningVariable(valueRangeProviderRefs = "salaRange")
    private Room sala;

    public SesionRamo() {
    }

    public SesionRamo(String id, Ramo ramo, int indiceSesion, TimeSlot fixedTimeSlot) {
        this.id = id;
        this.ramo = ramo;
        this.indiceSesion = indiceSesion;
        this.fixedTimeSlot = fixedTimeSlot;
        if (fixedTimeSlot != null) {
            this.timeslot = fixedTimeSlot; // valor inicial coherente con el horario fijo
        }
    }

    public String getId() {
        return id;
    }

    public Ramo getRamo() {
        return ramo;
    }

    public int getIndiceSesion() {
        return indiceSesion;
    }

    public TimeSlot getFixedTimeSlot() {
        return fixedTimeSlot;
    }

    public boolean isFixed() {
        return fixedTimeSlot != null;
    }

    public TimeSlot getTimeslot() {
        return timeslot;
    }

    public void setTimeslot(TimeSlot timeslot) {
        this.timeslot = timeslot;
    }

    public Room getSala() {
        return sala;
    }

    public void setSala(Room sala) {
        this.sala = sala;
    }

    public TimeSlot getTimeslotOriginal() {
        return timeslotOriginal;
    }

    public void setTimeslotOriginal(TimeSlot timeslotOriginal) {
        this.timeslotOriginal = timeslotOriginal;
    }

    public Room getSalaOriginal() {
        return salaOriginal;
    }

    public void setSalaOriginal(Room salaOriginal) {
        this.salaOriginal = salaOriginal;
    }

    /** true si el horario o la sala quedaron distintos a como estaban originalmente. */
    public boolean isMovida() {
        boolean cambioHorario = timeslotOriginal != null && !timeslotOriginal.equals(timeslot);
        boolean cambioSala = salaOriginal != null && !salaOriginal.equals(sala);
        return cambioHorario || cambioSala;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    @Override
    public String toString() {
        return ramo + "#" + indiceSesion;
    }
}
