package cl.colegio.timetabling.dto;

public class CursoDto {

    private String id;
    private String nombre;

    // Regla 4: hora de salida maxima para este curso (ej. IV medios en ciertos periodos
    // del año). Formato "HH:mm". Opcional (null = sin tope de salida).
    private String horaSalidaMaxima;

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

    public String getHoraSalidaMaxima() {
        return horaSalidaMaxima;
    }

    public void setHoraSalidaMaxima(String horaSalidaMaxima) {
        this.horaSalidaMaxima = horaSalidaMaxima;
    }
}
