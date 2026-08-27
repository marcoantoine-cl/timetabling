package cl.colegio.timetabling.repository;

import cl.colegio.timetabling.dto.ProfesorDto;
import org.springframework.stereotype.Repository;

@Repository
public class ProfesorRepository extends InMemoryRepository<ProfesorDto> {
    @Override
    protected String idDe(ProfesorDto entidad) {
        return entidad.getId();
    }

    @Override
    protected void asignarId(ProfesorDto entidad, String id) {
        entidad.setId(id);
    }
}
