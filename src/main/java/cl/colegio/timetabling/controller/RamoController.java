package cl.colegio.timetabling.controller;

import cl.colegio.timetabling.dto.RamoDto;
import cl.colegio.timetabling.repository.CursoRepository;
import cl.colegio.timetabling.repository.ProfesorRepository;
import cl.colegio.timetabling.repository.RamoRepository;
import cl.colegio.timetabling.repository.SalaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/ramos")
@CrossOrigin(origins = {"http://localhost:4200"})
public class RamoController {

    private final RamoRepository repositorio;
    private final CursoRepository cursoRepository;
    private final ProfesorRepository profesorRepository;
    private final SalaRepository salaRepository;

    public RamoController(RamoRepository repositorio, CursoRepository cursoRepository,
                           ProfesorRepository profesorRepository, SalaRepository salaRepository) {
        this.repositorio = repositorio;
        this.cursoRepository = cursoRepository;
        this.profesorRepository = profesorRepository;
        this.salaRepository = salaRepository;
    }

    @GetMapping
    public List<RamoDto> listar() {
        return repositorio.listar();
    }

    @GetMapping("/{id}")
    public RamoDto obtener(@PathVariable String id) {
        return repositorio.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ramo no encontrado: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RamoDto crear(@RequestBody RamoDto ramo) {
        if (ramo.getId() != null && repositorio.existe(ramo.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un ramo con id " + ramo.getId());
        }
        validar(ramo);
        return repositorio.guardar(ramo);
    }

    @PutMapping("/{id}")
    public RamoDto actualizar(@PathVariable String id, @RequestBody RamoDto ramo) {
        if (!repositorio.existe(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ramo no encontrado: " + id);
        }
        ramo.setId(id);
        validar(ramo);
        return repositorio.guardar(ramo);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        if (!repositorio.eliminar(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ramo no encontrado: " + id);
        }
    }

    private void validar(RamoDto r) {
        if (r.getNombre() == null || r.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ramo debe tener nombre");
        }
        if (r.getHorasSemanales() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horasSemanales debe ser mayor a 0");
        }
        if (r.getCursoId() == null || !cursoRepository.existe(r.getCursoId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cursoId invalido o inexistente: " + r.getCursoId());
        }
        if (r.getProfesorId() == null || !profesorRepository.existe(r.getProfesorId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profesorId invalido o inexistente: " + r.getProfesorId());
        }
        if (r.getSalaId() != null && !salaRepository.existe(r.getSalaId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "salaId inexistente: " + r.getSalaId());
        }
    }
}
