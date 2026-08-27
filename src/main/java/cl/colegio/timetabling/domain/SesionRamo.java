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
 * El solver decide el valor de "timeslot" para cada SesionRamo.
 * Si "fixedTimeSlot" viene informado (ramo con horario obligatorio predefinido,
 * ej. Orientacion jueves bloque 1), esta sesion queda anclada (ver ConstraintProvider,
 * regla horarioFijoRespetado) y en la practica el solver no la movera de ahi.
 */
@PlanningEntity
public class SesionRamo {

    @PlanningId
    private String id;

    private Ramo ramo;
    private int indiceSesion; // solo informativo, para distinguir sesiones del mismo ramo

    // Si no es null, esta sesion tiene horario obligatorio predefinido (hard constraint).
    private TimeSlot fixedTimeSlot;

    // Snapshot del timeslot "de referencia" (el horario ya cargado / el ultimo que el usuario
    // dejo asi a proposito). Se usa SOLO por la restriccion blanda de estabilidad: si esta seteado,
    // el solver prefiere no mover la sesion de ahi salvo que sea necesario para resolver un choque.
    // Es null en el flujo de generacion desde cero (POST /solve), por eso no interfiere ahi.
    private TimeSlot timeslotOriginal;

    // Si esta en true, el solver NUNCA mueve esta sesion (se usa para anclar el cambio manual
    // que el usuario acaba de pedir en POST /mover-sesion).
    @PlanningPin
    private boolean pinned;

    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeslot;

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

    public TimeSlot getTimeslotOriginal() {
        return timeslotOriginal;
    }

    public void setTimeslotOriginal(TimeSlot timeslotOriginal) {
        this.timeslotOriginal = timeslotOriginal;
    }

    public boolean isMovida() {
        return timeslotOriginal != null && !timeslotOriginal.equals(timeslot);
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
