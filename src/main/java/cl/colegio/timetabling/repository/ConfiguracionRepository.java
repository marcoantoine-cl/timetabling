package cl.colegio.timetabling.repository;

import cl.colegio.timetabling.dto.ConfiguracionColegioDto;
import org.springframework.stereotype.Repository;

/** Guarda la unica instancia de configuracion del colegio (dias, bloques, corte de manana). */
@Repository
public class ConfiguracionRepository {

    private ConfiguracionColegioDto configuracion = new ConfiguracionColegioDto();

    public ConfiguracionColegioDto obtener() {
        return configuracion;
    }

    public ConfiguracionColegioDto guardar(ConfiguracionColegioDto nueva) {
        this.configuracion = nueva;
        return configuracion;
    }
}
