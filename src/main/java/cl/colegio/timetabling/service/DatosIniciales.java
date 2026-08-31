package cl.colegio.timetabling.service;

import cl.colegio.timetabling.dto.*;
import cl.colegio.timetabling.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Precarga los repositorios CRUD con un dataset de ejemplo al arrancar, para que
 * /api/timetable/actual/solve y las paginas CRUD del frontend tengan algo que
 * mostrar de inmediato. En produccion esto se reemplaza por los datos reales
 * cargados por el usuario (o una migracion desde el sistema anterior).
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final ProfesorRepository profesorRepository;
    private final CursoRepository cursoRepository;
    private final SalaRepository salaRepository;
    private final RamoRepository ramoRepository;
    private final ConfiguracionRepository configuracionRepository;

    public DatosIniciales(ProfesorRepository profesorRepository, CursoRepository cursoRepository,
                           SalaRepository salaRepository, RamoRepository ramoRepository,
                           ConfiguracionRepository configuracionRepository) {
        this.profesorRepository = profesorRepository;
        this.cursoRepository = cursoRepository;
        this.salaRepository = salaRepository;
        this.ramoRepository = ramoRepository;
        this.configuracionRepository = configuracionRepository;
    }

    @Override
    public void run(String... args) {
        ConfiguracionColegioDto config = new ConfiguracionColegioDto();
        config.setDias(5);
        config.setBloquesPorDia(8);
        config.setHoraCorteManana("13:00");
        configuracionRepository.guardar(config);

        SalaDto gimnasio = sala("R1", "Gimnasio", "#FF7043");
        SalaDto sala101 = sala("R2", "Sala 101", "#4CAF50");
        SalaDto sala102 = sala("R3", "Sala 102", "#2196F3");
        salaRepository.guardar(gimnasio);
        salaRepository.guardar(sala101);
        salaRepository.guardar(sala102);

        ProfesorDto juan = profesor("P1", "Juan Perez", null, "08:00", "16:00", 30);
        ProfesorDto ana = profesor("P2", "Ana Soto", List.of(new TimeSlotDto(5, 1), new TimeSlotDto(5, 2),
                new TimeSlotDto(5, 3), new TimeSlotDto(5, 4), new TimeSlotDto(5, 5), new TimeSlotDto(5, 6),
                new TimeSlotDto(5, 7), new TimeSlotDto(5, 8)), "08:00", "14:00", 24);
        ProfesorDto luis = profesor("P3", "Luis Rojas", List.of(new TimeSlotDto(1, 1)), "08:00", "16:00", 20);
        profesorRepository.guardar(juan);
        profesorRepository.guardar(ana);
        profesorRepository.guardar(luis);

        CursoDto iiA = curso("C1", "II A", null);
        CursoDto iiB = curso("C2", "II B", "14:00");
        cursoRepository.guardar(iiA);
        cursoRepository.guardar(iiB);

        // profJuan dicta tanto Lenguaje como Orientacion a II A: un mismo profesor puede
        // dictar varios ramos a un mismo curso. La sala NO se fija aqui: el solver la asigna
        // por sesion (ver SesionRamo.sala) entre las salas cargadas arriba.
        ramoRepository.guardar(ramo("R-LEN-C1", "Lenguaje", "C1", "P1", 6, true, null));
        ramoRepository.guardar(ramo("R-MAT-C1", "Matematica", "C1", "P2", 6, true, null));
        ramoRepository.guardar(ramo("R-ORI-C1", "Orientacion", "C1", "P1", 1, false,
                List.of(new TimeSlotDto(4, 1))));
        ramoRepository.guardar(ramo("R-EDF-C1", "Educacion Fisica", "C1", "P3", 2, false, null));

        ramoRepository.guardar(ramo("R-LEN-C2", "Lenguaje", "C2", "P2", 6, true, null));
        ramoRepository.guardar(ramo("R-MAT-C2", "Matematica", "C2", "P1", 6, true, null));
        ramoRepository.guardar(ramo("R-ORI-C2", "Orientacion", "C2", "P2", 1, false,
                List.of(new TimeSlotDto(4, 1))));
        ramoRepository.guardar(ramo("R-EDF-C2", "Educacion Fisica", "C2", "P3", 2, false, null));
    }

    private SalaDto sala(String id, String nombre, String color) {
        SalaDto s = new SalaDto();
        s.setId(id);
        s.setNombre(nombre);
        s.setColor(color);
        return s;
    }

    private ProfesorDto profesor(String id, String nombre, List<TimeSlotDto> noDisponible,
                                  String horaIngreso, String horaSalida, Integer maxHorasSemanales) {
        ProfesorDto p = new ProfesorDto();
        p.setId(id);
        p.setNombre(nombre);
        p.setNoDisponible(noDisponible);
        p.setHoraIngreso(horaIngreso);
        p.setHoraSalida(horaSalida);
        p.setMaxHorasSemanales(maxHorasSemanales);
        return p;
    }

    private CursoDto curso(String id, String nombre, String horaSalidaMaxima) {
        CursoDto c = new CursoDto();
        c.setId(id);
        c.setNombre(nombre);
        c.setHoraSalidaMaxima(horaSalidaMaxima);
        return c;
    }

    private RamoDto ramo(String id, String nombre, String cursoId, String profesorId, int horasSemanales,
                          boolean preferirManana, List<TimeSlotDto> horariosFijos) {
        RamoDto r = new RamoDto();
        r.setId(id);
        r.setNombre(nombre);
        r.setCursoId(cursoId);
        r.setProfesorId(profesorId);
        r.setHorasSemanales(horasSemanales);
        r.setPreferirManana(preferirManana);
        r.setHorariosFijos(horariosFijos);
        return r;
    }
}
