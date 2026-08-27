package cl.colegio.timetabling.dto;

import java.util.List;

/**
 * Payload de entrada del endpoint POST /api/timetable/solve.
 * Representa el dataset completo a resolver: parametros de la grilla horaria
 * mas profesores, salas, cursos y ramos.
 */
public class TimetableRequest {

    private int dias = 5;              // dias habiles a la semana, parametrizable
    private int bloquesPorDia = 8;      // bloques de 45 min por dia, parametrizable

    // Regla 1: hora de inicio/fin de cada bloque. Opcional: si se omite, se genera un
    // horario por defecto (bloques de 45 min consecutivos desde las 08:00, sin recreos).
    private List<BloqueHorarioDto> bloques;

    // Regla 5: hora de corte para considerar un bloque "de manana". Opcional, formato
    // "HH:mm". Por defecto "13:00" si no se informa.
    private String horaCorteManana;

    private List<ProfesorDto> profesores;
    private List<SalaDto> salas;
    private List<CursoDto> cursos;
    private List<RamoDto> ramos;

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

    public List<ProfesorDto> getProfesores() {
        return profesores;
    }

    public void setProfesores(List<ProfesorDto> profesores) {
        this.profesores = profesores;
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

    public List<SalaDto> getSalas() {
        return salas;
    }

    public void setSalas(List<SalaDto> salas) {
        this.salas = salas;
    }

    public List<CursoDto> getCursos() {
        return cursos;
    }

    public void setCursos(List<CursoDto> cursos) {
        this.cursos = cursos;
    }

    public List<RamoDto> getRamos() {
        return ramos;
    }

    public void setRamos(List<RamoDto> ramos) {
        this.ramos = ramos;
    }
}
