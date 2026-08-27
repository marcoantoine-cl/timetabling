package cl.colegio.timetabling.domain;

/**
 * Sala/espacio fisico compartido y limitado (ej. Gimnasio).
 * Los ramos que se dictan en la sala base de cada curso NO necesitan una Room:
 * el conflicto de curso ya se resuelve con la restriccion "un curso, un ramo a la vez".
 * Room solo se usa para recursos compartidos y escasos entre cursos (gimnasio, laboratorio, etc).
 */
public class Room {

    private String id;
    private String name;

    public Room() {
    }

    public Room(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
