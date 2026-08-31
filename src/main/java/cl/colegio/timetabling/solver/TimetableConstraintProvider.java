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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TimetableConstraintProvider implements ConstraintProvider {

    // Regla de horas seguidas por ramo: evitar mas de 3 horas seguidas de un mismo ramo,
    // prefiriendo dejarlas en parejas de 2 bloques en la mayoria de los casos.
    private static final int MAX_HORAS_SEGUIDAS_RAMO = 3;
    private static final int LARGO_RACHA_IDEAL = 2;
    private static final int PENALIZACION_FRAGMENTACION = 8; // por cada hueco entre sesiones del mismo ramo/dia
    private static final int PENALIZACION_LARGO_SUBOPTIMO = 1; // racha de 1 o de 3 (no ideal, pero tolerable)
    private static final int PENALIZACION_POR_HORA_EXCEDIDA = 5; // por cada hora sobre el maximo (4a, 5a, ...)

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[]{
                // --- HARD ---
                profesorSinChoque(factory),
                cursoSinChoque(factory),
                salaSinChoque(factory),
                profesorDisponible(factory),
                horarioFijoRespetado(factory),
                profesorDentroDeVentanaContrato(factory),
                cursoDentroDeHorarioSalida(factory),
                // --- SOFT ---
                balanceCargaDiariaPorCurso(factory),
                evitarHorasSeguidasExcesivas(factory),
                preferirMantenerHorarioOriginal(factory),
                preferirMantenerSalaOriginal(factory),
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

    // Dos sesiones NO pueden coincidir en la misma sala al mismo tiempo. La sala es una
    // variable de planificacion por SESION (no un dato fijo del ramo): un mismo ramo puede
    // terminar con sesiones en salas distintas segun el dia.
    private Constraint salaSinChoque(ConstraintFactory factory) {
        return factory.forEachUniquePair(SesionRamo.class,
                        Joiners.equal(SesionRamo::getSala),
                        Joiners.equal(SesionRamo::getTimeslot))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Sala sin choque de horario");
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

    // SOFT: evitar mas de 3 horas seguidas de un mismo ramo el mismo dia, y evitar que
    // las sesiones de un mismo ramo queden separadas con huecos entre medio (ej. bloque 1
    // y bloque 3 del mismo ramo el mismo dia, con otra cosa en el bloque 2) — se prefiere
    // que si hay 2+ sesiones de un ramo el mismo dia, queden juntas, idealmente en pares.
    private Constraint evitarHorasSeguidasExcesivas(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .groupBy(s -> s.getRamo(),
                        s -> s.getTimeslot().getDayOfWeek(),
                        ConstraintCollectors.toList())
                .penalize(HardSoftScore.ONE_SOFT, (ramo, dia, sesiones) -> penalizacionHorasSeguidasRamo(sesiones))
                .asConstraint("Evitar horas seguidas excesivas de un mismo ramo");
    }

    // SOFT: al editar un horario ya cargado, preferir no mover una sesion de su horario
    // original salvo que sea necesario para arreglar un choque. Es null (no aplica) cuando
    // se genera un horario desde cero, asi que no afecta el flujo de POST /solve.
    // Peso mayor al de balance/horas-seguidas para que el solver priorice "tocar lo minimo".
    private Constraint preferirMantenerHorarioOriginal(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .filter(s -> s.getTimeslotOriginal() != null && !s.getTimeslotOriginal().equals(s.getTimeslot()))
                .penalize(HardSoftScore.ofSoft(10))
                .asConstraint("Mantener horario original salvo necesidad");
    }

    // SOFT: idem, pero para la sala. Al mover una sesion, si se puede resolver un choque
    // dejandola en la misma sala que tenia, se prefiere eso antes que reasignarle otra sala.
    private Constraint preferirMantenerSalaOriginal(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .filter(s -> s.getSalaOriginal() != null && !s.getSalaOriginal().equals(s.getSala()))
                .penalize(HardSoftScore.ofSoft(10))
                .asConstraint("Mantener sala original salvo necesidad");
    }

    // Regla 5: dar preferencia a que ciertos ramos (Lenguaje, Matematica, etc, segun
    // Ramo.preferirManana) se dicten en horario de manana (segun el corte parametrizable).
    private Constraint preferirRamosEnLaManana(ConstraintFactory factory) {
        return factory.forEach(SesionRamo.class)
                .filter(s -> s.getRamo().isPreferirManana() && !s.getTimeslot().isManana())
                .penalize(HardSoftScore.ONE_SOFT)
                .asConstraint("Preferir ramos de manana en horario matutino");
    }

    // Calcula la penalizacion para las sesiones de UN ramo en UN dia:
    // 1) Fragmentacion: si quedan en mas de una racha (hay un hueco entre sesiones del
    //    mismo ramo ese dia), penaliza fuerte por cada racha de mas.
    // 2) Largo de cada racha: ideal = 2 (sin penalizacion). 1 o 3 se toleran con
    //    penalizacion leve. 4+ se penaliza fuerte y creciente ("evitar mas de 3 seguidas").
    private int penalizacionHorasSeguidasRamo(List<SesionRamo> sesionesDelDia) {
        List<Integer> bloques = sesionesDelDia.stream()
                .map(s -> s.getTimeslot().getBlock())
                .sorted()
                .collect(Collectors.toList());

        List<Integer> largosDeRachas = new ArrayList<>();
        int rachaActual = 1;
        for (int i = 1; i < bloques.size(); i++) {
            if (bloques.get(i) - bloques.get(i - 1) == 1) {
                rachaActual++;
            } else {
                largosDeRachas.add(rachaActual);
                rachaActual = 1;
            }
        }
        largosDeRachas.add(rachaActual);

        int penalizacion = 0;

        // Huecos entre sesiones del mismo ramo el mismo dia (ej. bloque 1 y bloque 3 sueltos).
        if (largosDeRachas.size() > 1) {
            penalizacion += (largosDeRachas.size() - 1) * PENALIZACION_FRAGMENTACION;
        }

        for (int largo : largosDeRachas) {
            if (largo == LARGO_RACHA_IDEAL) {
                continue; // racha de 2: ideal, sin penalizacion
            }
            if (largo > MAX_HORAS_SEGUIDAS_RAMO) {
                penalizacion += (largo - MAX_HORAS_SEGUIDAS_RAMO) * PENALIZACION_POR_HORA_EXCEDIDA;
            } else {
                penalizacion += PENALIZACION_LARGO_SUBOPTIMO; // racha de 1 o de 3
            }
        }

        return penalizacion;
    }
}
