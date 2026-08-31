package cl.colegio.timetabling.service;

import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import cl.colegio.timetabling.domain.Room;
import cl.colegio.timetabling.domain.SesionRamo;
import cl.colegio.timetabling.domain.Teacher;
import cl.colegio.timetabling.domain.TimeSlot;
import cl.colegio.timetabling.domain.Timetable;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class TimetableService {

    private final SolverManager<Timetable, String> solverManager;

    public TimetableService(SolverManager<Timetable, String> solverManager) {
        this.solverManager = solverManager;
    }

    /**
     * Resuelve el problema de forma sincrona (bloqueante) y devuelve la mejor solucion
     * encontrada dentro del tiempo configurado en application.yml (termination.spent-limit).
     */
    public Timetable resolver(Timetable problema) {
        String jobId = UUID.randomUUID().toString();
        SolverJob<Timetable, String> job = solverManager.solve(jobId, problema);
        try {
            return job.getFinalBestSolution();
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Fallo al resolver el horario", e);
        }
    }

    /**
     * Caso "profesor renuncia a mitad de semestre": se marcan como no disponibles TODOS
     * los timeslots restantes para ese profesor y se vuelve a resolver.
     *
     * Nota sobre re-solve incremental de verdad (produccion): en vez de relanzar el solver
     * desde cero, Timefold permite mantener un SolverJob vivo y enviarle un ProblemChange
     * (solverManager.solve(jobId, ...) con un ProblemChangeDirector) que solo perturba las
     * variables afectadas y re-optimiza desde el estado actual, mucho mas rapido que un
     * cold-restart cuando el horario ya lleva semanas en curso. Para un colegio de tamaño
     * normal el cold-restart de abajo ya resuelve en segundos, asi que se deja como la
     * opcion simple por defecto; el enfoque de ProblemChange queda documentado para cuando
     * el volumen de datos lo justifique.
     */
    public Timetable reemplanificarPorAusenciaProfesor(Timetable problemaActual, String teacherId) {
        Teacher teacher = problemaActual.getTeacherList().stream()
                .filter(t -> t.getId().equals(teacherId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Profesor no encontrado: " + teacherId));

        // Bloquea todos los timeslots restantes para ese profesor.
        for (TimeSlot slot : problemaActual.getTimeSlotList()) {
            teacher.getUnavailableTimeSlots().add(slot);
        }

        // Las sesiones ya asignadas a ese profesor deben reasignarse a otro profesor
        // ANTES de re-resolver (el solver no cambia el profesor de un ramo, solo el horario;
        // el cambio de profesor titular de un ramo es una decision administrativa, no del solver).
        // Aqui se asume que ya se actualizo Ramo.teacher para los ramos afectados antes de llamar
        // a este metodo, para el reemplazante correspondiente.

        return resolver(problemaActual);
    }

    /**
     * Mueve una sesion puntual a un nuevo dia/bloque (y opcionalmente una nueva sala) y
     * re-resuelve para arreglar cualquier choque que ese cambio haya provocado, tocando lo
     * minimo posible del resto del horario (ver restricciones "Mantener horario/sala original
     * salvo necesidad" en TimetableConstraintProvider).
     *
     * Si nuevaSala es null, la sesion mantiene la sala que ya tenia (solo cambia de horario).
     *
     * La sesion movida queda "pinned" (@PlanningPin): el solver ya no la va a
     * volver a mover (ni de horario ni de sala), es una decision del usuario que se respeta tal cual.
     */
    public Timetable moverSesion(Timetable horarioActual, String ramoId, int indiceSesion,
                                  TimeSlot nuevoSlot, Room nuevaSala) {
        SesionRamo objetivo = horarioActual.getSesionRamoList().stream()
                .filter(s -> s.getRamo().getId().equals(ramoId) && s.getIndiceSesion() == indiceSesion)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontro la sesion " + indiceSesion + " del ramo " + ramoId));

        if (objetivo.isFixed() && !objetivo.getFixedTimeSlot().equals(nuevoSlot)) {
            throw new IllegalArgumentException(
                    "Esa sesion tiene horario obligatorio fijo (" + objetivo.getFixedTimeSlot()
                            + ") y no puede moverse a otro horario");
        }

        objetivo.setTimeslot(nuevoSlot);
        objetivo.setTimeslotOriginal(nuevoSlot); // el cambio manual pasa a ser el nuevo "de referencia"

        if (nuevaSala != null) {
            objetivo.setSala(nuevaSala);
            objetivo.setSalaOriginal(nuevaSala);
        }
        // si nuevaSala es null, se deja la sala que la sesion ya tenia (viene de sesionesActuales)

        objetivo.setPinned(true);

        return resolver(horarioActual);
    }
}
