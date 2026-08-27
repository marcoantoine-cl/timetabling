package cl.colegio.timetabling.domain;

/**
 * Sala/espacio fisico compartido y limitado (ej. Gimnasio).
 * Los ramos que se dictan en la sala base de cada curso NO necesitan una Room:
 * el conflicto de curso ya se resuelve con la restriccion "un curso, un ramo a la vez".
 * Room solo se usa para recursos compartidos y escasos entre cursos (gimnasio, laboratorio, etc).
 *
 * Las salas se identifican por el color de su puerta. Se guarda como codigo hexadecimal
 * ("#RRGGBB") en vez de un nombre libre ("rojo"/"Rojo"/"red") para evitar inconsistencias
 * y poder pintarlo directamente en la UI sin logica de mapeo nombre->color.
 */
public class Room {

    private String id;
    private String name;
    private String color; // "#RRGGBB", opcional

    public Room() {
    }

    public Room(String id, String name) {
        this(id, name, null);
    }

    public Room(String id, String name, String color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return name;
    }
}
