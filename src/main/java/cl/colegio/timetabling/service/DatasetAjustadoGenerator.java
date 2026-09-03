package cl.colegio.timetabling.service;

import cl.colegio.timetabling.dto.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Genera un dataset a escala REAL para probar el motor bajo carga: 24 cursos, 10 bloques
 * por dia, ~30 profesores. A diferencia de DemoDataGenerator (pequeño, para pruebas rapidas
 * durante desarrollo), este dataset esta pensado para verificar que el solver sigue
 * encontrando una solucion factible (y en un tiempo razonable) a la escala real del colegio.
 *
 * Parametros y su razonamiento estan documentados en el README, seccion "Dataset ajustado".
 *
 * Punto deliberadamente AJUSTADO (tension real, no un caso trivial): los cursos de IV Medio
 * tienen horaSalidaMaxima a las 13:30 (regla 4, ej. salida anticipada para PAES), lo que les
 * deja 35 bloques/semana disponibles para sus 34h de carga — un solo bloque de margen.
 *
 * NOTA (corregido tras probarlo a escala real): el horario fijo de Orientacion NO es el mismo
 * para todos los cursos — varia segun la posicion del curso dentro del grupo de su "profesor
 * jefe" (ver SLOTS_ORIENTACION). La primera version fijaba TODAS las Orientaciones al mismo
 * slot (jueves bloque 1); con solo 4 profesores de Historia cubriendo 24 cursos (6 cursos c/u,
 * reutilizados tambien como profesor jefe de Orientacion), eso obligaba a cada profesor a estar
 * en 6 salas a la vez — imposible de cumplir, y el solver terminaba rompiendo el horario fijo
 * en 20 de las 24 sesiones (-20hard). Confirmado con /verificar: "Horario fijo respetado"
 * concentraba el 100% de las violaciones duras.
 */
@Component
public class DatasetAjustadoGenerator {

    private static final int DIAS = 5;
    private static final int BLOQUES_DIA = 10;
    private static final String HORA_CORTE_MANANA = "13:00";

    private static final String[] NIVELES = {
            "1° Básico", "2° Básico", "3° Básico", "4° Básico", "5° Básico", "6° Básico",
            "7° Básico", "8° Básico", "I Medio", "II Medio", "III Medio", "IV Medio"
    };
    private static final String[] SECCIONES = {"A", "B"};

    // {nombre, horasSemanales, tamañoPoolProfesores}
    private static final Object[][] ASIGNATURAS = {
            {"Lenguaje", 6, 5},
            {"Matemática", 6, 5},
            {"Historia", 4, 4}, // tambien dicta Orientacion a sus cursos (1h extra, ver mas abajo)
            {"Ciencias Naturales", 4, 4},
            {"Inglés", 3, 3},
            {"Educación Física", 2, 2},
            {"Artes Visuales", 2, 2},
            {"Música", 2, 2},
            {"Tecnología", 2, 2},
            {"Religión", 2, 2}
    };

    private static final String[] PALETA_COLORES = {
            "#4CAF50", "#2196F3", "#FF7043", "#AB47BC", "#26A69A", "#FFCA28",
            "#8D6E63", "#78909C", "#EC407A", "#5C6BC0", "#7CB342", "#29B6F6"
    };

    public TimetableRequest generar() {
        TimetableRequest request = new TimetableRequest();
        request.setDias(DIAS);
        request.setBloquesPorDia(BLOQUES_DIA);
        request.setHoraCorteManana(HORA_CORTE_MANANA);
        request.setBloques(generarBloques());

        List<CursoDto> cursos = generarCursos();
        request.setCursos(cursos);

        List<SalaDto> salas = generarSalas(cursos);
        request.setSalas(salas);

        Map<String, List<String>> poolProfesoresPorAsignatura = new LinkedHashMap<>();
        List<ProfesorDto> profesores = generarProfesores(poolProfesoresPorAsignatura);
        request.setProfesores(profesores);

        request.setRamos(generarRamos(cursos, poolProfesoresPorAsignatura));

        return request;
    }

    // Regla 1: horario real con recreo (10:15-11:15... en realidad 15 min) y almuerzo,
    // no bloques corridos — asi se estresa tambien la tabla de horarios parametrizable.
    private List<BloqueHorarioDto> generarBloques() {
        String[][] horarios = {
                {"08:00", "08:45"}, {"08:45", "09:30"}, {"09:30", "10:15"}, {"10:15", "11:00"},
                {"11:15", "12:00"}, // 15 min de recreo entre bloque 4 y 5
                {"12:00", "12:45"}, {"12:45", "13:30"},
                {"14:15", "15:00"}, // 45 min de almuerzo entre bloque 7 y 8
                {"15:00", "15:45"}, {"15:45", "16:30"}
        };
        List<BloqueHorarioDto> bloques = new ArrayList<>();
        for (int i = 0; i < horarios.length; i++) {
            BloqueHorarioDto b = new BloqueHorarioDto();
            b.setNumero(i + 1);
            b.setHoraInicio(horarios[i][0]);
            b.setHoraFin(horarios[i][1]);
            bloques.add(b);
        }
        return bloques;
    }

    private List<CursoDto> generarCursos() {
        List<CursoDto> cursos = new ArrayList<>();
        for (String nivel : NIVELES) {
            for (String seccion : SECCIONES) {
                CursoDto curso = new CursoDto();
                curso.setId(idCurso(nivel, seccion));
                curso.setNombre(nivel + " " + seccion);
                // Punto ajustado: IV Medio con salida anticipada (regla 4) — 35 bloques/semana
                // disponibles para 34h de carga, un solo bloque de margen.
                if (nivel.equals("IV Medio")) {
                    curso.setHoraSalidaMaxima("13:30");
                }
                cursos.add(curso);
            }
        }
        return cursos;
    }

    private String idCurso(String nivel, String seccion) {
        return "C-" + nivel.replace(" ", "").replace("°", "") + seccion;
    }

    private List<SalaDto> generarSalas(List<CursoDto> cursos) {
        List<SalaDto> salas = new ArrayList<>();
        int colorIdx = 0;
        for (CursoDto curso : cursos) {
            SalaDto sala = new SalaDto();
            sala.setId("SALA-" + curso.getId());
            sala.setNombre("Aula " + curso.getNombre());
            sala.setColor(PALETA_COLORES[colorIdx++ % PALETA_COLORES.length]);
            salas.add(sala);
        }
        salas.add(sala("SALA-GIM1", "Gimnasio 1", "#E53935"));
        salas.add(sala("SALA-GIM2", "Gimnasio 2", "#D32F2F"));
        salas.add(sala("SALA-LAB", "Laboratorio de Ciencias", "#00897B"));
        salas.add(sala("SALA-COMP", "Sala de Computación", "#3949AB"));
        return salas;
    }

    private SalaDto sala(String id, String nombre, String color) {
        SalaDto s = new SalaDto();
        s.setId(id);
        s.setNombre(nombre);
        s.setColor(color);
        return s;
    }

    // Genera el pool de profesores por asignatura y los deja registrados en
    // poolProfesoresPorAsignatura (asignatura -> lista de ids de profesor, en orden).
    private List<ProfesorDto> generarProfesores(Map<String, List<String>> poolProfesoresPorAsignatura) {
        List<ProfesorDto> profesores = new ArrayList<>();
        int contadorGlobal = 1;

        for (Object[] asignatura : ASIGNATURAS) {
            String nombreAsignatura = (String) asignatura[0];
            int tamañoPool = (int) asignatura[2];

            List<String> idsDelPool = new ArrayList<>();
            for (int i = 1; i <= tamañoPool; i++) {
                String id = "P" + contadorGlobal;
                ProfesorDto p = new ProfesorDto();
                p.setId(id);
                p.setNombre("Profesor " + nombreAsignatura + " " + i);
                p.setMaxHorasSemanales(32); // regla 2: tope de contrato realista

                // Un profesor de Religion queda part-time (solo mañana) para estresar
                // tambien la ventana de contrato (regla 3) a esta escala.
                boolean esPartTimeDemo = nombreAsignatura.equals("Religión") && i == 1;
                p.setHoraIngreso("08:00");
                p.setHoraSalida(esPartTimeDemo ? "13:30" : "16:30");

                profesores.add(p);
                idsDelPool.add(id);
                contadorGlobal++;
            }
            poolProfesoresPorAsignatura.put(nombreAsignatura, idsDelPool);
        }
        return profesores;
    }

    // Slots candidatos para el horario fijo de Orientacion. Con pool de Historia = 4 profesores
    // y 24 cursos, cada profesor termina siendo "jefe" de 6 cursos (24/4) — por eso se necesitan
    // al menos 6 slots distintos: si TODOS los cursos de un mismo profesor jefe compartieran el
    // mismo horario fijo, ese profesor tendria que estar en varias salas a la vez (imposible).
    // En la practica esto tambien es realista: distintos niveles suelen tener Orientacion en
    // horarios distintos, no todo el colegio a la vez.
    private static final int[][] SLOTS_ORIENTACION = {
            {1, 1}, {2, 1}, {3, 1}, {4, 1}, {5, 1}, {1, 2}
    };

    private List<RamoDto> generarRamos(List<CursoDto> cursos, Map<String, List<String>> poolProfesoresPorAsignatura) {
        List<RamoDto> ramos = new ArrayList<>();
        int poolHistoria = poolProfesoresPorAsignatura.get("Historia").size();

        for (int cursoIdx = 0; cursoIdx < cursos.size(); cursoIdx++) {
            CursoDto curso = cursos.get(cursoIdx);
            String profesorHistoria = null;

            for (Object[] asignatura : ASIGNATURAS) {
                String nombreAsignatura = (String) asignatura[0];
                int horas = (int) asignatura[1];
                List<String> pool = poolProfesoresPorAsignatura.get(nombreAsignatura);
                String profesorId = pool.get(cursoIdx % pool.size()); // round-robin sobre el pool

                if (nombreAsignatura.equals("Historia")) {
                    profesorHistoria = profesorId; // se reutiliza para Orientacion (profesor jefe)
                }

                RamoDto ramo = new RamoDto();
                ramo.setId(curso.getId() + "-" + codigoAsignatura(nombreAsignatura));
                ramo.setNombre(nombreAsignatura);
                ramo.setCursoId(curso.getId());
                ramo.setProfesorId(profesorId);
                ramo.setHorasSemanales(horas);
                ramo.setPreferirManana(nombreAsignatura.equals("Lenguaje") || nombreAsignatura.equals("Matemática"));
                ramos.add(ramo);
            }

            // Orientacion: la dicta el mismo profesor de Historia de ese curso (patron
            // "profesor jefe"), demostrando que un profesor puede dictar varios ramos a un
            // mismo curso. El horario fijo VARIA segun la posicion del curso dentro del grupo
            // de ese profesor jefe (ver SLOTS_ORIENTACION), para que un mismo profesor nunca
            // tenga dos Orientaciones fijas a la misma hora.
            int posicionEnGrupo = cursoIdx / poolHistoria; // 0..5, cual de los 6 cursos de este profesor
            int[] slot = SLOTS_ORIENTACION[posicionEnGrupo % SLOTS_ORIENTACION.length];

            RamoDto orientacion = new RamoDto();
            orientacion.setId(curso.getId() + "-ORI");
            orientacion.setNombre("Orientación");
            orientacion.setCursoId(curso.getId());
            orientacion.setProfesorId(profesorHistoria);
            orientacion.setHorasSemanales(1);
            List<TimeSlotDto> fijo = new ArrayList<>();
            fijo.add(new TimeSlotDto(slot[0], slot[1]));
            orientacion.setHorariosFijos(fijo);
            ramos.add(orientacion);
        }
        return ramos;
    }

    private static final Map<String, String> CODIGOS_ASIGNATURA = Map.ofEntries(
            Map.entry("Lenguaje", "LEN"),
            Map.entry("Matemática", "MAT"),
            Map.entry("Historia", "HIS"),
            Map.entry("Ciencias Naturales", "CIE"),
            Map.entry("Inglés", "ING"),
            Map.entry("Educación Física", "EDF"),
            Map.entry("Artes Visuales", "ART"),
            Map.entry("Música", "MUS"),
            Map.entry("Tecnología", "TEC"),
            Map.entry("Religión", "REL")
    );

    private String codigoAsignatura(String nombre) {
        return CODIGOS_ASIGNATURA.getOrDefault(nombre, nombre.substring(0, Math.min(3, nombre.length())).toUpperCase());
    }
}
