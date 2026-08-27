package cl.colegio.timetabling.dto;

public class SalaDto {

    private String id;
    private String nombre;
    // Identifica la sala por el color de su puerta. Formato hexadecimal "#RRGGBB".
    private String color;

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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
