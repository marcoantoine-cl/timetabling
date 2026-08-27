package cl.colegio.timetabling.service;

import cl.colegio.timetabling.dto.TimetableRequest;
import cl.colegio.timetabling.repository.ConfiguracionRepository;
import cl.colegio.timetabling.repository.CursoRepository;
import cl.colegio.timetabling.repository.ProfesorRepository;
import cl.colegio.timetabling.repository.RamoRepository;
import cl.colegio.timetabling.repository.SalaRepository;
import org.springframework.stereotype.Component;

/**
 * Arma el TimetableRequest completo a partir de lo cargado via CRUD
 * (profesores, cursos, salas, ramos, configuracion). Permite resolver/verificar
 * sin que el usuario tenga que construir el JSON a mano.
 */
@Component
public class TimetableRequestBuilderService {

    private final ProfesorRepository profesorRepository;
    private final CursoRepository cursoRepository;
    private final SalaRepository salaRepository;
    private final RamoRepository ramoRepository;
    private final ConfiguracionRepository configuracionRepository;

    public TimetableRequestBuilderService(ProfesorRepository profesorRepository, CursoRepository cursoRepository,
                                           SalaRepository salaRepository, RamoRepository ramoRepository,
                                           ConfiguracionRepository configuracionRepository) {
        this.profesorRepository = profesorRepository;
        this.cursoRepository = cursoRepository;
        this.salaRepository = salaRepository;
        this.ramoRepository = ramoRepository;
        this.configuracionRepository = configuracionRepository;
    }

    public TimetableRequest construir() {
        var config = configuracionRepository.obtener();

        TimetableRequest request = new TimetableRequest();
        request.setDias(config.getDias());
        request.setBloquesPorDia(config.getBloquesPorDia());
        request.setBloques(config.getBloques());
        request.setHoraCorteManana(config.getHoraCorteManana());
        request.setProfesores(profesorRepository.listar());
        request.setCursos(cursoRepository.listar());
        request.setSalas(salaRepository.listar());
        request.setRamos(ramoRepository.listar());
        return request;
    }
}
