package cl.colegio.timetabling.dto;

/**
 * Payload de POST /api/timetable/mover-sesion: el horario actual completo
 * (con sesionesActuales pobladas en cada ramo) mas el movimiento puntual pedido.
 */
public class MoverSesionRequest {

    private TimetableRequest horario;
    private String ramoId;
    private int indiceSesion;
    private TimeSlotDto nuevoSlot;

    public TimetableRequest getHorario() {
        return horario;
    }

    public void setHorario(TimetableRequest horario) {
        this.horario = horario;
    }

    public String getRamoId() {
        return ramoId;
    }

    public void setRamoId(String ramoId) {
        this.ramoId = ramoId;
    }

    public int getIndiceSesion() {
        return indiceSesion;
    }

    public void setIndiceSesion(int indiceSesion) {
        this.indiceSesion = indiceSesion;
    }

    public TimeSlotDto getNuevoSlot() {
        return nuevoSlot;
    }

    public void setNuevoSlot(TimeSlotDto nuevoSlot) {
        this.nuevoSlot = nuevoSlot;
    }
}
