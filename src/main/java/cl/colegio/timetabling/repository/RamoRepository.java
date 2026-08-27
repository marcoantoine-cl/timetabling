package cl.colegio.timetabling.repository;

import cl.colegio.timetabling.dto.RamoDto;
import org.springframework.stereotype.Repository;

@Repository
public class RamoRepository extends InMemoryRepository<RamoDto> {
    @Override
    protected String idDe(RamoDto entidad) {
        return entidad.getId();
    }

    @Override
    protected void asignarId(RamoDto entidad, String id) {
        entidad.setId(id);
    }
}
