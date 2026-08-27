package cl.colegio.timetabling.repository;

import cl.colegio.timetabling.dto.SalaDto;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SalaRepository extends InMemoryRepository<SalaDto> {
    @Override
    protected String idDe(SalaDto entidad) {
        return entidad.getId();
    }

    @Override
    protected void asignarId(SalaDto entidad, String id) {
        entidad.setId(id);
    }

    public Optional<SalaDto> buscarPorColor(String color) {
        if (color == null) {
            return Optional.empty();
        }
        return datos.values().stream()
                .filter(s -> color.equalsIgnoreCase(s.getColor()))
                .findFirst();
    }
}
