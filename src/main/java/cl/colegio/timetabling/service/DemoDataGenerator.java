package cl.colegio.timetabling.service;

import cl.colegio.timetabling.domain.*;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;

/**
 * Genera un dataset pequeño de ejemplo para validar el motor end-to-end,
 * incluyendo las reglas de ventana horaria, hora de salida por curso,
 * preferencia de manana y la tupla <Curso,Profesor,Ramo,Sala>.
 * En un caso real, este dataset vendria de la base de datos / de un formulario de carga.
 */
@Component
public class DemoDataGenerator {

    private static final int DIAS = 5;       // Lunes..Viernes, parametrizable
    private static final int BLOQUES_DIA = 8; // parametrizable
    private static final LocalTime PRIMER_BLOQUE = LocalTime.of(8, 0);
    private static final int MINUTOS_POR_BLOQUE = 45;
    private static final LocalTime CORTE_MANANA = LocalTime.of(13, 0);

    public Timetable generarProblema() {
        List<TimeSlot> timeSlots = generarTimeSlots();

        // Las salas se identifican por el color de su puerta (codigo hexadecimal).
        Room gimnasio = new Room("R1", "Gimnasio", "#FF7043");
        Room sala101 = new Room("R2", "Sala 101", "#4CAF50"); // sala base de II A
        Room sala102 = new Room("R3", "Sala 102", "#2196F3"); // sala base de II B
        List<Room> rooms = List.of(gimnasio, sala101, sala102);

        // Regla 3: ventana horaria de contrato. Regla 2: maximo de horas semanales.
        Teacher profJuan = new Teacher("P1", "Juan Perez", new HashSet<>(),
                LocalTime.of(8, 0), LocalTime.of(16, 0), 30);
        // No disponible el viernes completo (contrato part-time esos dias)
        Teacher profAna = new Teacher("P2", "Ana Soto", noDisponibleTodoElDia(5),
                LocalTime.of(8, 0), LocalTime.of(14, 0), 24);
        Teacher profLuis = new Teacher("P3", "Luis Rojas", new HashSet<>(List.of(new TimeSlot(1, 1))),
                LocalTime.of(8, 0), LocalTime.of(16, 0), 20);
        List<Teacher> teachers = List.of(profJuan, profAna, profLuis);

        Curso iiA = new Curso("C1", "II A");
        // Regla 4: este curso tiene salida maxima 14:00 (coincide con el limite del bloque 8 del dia).
        Curso iiB = new Curso("C2", "II B", LocalTime.of(14, 0));
        List<Curso> cursos = List.of(iiA, iiB);

        // Un mismo profesor puede dictar varios ramos a un mismo curso: aqui profJuan
        // dicta tanto Lenguaje como Orientacion a II A. La tupla es <Curso,Profesor,Ramo,Sala>,
        // no hay restriccion de unicidad sobre (curso, profesor).
        // Regla 5: Lenguaje y Matematica con preferencia de manana (penultimo parametro = true)
        Ramo lenguajeIIA = new Ramo("R-LEN-C1", "Lenguaje", iiA, profJuan, 6, sala101, true);
        Ramo matematicaIIA = new Ramo("R-MAT-C1", "Matematica", iiA, profAna, 6, sala101, true);
        Ramo orientacionIIA = new Ramo("R-ORI-C1", "Orientacion", iiA, profJuan, 1, sala101); // horario fijo
        Ramo edFisicaIIA = new Ramo("R-EDF-C1", "Educacion Fisica", iiA, profLuis, 2, gimnasio);

        Ramo lenguajeIIB = new Ramo("R-LEN-C2", "Lenguaje", iiB, profAna, 6, sala102, true);
        Ramo matematicaIIB = new Ramo("R-MAT-C2", "Matematica", iiB, profJuan, 6, sala102, true);
        Ramo orientacionIIB = new Ramo("R-ORI-C2", "Orientacion", iiB, profAna, 1, sala102); // horario fijo
        Ramo edFisicaIIB = new Ramo("R-EDF-C2", "Educacion Fisica", iiB, profLuis, 2, gimnasio);

        List<Ramo> ramos = List.of(lenguajeIIA, matematicaIIA, orientacionIIA, edFisicaIIA,
                lenguajeIIB, matematicaIIB, orientacionIIB, edFisicaIIB);

        // Horario fijo predefinido: Orientacion siempre jueves (dia=4), bloque 1
        TimeSlot horarioFijoOrientacion = new TimeSlot(4, 1);

        List<SesionRamo> sesiones = new ArrayList<>();
        for (Ramo ramo : ramos) {
            boolean esFijo = ramo.getName().equals("Orientacion");
            for (int i = 0; i < ramo.getWeeklyHours(); i++) {
                TimeSlot fijo = esFijo ? horarioFijoOrientacion : null;
                sesiones.add(new SesionRamo(ramo.getId() + "-S" + i, ramo, i, fijo));
            }
        }

        return new Timetable(timeSlots, rooms, teachers, cursos, ramos, sesiones);
    }

    private List<TimeSlot> generarTimeSlots() {
        List<TimeSlot> slots = new ArrayList<>();
        for (int dia = 1; dia <= DIAS; dia++) {
            LocalTime cursor = PRIMER_BLOQUE;
            for (int bloque = 1; bloque <= BLOQUES_DIA; bloque++) {
                LocalTime inicio = cursor;
                LocalTime fin = cursor.plusMinutes(MINUTOS_POR_BLOQUE);
                cursor = fin;
                TimeSlot slot = new TimeSlot(dia, bloque, inicio, fin);
                slot.setManana(inicio.isBefore(CORTE_MANANA));
                slots.add(slot);
            }
        }
        return slots;
    }

    private Set<TimeSlot> noDisponibleTodoElDia(int dia) {
        Set<TimeSlot> set = new HashSet<>();
        for (int bloque = 1; bloque <= BLOQUES_DIA; bloque++) {
            set.add(new TimeSlot(dia, bloque));
        }
        return set;
    }
}
