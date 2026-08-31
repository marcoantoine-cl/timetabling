package cl.colegio.timetabling.dto;

/**
 * Representa el estado COMPLETO de una sesion: cuando (dia/bloque) y donde (sala).
 * A diferencia de TimeSlotDto (que solo describe un horario), este DTO se usa donde
 * hace falta describir una sesion ya ubicada por el solver: RamoDto.sesionesActuales
 * (precargar un horario existente) y MoverSesionRequest.nuevoSlot (mover una sesion).
 *
 * salaId es obligatorio en sesionesActuales (para poder calcular el score completo en
 * /verificar) pero opcional en MoverSesionRequest.nuevoSlot (si se omite, la sesion
 * mantiene la sala que ya tenia).
 */
public class AsignacionSesionDto {

    private int dia;
    private int bloque;
    private String salaId;

    public AsignacionSesionDto() {
    }

    public AsignacionSesionDto(int dia, int bloque, String salaId) {
        this.dia = dia;
        this.bloque = bloque;
        this.salaId = salaId;
    }

    public int getDia() {
        return dia;
    }

    public void setDia(int dia) {
        this.dia = dia;
    }

    public int getBloque() {
        return bloque;
    }

    public void setBloque(int bloque) {
        this.bloque = bloque;
    }

    public String getSalaId() {
        return salaId;
    }

    public void setSalaId(String salaId) {
        this.salaId = salaId;
    }
}
