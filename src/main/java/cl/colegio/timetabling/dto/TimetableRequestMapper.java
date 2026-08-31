package cl.colegio.timetabling.dto;

import cl.colegio.timetabling.domain.*;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class TimetableRequestMapper {

    private static final LocalTime HORA_CORTE_MANANA_DEFAULT = LocalTime.of(13, 0);
    private static final LocalTime PRIMER_BLOQUE_DEFAULT = LocalTime.of(8, 0);
    private static final int MINUTOS_POR_BLOQUE_DEFAULT = 45;

    public Timetable aDominio(TimetableRequest request) {
        validar(request);

        Map<Integer, BloqueHorarioDto> tablaBloques = tablaDeBloques(request);
        LocalTime horaCorteManana = !esVacio(request.getHoraCorteManana())
                ? parsearHora(request.getHoraCorteManana(), "horaCorteManana")
                : HORA_CORTE_MANANA_DEFAULT;

        List<TimeSlot> timeSlots = generarTimeSlots(request.getDias(), request.getBloquesPorDia(),
                tablaBloques, horaCorteManana);
        Map<Integer, TimeSlot> plantillaPorBloque = new HashMap<>();
        for (TimeSlot ts : timeSlots) {
            if (ts.getDayOfWeek() == 1) { // cualquier dia sirve, la hora es la misma todos los dias
                plantillaPorBloque.put(ts.getBlock(), ts);
            }
        }

        Map<String, Room> salasPorId = new HashMap<>();
        List<Room> rooms = new ArrayList<>();
        if (request.getSalas() != null) {
            for (SalaDto s : request.getSalas()) {
                Room room = new Room(s.getId(), s.getNombre(), s.getColor());
                salasPorId.put(s.getId(), room);
                rooms.add(room);
            }
        }

        Map<String, Teacher> profesoresPorId = new HashMap<>();
        List<Teacher> teachers = new ArrayList<>();
        for (ProfesorDto p : request.getProfesores()) {
            Set<TimeSlot> noDisponible = new HashSet<>();
            if (p.getNoDisponible() != null) {
                for (TimeSlotDto ts : p.getNoDisponible()) {
                    noDisponible.add(new TimeSlot(ts.getDia(), ts.getBloque()));
                }
            }
            LocalTime horaIngreso = !esVacio(p.getHoraIngreso())
                    ? parsearHora(p.getHoraIngreso(), "profesor '" + p.getId() + "'.horaIngreso") : null;
            LocalTime horaSalida = !esVacio(p.getHoraSalida())
                    ? parsearHora(p.getHoraSalida(), "profesor '" + p.getId() + "'.horaSalida") : null;

            Teacher teacher = new Teacher(p.getId(), p.getNombre(), noDisponible,
                    horaIngreso, horaSalida, p.getMaxHorasSemanales());
            profesoresPorId.put(p.getId(), teacher);
            teachers.add(teacher);
        }

        Map<String, Curso> cursosPorId = new HashMap<>();
        List<Curso> cursos = new ArrayList<>();
        for (CursoDto c : request.getCursos()) {
            LocalTime horaSalidaMaxima = !esVacio(c.getHoraSalidaMaxima())
                    ? parsearHora(c.getHoraSalidaMaxima(), "curso '" + c.getId() + "'.horaSalidaMaxima") : null;
            Curso curso = new Curso(c.getId(), c.getNombre(), horaSalidaMaxima);
            cursosPorId.put(c.getId(), curso);
            cursos.add(curso);
        }

        List<Ramo> ramos = new ArrayList<>();
        List<SesionRamo> sesiones = new ArrayList<>();
        Map<String, Integer> horasAsignadasPorProfesor = new HashMap<>();

        for (RamoDto r : request.getRamos()) {
            Curso curso = requerido(cursosPorId, r.getCursoId(), "curso", r.getId());
            Teacher teacher = requerido(profesoresPorId, r.getProfesorId(), "profesor", r.getId());

            Ramo ramo = new Ramo(r.getId(), r.getNombre(), curso, teacher, r.getHorasSemanales(),
                    r.isPreferirManana());
            ramos.add(ramo);

            horasAsignadasPorProfesor.merge(teacher.getId(), r.getHorasSemanales(), Integer::sum);

            List<TimeSlotDto> fijos = r.getHorariosFijos() != null ? r.getHorariosFijos() : List.of();
            List<AsignacionSesionDto> actuales = r.getSesionesActuales() != null ? r.getSesionesActuales() : List.of();
            for (int i = 0; i < r.getHorasSemanales(); i++) {
                TimeSlot fijo = i < fijos.size()
                        ? new TimeSlot(fijos.get(i).getDia(), fijos.get(i).getBloque())
                        : null;
                SesionRamo sesion = new SesionRamo(r.getId() + "-S" + i, ramo, i, fijo);
                if (i < actuales.size()) {
                    AsignacionSesionDto actualDto = actuales.get(i);
                    TimeSlot actual = conHoraDeReloj(actualDto.getDia(), actualDto.getBloque(), plantillaPorBloque);
                    sesion.setTimeslot(actual);
                    sesion.setTimeslotOriginal(actual);

                    if (!esVacio(actualDto.getSalaId())) {
                        Room salaActual = requerido(salasPorId, actualDto.getSalaId(),
                                "sala de sesionesActuales", r.getId() + "-S" + i);
                        sesion.setSala(salaActual);
                        sesion.setSalaOriginal(salaActual);
                    }
                }
                sesiones.add(sesion);
            }
        }

        validarCargaMaximaProfesores(request, profesoresPorId, horasAsignadasPorProfesor);

        return new Timetable(timeSlots, rooms, teachers, cursos, ramos, sesiones);
    }

    private TimeSlot conHoraDeReloj(int dia, int bloque, Map<Integer, TimeSlot> plantillaPorBloque) {
        TimeSlot plantilla = plantillaPorBloque.get(bloque);
        if (plantilla == null) {
            return new TimeSlot(dia, bloque); // bloque fuera de la tabla conocida; sin horas de reloj
        }
        TimeSlot conHora = new TimeSlot(dia, bloque, plantilla.getHoraInicio(), plantilla.getHoraFin());
        conHora.setManana(plantilla.isManana());
        return conHora;
    }

    private <T> T requerido(Map<String, T> mapa, String id, String tipo, String ramoId) {
        T valor = mapa.get(id);
        if (valor == null) {
            throw new IllegalArgumentException(
                    "El ramo '" + ramoId + "' referencia un " + tipo + " inexistente: '" + id + "'");
        }
        return valor;
    }

    private void validar(TimetableRequest request) {
        if (request.getDias() <= 0 || request.getBloquesPorDia() <= 0) {
            throw new IllegalArgumentException("dias y bloquesPorDia deben ser mayores a 0");
        }
        if (request.getProfesores() == null || request.getProfesores().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un profesor");
        }
        if (request.getCursos() == null || request.getCursos().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un curso");
        }
        if (request.getRamos() == null || request.getRamos().isEmpty()) {
            throw new IllegalArgumentException("Debe indicar al menos un ramo");
        }
        if (request.getSalas() == null || request.getSalas().isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe indicar al menos una sala — la sala es parte de lo que el solver debe asignar a cada sesion");
        }
        for (RamoDto r : request.getRamos()) {
            if (r.getHorasSemanales() <= 0) {
                throw new IllegalArgumentException("El ramo '" + r.getId() + "' debe tener horasSemanales > 0");
            }
            if (r.getHorariosFijos() != null && r.getHorariosFijos().size() > r.getHorasSemanales()) {
                throw new IllegalArgumentException(
                        "El ramo '" + r.getId() + "' tiene mas horariosFijos que horasSemanales");
            }
        }
        if (request.getBloques() != null) {
            Set<Integer> numeros = new HashSet<>();
            for (BloqueHorarioDto b : request.getBloques()) {
                if (!numeros.add(b.getNumero())) {
                    throw new IllegalArgumentException("El bloque numero " + b.getNumero() + " esta duplicado en 'bloques'");
                }
                if (esVacio(b.getHoraInicio()) || esVacio(b.getHoraFin())) {
                    throw new IllegalArgumentException(
                            "El bloque numero " + b.getNumero() + " debe traer horaInicio y horaFin (no vacios)");
                }
            }
            for (int i = 1; i <= request.getBloquesPorDia(); i++) {
                if (!numeros.contains(i)) {
                    throw new IllegalArgumentException(
                            "'bloques' debe traer una entrada para cada numero de 1 a bloquesPorDia; falta el bloque " + i);
                }
            }
        }
    }

    /** true si el valor es null, vacio, o solo espacios en blanco — se trata como "no informado". */
    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }

    // Regla 2: valida que ningun profesor supere su maximo de horas semanales frente a curso.
    // No es un constraint del solver: la carga de cada profesor esta fija de antemano por
    // la asignacion curso/ramo/profesor, asi que se valida aqui, al cargar los datos.
    private void validarCargaMaximaProfesores(TimetableRequest request, Map<String, Teacher> profesoresPorId,
                                               Map<String, Integer> horasAsignadasPorProfesor) {
        for (ProfesorDto p : request.getProfesores()) {
            Teacher teacher = profesoresPorId.get(p.getId());
            if (teacher.getMaxHorasSemanales() == null) {
                continue;
            }
            int horasAsignadas = horasAsignadasPorProfesor.getOrDefault(p.getId(), 0);
            if (horasAsignadas > teacher.getMaxHorasSemanales()) {
                throw new IllegalArgumentException(
                        "El profesor '" + p.getNombre() + "' (" + p.getId() + ") tiene " + horasAsignadas
                                + " horas semanales asignadas entre todos sus ramos, pero su maximo de contrato es "
                                + teacher.getMaxHorasSemanales());
            }
        }
    }

    private Map<Integer, BloqueHorarioDto> tablaDeBloques(TimetableRequest request) {
        Map<Integer, BloqueHorarioDto> tabla = new HashMap<>();
        if (request.getBloques() != null) {
            for (BloqueHorarioDto b : request.getBloques()) {
                tabla.put(b.getNumero(), b);
            }
        }
        return tabla;
    }

    private List<TimeSlot> generarTimeSlots(int dias, int bloquesPorDia, Map<Integer, BloqueHorarioDto> tablaBloques,
                                             LocalTime horaCorteManana) {
        List<TimeSlot> slots = new ArrayList<>();
        for (int dia = 1; dia <= dias; dia++) {
            LocalTime cursorDefault = PRIMER_BLOQUE_DEFAULT;
            for (int bloque = 1; bloque <= bloquesPorDia; bloque++) {
                LocalTime horaInicio;
                LocalTime horaFin;
                BloqueHorarioDto def = tablaBloques.get(bloque);
                if (def != null) {
                    horaInicio = parsearHora(def.getHoraInicio(), "bloques[" + bloque + "].horaInicio");
                    horaFin = parsearHora(def.getHoraFin(), "bloques[" + bloque + "].horaFin");
                } else {
                    // Sin tabla informada: bloques de 45 min consecutivos desde las 08:00 (default razonable
                    // para no romper compatibilidad con datasets antiguos). Para un colegio real, informar 'bloques'.
                    horaInicio = cursorDefault;
                    horaFin = cursorDefault.plusMinutes(MINUTOS_POR_BLOQUE_DEFAULT);
                    cursorDefault = horaFin;
                }
                TimeSlot slot = new TimeSlot(dia, bloque, horaInicio, horaFin);
                slot.setManana(horaInicio.isBefore(horaCorteManana));
                slots.add(slot);
            }
        }
        return slots;
    }

    private LocalTime parsearHora(String valor, String campo) {
        try {
            return LocalTime.parse(valor);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "El campo '" + campo + "' tiene un formato de hora invalido: '" + valor + "' (use HH:mm)");
        }
    }
}
