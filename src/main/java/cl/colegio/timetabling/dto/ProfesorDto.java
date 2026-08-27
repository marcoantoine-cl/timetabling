package cl.colegio.timetabling.dto;

import java.util.List;

public class ProfesorDto {

    private String id;
    private String nombre;
    // Lista de TimeSlots en que el profesor NO esta disponible (por contrato).
    // Vacio o null = disponible siempre.
    private List<TimeSlotDto> noDisponible;

    // Regla 3: ventana horaria diaria de contrato. Formato "HH:mm". Opcional
    // (null = sin restriccion de ventana).
    private String horaIngreso;
    private String horaSalida;

    // Regla 2: maximo de horas semanales frente a curso. Opcional (null = sin tope).
    // Se valida contra la suma de horasSemanales de los ramos ya asignados a este
    // profesor; NO es un constraint del solver (ver TimetableRequestMapper.validar).
    private Integer maxHorasSemanales;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<TimeSlotDto> getNoDisponible() {
        return noDisponible;
    }

    public void setNoDisponible(List<TimeSlotDto> noDisponible) {
        this.noDisponible = noDisponible;
    }

    public String getHoraIngreso() {
        return horaIngreso;
    }

    public void setHoraIngreso(String horaIngreso) {
        this.horaIngreso = horaIngreso;
    }

    public String getHoraSalida() {
        return horaSalida;
    }

    public void setHoraSalida(String horaSalida) {
        this.horaSalida = horaSalida;
    }

    public Integer getMaxHorasSemanales() {
        return maxHorasSemanales;
    }

    public void setMaxHorasSemanales(Integer maxHorasSemanales) {
        this.maxHorasSemanales = maxHorasSemanales;
    }
}
