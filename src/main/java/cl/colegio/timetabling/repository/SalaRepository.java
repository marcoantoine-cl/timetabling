package cl.colegio.timetabling.repository;

import cl.colegio.timetabling.dto.SalaDto;
import org.springframework.stereotype.Repository;

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
}
