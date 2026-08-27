package cl.colegio.timetabling.service;

import cl.colegio.timetabling.domain.Timetable;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.ScoreManager;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TimetableVerificationService {

    private final ScoreManager<Timetable, HardSoftScore> scoreManager;

    public TimetableVerificationService(ScoreManager<Timetable, HardSoftScore> scoreManager) {
        this.scoreManager = scoreManager;
    }

    /**
     * Calcula el score de un horario YA armado, sin optimizar nada (no invoca al solver).
     * Sirve para responder "este horario que ya tengo, ¿es factible?".
     * Nota: OptaPlanner solo actualiza el score si se lo pedimos explicitamente porque
     * un Timetable recien mapeado desde JSON no trae el score calculado.
     */
    public ResultadoVerificacion verificar(Timetable timetable) {
        ScoreExplanation<Timetable, HardSoftScore> explicacion = scoreManager.explainScore(timetable);
        HardSoftScore score = explicacion.getScore();

        List<DetalleRestriccion> detalle = explicacion.getConstraintMatchTotalMap().values().stream()
                .filter(cmt -> cmt.getScore().compareTo(HardSoftScore.ZERO) != 0)
                .map(cmt -> new DetalleRestriccion(
                        cmt.getConstraintName(), cmt.getScore().toString(), cmt.getConstraintMatchCount()))
                .collect(Collectors.toList());

        return new ResultadoVerificacion(score, score.getHardScore() >= 0, detalle);
    }

    public static class ResultadoVerificacion {
        private final HardSoftScore score;
        private final boolean factible;
        private final List<DetalleRestriccion> detalle;

        public ResultadoVerificacion(HardSoftScore score, boolean factible, List<DetalleRestriccion> detalle) {
            this.score = score;
            this.factible = factible;
            this.detalle = detalle;
        }

        public HardSoftScore getScore() {
            return score;
        }

        public boolean isFactible() {
            return factible;
        }

        public List<DetalleRestriccion> getDetalle() {
            return detalle;
        }
    }

    public static class DetalleRestriccion {
        private final String restriccion;
        private final String score;
        private final int cantidadOcurrencias;

        public DetalleRestriccion(String restriccion, String score, int cantidadOcurrencias) {
            this.restriccion = restriccion;
            this.score = score;
            this.cantidadOcurrencias = cantidadOcurrencias;
        }

        public String getRestriccion() {
            return restriccion;
        }

        public String getScore() {
            return score;
        }

        public int getCantidadOcurrencias() {
            return cantidadOcurrencias;
        }
    }
}
