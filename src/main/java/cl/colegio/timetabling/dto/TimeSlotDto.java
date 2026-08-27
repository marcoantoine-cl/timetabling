package cl.colegio.timetabling.dto;

public class TimeSlotDto {

    private int dia;    // 1 = Lunes ... 5 = Viernes
    private int bloque; // 1..bloquesPorDia

    public TimeSlotDto() {
    }

    public TimeSlotDto(int dia, int bloque) {
        this.dia = dia;
        this.bloque = bloque;
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
}
