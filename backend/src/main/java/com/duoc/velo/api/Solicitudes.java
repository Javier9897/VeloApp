package com.duoc.velo.api;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.duoc.velo.api.Modelos.*;

@Service
class Solicitudes {
    private static final RowMapper<Solicitud> SOLICITUD = (fila, numero) -> new Solicitud(
            fila.getString("id"), fila.getString("productor"), fila.getString("producto"),
            fila.getDouble("hectareas"), fila.getBoolean("agua"), fila.getString("turno"),
            fila.getString("obstaculos"), fila.getString("comentario"), fila.getString("fotos"),
            fila.getLong("creada"), fila.getString("aceptada"));
    private static final RowMapper<Oferta> OFERTA = (fila, numero) -> new Oferta(
            fila.getString("solicitud"), fila.getString("aplicador"), fila.getString("nombre"),
            fila.getString("telefono"), fila.getLong("precio"));
    private static final String DATOS_OFERTA = """
            SELECT o.solicitud, o.aplicador, (u.nombre || ' ' || u.apellido) AS nombre,
                   u.telefono, o.precio
            FROM ofertas o JOIN usuarios u ON u.rut = o.aplicador
            WHERE o.solicitud = ?
            """;
    private final JdbcTemplate sql;

    Solicitudes(JdbcTemplate sql) {
        this.sql = sql;
    }

    public void comprobarDisponibilidad() {
        sql.queryForObject("SELECT 1", Integer.class);
    }

    public List<SolicitudConOfertas> listar(Usuario usuario) {
        String filtro = usuario.rol() == Rol.PRODUCTOR ? "s.productor = ?" :
                "(s.aceptada IS NULL OR s.aceptada = ?)";
        return sql.query("""
                SELECT s.id, s.productor, s.producto, s.hectareas, s.agua, s.turno, s.obstaculos,
                       s.comentario, '' AS fotos, s.creada, s.aceptada,
                       (SELECT COUNT(*) FROM ofertas o WHERE o.solicitud = s.id) AS cantidad_ofertas
                FROM solicitudes s WHERE """ + " " + filtro + " ORDER BY s.creada DESC, s.id",
                (fila, numero) -> new SolicitudConOfertas(SOLICITUD.mapRow(fila, numero),
                        fila.getInt("cantidad_ofertas")), usuario.rut());
    }

    public Solicitud consultar(Usuario usuario, String id) {
        return visible(usuario, buscar(Validacion.id(id), false));
    }

    @Transactional
    public Solicitud crear(Usuario usuario, DatosSolicitud entrada) {
        exigirRol(usuario, Rol.PRODUCTOR);
        String id = Validacion.id(entrada.id());
        DatosSolicitud datos = Validacion.solicitud(entrada, id);
        long creada = System.currentTimeMillis();
        try {
            sql.update("""
                    INSERT INTO solicitudes (id, productor, producto, hectareas, agua, turno,
                       obstaculos, comentario, fotos, creada)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, id, usuario.rut(), datos.producto(), datos.hectareas(), datos.agua(),
                    datos.turno(), datos.obstaculos(), datos.comentario(), datos.fotos(), creada);
        } catch (DuplicateKeyException error) {
            Solicitud anterior = buscar(id, true);
            if (usuario.rut().equals(anterior.productor()) && datos.equals(datosDe(anterior))) return anterior;
            throw Fallo.conflicto("La solicitud ya existe.");
        }
        return new Solicitud(id, usuario.rut(), datos.producto(), datos.hectareas(), datos.agua(),
                datos.turno(), datos.obstaculos(), datos.comentario(), datos.fotos(), creada, null);
    }

    @Transactional
    public Solicitud editar(Usuario usuario, String id, DatosSolicitud entrada) {
        Solicitud actual = paraModificar(usuario, id);
        DatosSolicitud datos = Validacion.solicitud(entrada, actual.id());
        sql.update("""
                UPDATE solicitudes SET producto = ?, hectareas = ?, agua = ?, turno = ?,
                    obstaculos = ?, comentario = ?, fotos = ?
                WHERE id = ?
                """, datos.producto(), datos.hectareas(), datos.agua(), datos.turno(),
                datos.obstaculos(), datos.comentario(), datos.fotos(), actual.id());
        return buscar(actual.id(), false);
    }

    @Transactional
    public void eliminar(Usuario usuario, String id) {
        Solicitud actual = paraModificar(usuario, id);
        sql.update("DELETE FROM solicitudes WHERE id = ?", actual.id());
    }

    public List<Oferta> ofertas(Usuario usuario, String id) {
        Solicitud solicitud = consultar(usuario, id);
        if (usuario.rol() == Rol.APLICADOR) {
            return sql.query(DATOS_OFERTA + " AND o.aplicador = ?", OFERTA, solicitud.id(), usuario.rut());
        }
        return sql.query(DATOS_OFERTA + " ORDER BY o.precio, nombre, o.aplicador", OFERTA, solicitud.id());
    }

    @Transactional
    public Oferta ofertar(Usuario usuario, String id, NuevaOferta entrada) {
        exigirRol(usuario, Rol.APLICADOR);
        if (entrada.precio() == null || entrada.precio() <= 0 || entrada.precio() > 9_999_999_999L) {
            throw Fallo.entrada("Ingresa un precio válido.");
        }
        Solicitud solicitud = visible(usuario, buscar(Validacion.id(id), true));
        var anteriores = sql.query(DATOS_OFERTA + " AND o.aplicador = ?", OFERTA, id, usuario.rut());
        if (!anteriores.isEmpty()) {
            Oferta anterior = anteriores.getFirst();
            if (anterior.precio() == entrada.precio()) return anterior;
            throw Fallo.conflicto("Ya enviaste una oferta para esta solicitud.");
        }
        if (solicitud.aceptada() != null) throw Fallo.conflicto("La solicitud ya tiene una oferta aceptada.");
        sql.update("INSERT INTO ofertas (solicitud, aplicador, precio) VALUES (?, ?, ?)",
                id, usuario.rut(), entrada.precio());
        return new Oferta(id, usuario.rut(), usuario.nombre() + " " + usuario.apellido(),
                usuario.telefono(), entrada.precio());
    }

    @Transactional
    public Solicitud aceptar(Usuario usuario, String id, Aceptacion entrada) {
        exigirRol(usuario, Rol.PRODUCTOR);
        Solicitud actual = visible(usuario, buscar(Validacion.id(id), true));
        String aplicador = Validacion.rut(entrada.aplicador());
        if (aplicador.equals(actual.aceptada())) return actual;
        if (actual.aceptada() != null) throw Fallo.conflicto("Ya se aceptó otra oferta.");
        boolean existeOferta = Boolean.TRUE.equals(sql.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM ofertas WHERE solicitud = ? AND aplicador = ?)
                """, Boolean.class, actual.id(), aplicador));
        if (!existeOferta) {
            throw new Fallo(org.springframework.http.HttpStatus.NOT_FOUND, "No se encontró la oferta.");
        }
        sql.update("UPDATE solicitudes SET aceptada = ? WHERE id = ?", aplicador, actual.id());
        return buscar(actual.id(), false);
    }

    private Solicitud paraModificar(Usuario usuario, String id) {
        exigirRol(usuario, Rol.PRODUCTOR);
        // Editar, eliminar y ofertar bloquean la misma fila antes de verificar el estado.
        Solicitud actual = visible(usuario, buscar(Validacion.id(id), true));
        boolean tieneOfertas = Boolean.TRUE.equals(sql.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM ofertas WHERE solicitud = ?)", Boolean.class, actual.id()));
        if (actual.aceptada() != null || tieneOfertas) {
            throw Fallo.conflicto("Solo puedes cambiar una solicitud que todavía no tiene ofertas.");
        }
        return actual;
    }

    private Solicitud buscar(String id, boolean bloquear) {
        String consulta = bloquear ? "SELECT * FROM solicitudes WHERE id = ? FOR UPDATE" :
                "SELECT * FROM solicitudes WHERE id = ?";
        var filas = sql.query(consulta, SOLICITUD, id);
        if (filas.isEmpty()) throw Fallo.solicitudNoEncontrada();
        return filas.getFirst();
    }

    private static Solicitud visible(Usuario usuario, Solicitud solicitud) {
        boolean puedeVer = usuario.rol() == Rol.PRODUCTOR ? usuario.rut().equals(solicitud.productor()) :
                solicitud.aceptada() == null || usuario.rut().equals(solicitud.aceptada());
        if (!puedeVer) throw Fallo.solicitudNoEncontrada();
        return solicitud;
    }

    private static void exigirRol(Usuario usuario, Rol rol) {
        if (usuario.rol() != rol) throw Fallo.acceso();
    }

    private static DatosSolicitud datosDe(Solicitud solicitud) {
        return new DatosSolicitud(solicitud.id(), solicitud.producto(), solicitud.hectareas(),
                solicitud.agua(), solicitud.turno(), solicitud.obstaculos(), solicitud.comentario(), solicitud.fotos());
    }
}
