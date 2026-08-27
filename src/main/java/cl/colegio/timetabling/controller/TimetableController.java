package cl.colegio.timetabling.controller;

import cl.colegio.timetabling.domain.SesionRamo;
import cl.colegio.timetabling.domain.Timetable;
import cl.colegio.timetabling.domain.TimeSlot;
import cl.colegio.timetabling.dto.MoverSesionRequest;
import cl.colegio.timetabling.dto.RamoDto;
import cl.colegio.timetabling.dto.TimetableRequest;
import cl.colegio.timetabling.dto.TimetableRequestMapper;
import cl.colegio.timetabling.service.DemoDataGenerator;
import cl.colegio.timetabling.service.TimetableRequestBuilderService;
import cl.colegio.timetabling.service.TimetableService;
import cl.colegio.timetabling.service.TimetableVerificationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/timetable")
// Habilita el front Angular (ng serve corre por defecto en localhost:4200) para llamar a esta API
// que corre en otro puerto (8080). En produccion, restringir a los origenes reales.
@CrossOrigin(origins = {"http://localhost:4200"})
public class TimetableController {

    private final TimetableService timetableService;
    private final DemoDataGenerator demoDataGenerator;
    private final TimetableRequestMapper requestMapper;
    private final TimetableVerificationService verificationService;
    private final TimetableRequestBuilderService requestBuilderService;

    public TimetableController(TimetableService timetableService,
                                DemoDataGenerator demoDataGenerator,
                                TimetableRequestMapper requestMapper,
                                TimetableVerificationService verificationService,
                                TimetableRequestBuilderService requestBuilderService) {
        this.timetableService = timetableService;
        this.demoDataGenerator = demoDataGenerator;
        this.requestMapper = requestMapper;
        this.verificationService = verificationService;
        this.requestBuilderService = requestBuilderService;
    }

    // Resuelve el dataset de ejemplo y devuelve el horario en formato legible.
    @GetMapping("/demo/solve")
    public Map<String, Object> resolverDemo() {
        Timetable problema = demoDataGenerator.generarProblema();
        Timetable solucion = timetableService.resolver(problema);
        return formatear(solucion);
    }

    // Arma el TimetableRequest a partir de lo cargado via CRUD (profesores/cursos/salas/ramos/config),
    // SIN resolver. Sirve para que el frontend lo use como base para /solve, /verificar o /mover-sesion.
    @GetMapping("/actual")
    public TimetableRequest obtenerActual() {
        return requestBuilderService.construir();
    }

    // Arma el TimetableRequest desde los datos CRUD y lo resuelve de una, sin que el
    // usuario tenga que armar/copiar el JSON a mano.
    @GetMapping("/actual/solve")
    public Map<String, Object> resolverActual() {
        Timetable problema = mapearOFallar(requestBuilderService.construir());
        Timetable solucion = timetableService.resolver(problema);
        return formatear(solucion);
    }

    // Recibe cursos/profesores/ramos en JSON, resuelve DESDE CERO y devuelve el horario resultante.
    @PostMapping("/solve")
    public Map<String, Object> resolver(@RequestBody TimetableRequest request) {
        Timetable problema = mapearOFallar(request);
        Timetable solucion = timetableService.resolver(problema);
        return formatear(solucion);
    }

    // Recibe un horario YA armado (cada ramo con sesionesActuales completas) y solo
    // CALCULA el score: no optimiza nada, no mueve ninguna sesion. Sirve para precargar
    // un horario existente y saber de inmediato si es factible o no, y por que.
    @PostMapping("/verificar")
    public Map<String, Object> verificar(@RequestBody TimetableRequest request) {
        validarHorarioCompleto(request);
        Timetable timetable = mapearOFallar(request);

        TimetableVerificationService.ResultadoVerificacion resultado = verificationService.verificar(timetable);

        List<Map<String, Object>> detalle = resultado.getDetalle().stream()
                .map(d -> Map.<String, Object>of(
                        "restriccion", d.getRestriccion(),
                        "score", d.getScore(),
                        "ocurrencias", d.getCantidadOcurrencias()))
                .collect(Collectors.toList());

        Map<String, Object> respuesta = new java.util.HashMap<>(formatear(timetable, resultado.getScore().toString(), resultado.isFactible()));
        respuesta.put("detalle", detalle);
        return respuesta;
    }

    // Mueve UNA sesion puntual a un nuevo dia/bloque, anclandola ahi, y re-resuelve para
    // arreglar solo lo que ese cambio haya roto (el resto del horario se mantiene salvo
    // que sea estrictamente necesario tocarlo). Devuelve el horario resultante con cada
    // sesion marcada "movida":true/false para que el frontend resalte que mas cambio.
    @PostMapping("/mover-sesion")
    public Map<String, Object> moverSesion(@RequestBody MoverSesionRequest request) {
        Timetable horarioActual = mapearOFallar(request.getHorario());
        TimeSlot nuevoSlot = new TimeSlot(request.getNuevoSlot().getDia(), request.getNuevoSlot().getBloque());

        Timetable resultado;
        try {
            resultado = timetableService.moverSesion(
                    horarioActual, request.getRamoId(), request.getIndiceSesion(), nuevoSlot);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        return formatear(resultado);
    }

    private Timetable mapearOFallar(TimetableRequest request) {
        try {
            return requestMapper.aDominio(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // /verificar necesita el horario COMPLETO (no puede calcular el score de sesiones sin ubicar).
    private void validarHorarioCompleto(TimetableRequest request) {
        if (request.getRamos() == null) {
            return;
        }
        for (RamoDto r : request.getRamos()) {
            int cantidadActuales = r.getSesionesActuales() == null ? 0 : r.getSesionesActuales().size();
            if (cantidadActuales < r.getHorasSemanales()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para verificar un horario, el ramo '" + r.getId()
                                + "' debe traer sesionesActuales para las " + r.getHorasSemanales()
                                + " horas semanales (trae " + cantidadActuales + ")");
            }
        }
    }

    private Map<String, Object> formatear(Timetable solucion) {
        return formatear(solucion, solucion.getScore() != null ? solucion.getScore().toString() : null,
                solucion.getScore() != null && solucion.getScore().getHardScore() >= 0);
    }

    private Map<String, Object> formatear(Timetable solucion, String score, boolean factible) {
        List<Map<String, Object>> sesiones = solucion.getSesionRamoList().stream()
                .sorted(Comparator.comparingInt((SesionRamo s) -> s.getTimeslot().getDayOfWeek())
                        .thenComparingInt(s -> s.getTimeslot().getBlock()))
                .map(s -> Map.<String, Object>of(
                        "ramoId", s.getRamo().getId(),
                        "indiceSesion", s.getIndiceSesion(),
                        "cursoId", s.getRamo().getCurso().getId(),
                        "curso", s.getRamo().getCurso().getName(),
                        "profesorId", s.getRamo().getTeacher().getId(),
                        "ramo", s.getRamo().getName(),
                        "profesor", s.getRamo().getTeacher().getName(),
                        "dia", s.getTimeslot().getDayOfWeek(),
                        "bloque", s.getTimeslot().getBlock(),
                        "movida", s.isMovida()
                ))
                .collect(Collectors.toList());

        Map<String, Object> respuesta = new java.util.HashMap<>();
        respuesta.put("score", score);
        respuesta.put("factible", factible);
        respuesta.put("sesiones", sesiones);
        return respuesta;
    }
}
