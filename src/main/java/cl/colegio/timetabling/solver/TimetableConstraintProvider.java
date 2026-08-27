package cl.colegio.timetabling.solver;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import cl.colegio.timetabling.domain.SesionRamo;
import cl.colegio.timetabling.domain.TimeSlot;
import cl.colegio.timetabling.domain.Curso;

import java.util.List;
import java.util.stream.Collectors;

public class TimetableConstraintProvider implements ConstraintProvider {

    private static final int MAX_HORAS_SEGUIDAS_DESEABLE = 4;

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                // --- HARD ---
                profesorSinChoque(factory),
                cursoSinChoque(factory),
                salaCompartidaSinChoque(factory),
                profesorDisponible(factory),
                horarioFijoRespetado(factory),
                profesorDentroDeVentanaContrato(factory),
                cursoDentroDeHorarioSalida(factory),
                // --- SOFT ---
                balanceCargaDiariaPorCurso(factory),
                evitarHorasSeguidasExcesivas(factory),
                preferirMantenerAsignacionOriginal(factory),
                preferirRamosEnLaManana(factory)
        };
    }

    // Regla 3: la sesion debe caer dentro de la ventana horaria de contrato del profesor
    // (hora de ingreso / salida). Si el profesor no tiene ventana definida, no aplica.
    private Constraint profesorDentroDeVentanaContrato(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .filter(s -> s.getRamo().getTeacher().estaFueraDeVentanaContrato(s.getTimeslot()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Profesor dentro de su ventana horaria de contrato");
    }

    // Regla 4: la sesion no puede terminar despues de la hora de salida maxima del curso
    // (ej. IV medios con salida distinta en ciertos periodos). Si el curso no tiene tope, no aplica.
    private Constraint cursoDentroDeHorarioSalida(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .filter(s -> {
                    Curso curso = s.getRamo().getCurso();
                    TimeSlot ts = s.getTimeslot();
                    return curso.getHoraSalidaMaxima() != null
                            && ts.getHoraFin() != null
                            && ts.getHoraFin().isAfter(curso.getHoraSalidaMaxima());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Curso dentro de su horario de salida");
    }

    // Un profesor no puede dictar dos sesiones al mismo tiempo.
    private Constraint profesorSinChoque(ConstraintFactory factory) {
        return factory.forEachUniquePair(SesionRamo.class,
                        Joiners.equal(s -> s.getRamo().getTeacher()),
                        Joiners.equal(SesionRamo::getTimeslot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Profesor sin choque de horario");
    }

    // Un curso no puede tener dos ramos al mismo tiempo.
    private Constraint cursoSinChoque(ConstraintFactory factory) {
        return factory.forEachUniquePair(SesionRamo.class,
                        Joiners.equal(s -> s.getRamo().getCurso()),
                        Joiners.equal(SesionRamo::getTimeslot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Curso sin choque de horario");
    }

    // Dos ramos que requieren la misma sala compartida (ej. gimnasio) no pueden coincidir.
    private Constraint salaCompartidaSinChoque(ConstraintFactory factory) {
        return factory.forEachUniquePair(SesionRamo.class,
                        Joiners.equal(s -> s.getRamo().getRequiredRoom()),
                        Joiners.equal(SesionRamo::getTimeslot))
                .filter((s1, s2) -> s1.getRamo().getRequiredRoom() != null)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Sala compartida sin choque");
    }

    // La sesion debe caer en un TimeSlot donde el profesor este disponible segun su contrato.
    private Constraint profesorDisponible(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .filter(s -> !s.getRamo().getTeacher().isAvailableAt(s.getTimeslot()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Profesor debe estar disponible");
    }

    // Sesiones con horario obligatorio predefinido (ej. Orientacion) deben quedar exactamente ahi.
    private Constraint horarioFijoRespetado(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .filter(s -> s.isFixed() && !s.getFixedTimeSlot().equals(s.getTimeslot()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Horario fijo respetado");
    }

    // SOFT: repartir las sesiones de cada curso de forma pareja entre los dias de la semana.
    // Minimizar la suma de cuadrados de sesiones por (curso, dia) empuja hacia el balance,
    // dado un total fijo de sesiones por curso.
    private Constraint balanceCargaDiariaPorCurso(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .groupBy(s -> s.getRamo().getCurso(),
                        s -> s.getTimeslot().getDayOfWeek(),
                        ConstraintCollectors.count())
                .penalize(HardSoftScore.ONE_SOFT, (curso, dia, cantidad) -> cantidad * cantidad)
                .asConstraint("Balancear carga diaria por curso");
    }

    // SOFT: evitar que un profesor tenga demasiadas horas seguidas el mismo dia.
    private Constraint evitarHorasSeguidasExcesivas(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .groupBy(s -> s.getRamo().getTeacher(),
                        s -> s.getTimeslot().getDayOfWeek(),
                        ConstraintCollectors.toList())
                .penalize(HardSoftScore.ONE_SOFT, (teacher, dia, sesiones) -> penalizacionHorasSeguidas(sesiones))
                .asConstraint("Evitar horas seguidas excesivas");
    }

    // SOFT: al editar un horario ya cargado, preferir no mover una sesion de su posicion
    // original salvo que sea necesario para arreglar un choque. Es null (no aplica) cuando
    // se genera un horario desde cero, asi que no afecta el flujo de POST /solve.
    // Peso mayor al de balance/horas-seguidas para que el solver priorice "tocar lo minimo".
    private Constraint preferirMantenerAsignacionOriginal(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .filter(s -> s.getTimeslotOriginal() != null && !s.getTimeslotOriginal().equals(s.getTimeslot()))
                .penalize(HardSoftScore.ofSoft(10))
                .asConstraint("Mantener asignacion original salvo necesidad");
    }

    // Regla 5: dar preferencia a que ciertos ramos (Lenguaje, Matematica, etc, segun
    // Ramo.preferirManana) se dicten en horario de manana (segun el corte parametrizable).
    private Constraint preferirRamosEnLaManana(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .filter(s -> s.getRamo().isPreferirManana() && !s.getTimeslot().isManana())
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Preferir ramos de manana en horario matutino");
    }

    // Calcula el exceso sobre MAX_HORAS_SEGUIDAS_DESEABLE en la racha continua mas larga del dia.
    private int penalizacionHorasSeguidas(List<SesionRamo> sesionesDelDia) {
        List<Integer> bloques = sesionesDelDia.stream()
                .map(s -> s.getTimeslot().getBlock())
                .sorted()
                .collect(Collectors.toList());

        int penalizacion = 0;
        int rachaActual = 1;
        for (int i = 1; i < bloques.size(); i++) {
            if (bloques.get(i) - bloques.get(i - 1) == 1) {
                rachaActual++;
            } else {
                penalizacion += Math.max(0, rachaActual - MAX_HORAS_SEGUIDAS_DESEABLE);
                rachaActual = 1;
            }
        }
        penalizacion += Math.max(0, rachaActual - MAX_HORAS_SEGUIDAS_DESEABLE);
        return penalizacion;
    }
}
