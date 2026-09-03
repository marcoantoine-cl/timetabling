package cl.colegio.timetabling.service;

import cl.colegio.timetabling.dto.*;
import cl.colegio.timetabling.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Precarga los repositorios CRUD al arrancar, para que /api/timetable/actual(/solve)
 * y las paginas CRUD del frontend ("Cargar desde CRUD") tengan algo que mostrar de
 * inmediato — y para que sea el MISMO dataset a escala real que ya se valido con
 * /dataset-ajustado(/solve), en vez de un dataset chico distinto. Asi cualquier
 * ajuste que se le haga a DatasetAjustadoGenerator (parametros, correcciones como la
 * de Orientacion) se refleja automaticamente tanto en /dataset-ajustado como en el
 * CRUD/frontend, sin mantener dos datasets separados.
 *
 * En produccion esto se reemplaza por los datos reales cargados por el usuario (o
 * una migracion desde el sistema anterior) — este precargado es solo para que el
 * sistema no arranque vacio durante desarrollo/pruebas.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final ProfesorRepository profesorRepository;
    private final CursoRepository cursoRepository;
    private final SalaRepository salaRepository;
    private final RamoRepository ramoRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final DatasetAjustadoGenerator datasetAjustadoGenerator;

    public DatosIniciales(ProfesorRepository profesorRepository, CursoRepository cursoRepository,
                           SalaRepository salaRepository, RamoRepository ramoRepository,
                           ConfiguracionRepository configuracionRepository,
                           DatasetAjustadoGenerator datasetAjustadoGenerator) {
        this.profesorRepository = profesorRepository;
        this.cursoRepository = cursoRepository;
        this.salaRepository = salaRepository;
        this.ramoRepository = ramoRepository;
        this.configuracionRepository = configuracionRepository;
        this.datasetAjustadoGenerator = datasetAjustadoGenerator;
    }

    @Override
    public void run(String... args) {
        TimetableRequest dataset = datasetAjustadoGenerator.generar();

        ConfiguracionColegioDto config = new ConfiguracionColegioDto();
        config.setDias(dataset.getDias());
        config.setBloquesPorDia(dataset.getBloquesPorDia());
        config.setBloques(dataset.getBloques());
        config.setHoraCorteManana(dataset.getHoraCorteManana());
        configuracionRepository.guardar(config);

        dataset.getSalas().forEach(salaRepository::guardar);
        dataset.getProfesores().forEach(profesorRepository::guardar);
        dataset.getCursos().forEach(cursoRepository::guardar);
        dataset.getRamos().forEach(ramoRepository::guardar);
    }
}
