package cl.colegio.timetabling.dto;

/**
 * Payload de POST /api/timetable/mover-sesion: el horario actual completo
 * (con sesionesActuales pobladas en cada ramo) mas el movimiento puntual pedido.
 * nuevoSlot.salaId es opcional: si se omite, la sesion mantiene la sala que ya tenia.
 */
public class MoverSesionRequest {

    private TimetableRequest horario;
    private String ramoId;
    private int indiceSesion;
    private AsignacionSesionDto nuevoSlot;

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

    public AsignacionSesionDto getNuevoSlot() {
        return nuevoSlot;
    }

    public void setNuevoSlot(AsignacionSesionDto nuevoSlot) {
        this.nuevoSlot = nuevoSlot;
    }
}
