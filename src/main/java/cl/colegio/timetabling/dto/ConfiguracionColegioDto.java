package cl.colegio.timetabling.dto;

import java.util.List;

/**
 * Configuracion global del colegio: dimension de la grilla horaria (regla 1) y el
 * corte de "manana" (regla 5). Es un singleton (no una lista de entidades como
 * profesores/cursos/ramos/salas).
 */
public class ConfiguracionColegioDto {

    private int dias = 5;
    private int bloquesPorDia = 8;
    private List<BloqueHorarioDto> bloques;
    private String horaCorteManana;

    public int getDias() {
        return dias;
    }

    public void setDias(int dias) {
        this.dias = dias;
    }

    public int getBloquesPorDia() {
        return bloquesPorDia;
    }

    public void setBloquesPorDia(int bloquesPorDia) {
        this.bloquesPorDia = bloquesPorDia;
    }

    public List<BloqueHorarioDto> getBloques() {
        return bloques;
    }

    public void setBloques(List<BloqueHorarioDto> bloques) {
        this.bloques = bloques;
    }

    public String getHoraCorteManana() {
        return horaCorteManana;
    }

    public void setHoraCorteManana(String horaCorteManana) {
        this.horaCorteManana = horaCorteManana;
    }
}
