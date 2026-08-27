package cl.colegio.timetabling.controller;

import cl.colegio.timetabling.dto.ProfesorDto;
import cl.colegio.timetabling.repository.ProfesorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/profesores")
@CrossOrigin(origins = {"http://localhost:4200"})
public class ProfesorController {

    private final ProfesorRepository repositorio;

    public ProfesorController(ProfesorRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public List<ProfesorDto> listar() {
        return repositorio.listar();
    }

    @GetMapping("/{id}")
    public ProfesorDto obtener(@PathVariable String id) {
        return repositorio.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profesor no encontrado: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfesorDto crear(@RequestBody ProfesorDto profesor) {
        if (profesor.getId() != null && repositorio.existe(profesor.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un profesor con id " + profesor.getId());
        }
        validar(profesor);
        return repositorio.guardar(profesor);
    }

    @PutMapping("/{id}")
    public ProfesorDto actualizar(@PathVariable String id, @RequestBody ProfesorDto profesor) {
        if (!repositorio.existe(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profesor no encontrado: " + id);
        }
        profesor.setId(id);
        validar(profesor);
        return repositorio.guardar(profesor);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        if (!repositorio.eliminar(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Profesor no encontrado: " + id);
        }
        // Nota: no se valida en cascada que ningun ramo siga referenciando este profesor;
        // eso se detecta recien al construir el horario (POST /solve, /verificar, etc.)
        // con un mensaje 400 claro senalando el ramo huerfano.
    }

    private void validar(ProfesorDto p) {
        if (p.getNombre() == null || p.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El profesor debe tener nombre");
        }
    }
}
