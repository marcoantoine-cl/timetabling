package cl.colegio.timetabling.dto;

/**
 * Hora de comienzo y fin de un numero de bloque (regla 1). Es global para todos los
 * dias de la semana (mismo horario lunes a viernes). Formato de hora: "HH:mm".
 */
public class BloqueHorarioDto {

    private int numero;      // 1..bloquesPorDia
    private String horaInicio;
    private String horaFin;

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(String horaFin) {
        this.horaFin = horaFin;
    }
}
