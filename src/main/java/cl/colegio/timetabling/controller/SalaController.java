package cl.colegio.timetabling.controller;

import cl.colegio.timetabling.dto.SalaDto;
import cl.colegio.timetabling.repository.SalaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/salas")
@CrossOrigin(origins = {"http://localhost:4200"})
public class SalaController {

    private final SalaRepository repositorio;

    public SalaController(SalaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public List<SalaDto> listar() {
        return repositorio.listar();
    }

    @GetMapping("/{id}")
    public SalaDto obtener(@PathVariable String id) {
        return repositorio.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sala no encontrada: " + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalaDto crear(@RequestBody SalaDto sala) {
        if (sala.getId() != null && repositorio.existe(sala.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una sala con id " + sala.getId());
        }
        validar(sala);
        return repositorio.guardar(sala);
    }

    @PutMapping("/{id}")
    public SalaDto actualizar(@PathVariable String id, @RequestBody SalaDto sala) {
        if (!repositorio.existe(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sala no encontrada: " + id);
        }
        sala.setId(id);
        validar(sala);
        return repositorio.guardar(sala);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        if (!repositorio.eliminar(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sala no encontrada: " + id);
        }
    }

    private void validar(SalaDto s) {
        if (s.getNombre() == null || s.getNombre().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sala debe tener nombre");
        }
    }
}
