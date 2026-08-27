package cl.colegio.timetabling.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositorio en memoria generico. Sirve mientras la persistencia sea "todo en memoria"
 * (como se definio al inicio del proyecto); si mas adelante se agrega una base de datos,
 * este es el unico lugar a reemplazar por JPA/JDBC sin tocar los controllers.
 */
public abstract class InMemoryRepository<T> {

    protected final Map<String, T> datos = new ConcurrentHashMap<>();

    protected abstract String idDe(T entidad);

    protected abstract void asignarId(T entidad, String id);

    public List<T> listar() {
        return new ArrayList<>(datos.values());
    }

    public Optional<T> buscar(String id) {
        return Optional.ofNullable(datos.get(id));
    }

    /** Crea (si no trae id) o reemplaza (si el id ya existe) la entidad. */
    public T guardar(T entidad) {
        String id = idDe(entidad);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            asignarId(entidad, id);
        }
        datos.put(id, entidad);
        return entidad;
    }

    public boolean eliminar(String id) {
        return datos.remove(id) != null;
    }

    public boolean existe(String id) {
        return datos.containsKey(id);
    }
}
