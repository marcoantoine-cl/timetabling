package cl.colegio.timetabling.controller;

import cl.colegio.timetabling.dto.CursoDto;
import cl.colegio.timetabling.repository.CursoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/cursos")
@CrossOrigin(origins = {"http://localhost:4200"})
public class CursoController {

    private final CursoRepository repositorio;

    public CursoController(CursoRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public List<CursoDto> listar() {
        return repositorio.listar();
    }

    @GetMapping("/{id}")
    public CursoDto obtener(@PathVariable String id) {
        return repositorio.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoDto crear(@RequestBody CursoDto curso) {
        if (curso.getId() != null && repositorio.existe(curso.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un curso con id " + curso.getId());
        }
        validar(curso);
        return repositorio.guardar(curso);
    }

    @PutMapping("/{id}")
    public CursoDto actualizar(@PathVariable String id, @RequestBody CursoDto curso) {
        if (!repositorio.existe(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado: " + id);
        }
        curso.setId(id);
        validar(curso);
        return repositorio.guardar(curso);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        if (!repositorio.eliminar(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado: " + id);
        }
    }

    private void validar(CursoDto c) {
        if (c.getNombre() == null || c.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El curso debe tener nombre");
        }
    }
}
