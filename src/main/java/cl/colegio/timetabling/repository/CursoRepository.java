package cl.colegio.timetabling.repository;

import cl.colegio.timetabling.dto.CursoDto;
import org.springframework.stereotype.Repository;

@Repository
public class CursoRepository extends InMemoryRepository<CursoDto> {
    @Override
    protected String idDe(CursoDto entidad) {
        return entidad.getId();
    }

    @Override
    protected void asignarId(CursoDto entidad, String id) {
        entidad.setId(id);
    }
}
