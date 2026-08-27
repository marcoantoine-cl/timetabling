package cl.colegio.timetabling.dto;

import java.util.List;

public class RamoDto {

    private String id;
    private String nombre;
    private String cursoId;
    private String profesorId;
    private int horasSemanales;
    private String salaId; // opcional: solo si compite por un recurso compartido (ej. gimnasio)

    // Opcional: horarios obligatorios predefinidos, en orden, para las primeras N sesiones
    // de este ramo (ej. Orientacion -> [{dia:4, bloque:1}]). Las sesiones restantes,
    // si horasSemanales > horariosFijos.size(), quedan libres para que el solver las ubique.
    private List<TimeSlotDto> horariosFijos;

    // Opcional: posicion ACTUAL de cada sesion, para precargar un horario ya existente
    // (usado por /verificar y /mover-sesion). Si no se entrega, el solver decide todo
    // desde cero (flujo normal de /solve). Debe tener el mismo largo que horasSemanales
    // para /verificar; para /mover-sesion representa el estado antes del cambio.
    private List<TimeSlotDto> sesionesActuales;

    // Regla 5: dar preferencia a que este ramo se dicte en la manana (ej. Lenguaje, Matematica).
    private boolean preferirManana;

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

    public String getCursoId() {
        return cursoId;
    }

    public void setCursoId(String cursoId) {
        this.cursoId = cursoId;
    }

    public String getProfesorId() {
        return profesorId;
    }

    public void setProfesorId(String profesorId) {
        this.profesorId = profesorId;
    }

    public int getHorasSemanales() {
        return horasSemanales;
    }

    public void setHorasSemanales(int horasSemanales) {
        this.horasSemanales = horasSemanales;
    }

    public String getSalaId() {
        return salaId;
    }

    public void setSalaId(String salaId) {
        this.salaId = salaId;
    }

    public List<TimeSlotDto> getHorariosFijos() {
        return horariosFijos;
    }

    public void setHorariosFijos(List<TimeSlotDto> horariosFijos) {
        this.horariosFijos = horariosFijos;
    }

    public List<TimeSlotDto> getSesionesActuales() {
        return sesionesActuales;
    }

    public void setSesionesActuales(List<TimeSlotDto> sesionesActuales) {
        this.sesionesActuales = sesionesActuales;
    }

    public boolean isPreferirManana() {
        return preferirManana;
    }

    public void setPreferirManana(boolean preferirManana) {
        this.preferirManana = preferirManana;
    }
}
