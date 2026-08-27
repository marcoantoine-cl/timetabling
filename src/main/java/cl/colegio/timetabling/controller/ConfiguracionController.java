package cl.colegio.timetabling.controller;

import cl.colegio.timetabling.dto.ConfiguracionColegioDto;
import cl.colegio.timetabling.repository.ConfiguracionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = {"http://localhost:4200"})
public class ConfiguracionController {

    private final ConfiguracionRepository repositorio;

    public ConfiguracionController(ConfiguracionRepository repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public ConfiguracionColegioDto obtener() {
        return repositorio.obtener();
    }

    @PutMapping
    public ConfiguracionColegioDto actualizar(@RequestBody ConfiguracionColegioDto configuracion) {
        if (configuracion.getDias() <= 0 || configuracion.getBloquesPorDia() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dias y bloquesPorDia deben ser mayores a 0");
        }
        return repositorio.guardar(configuracion);
    }
}
