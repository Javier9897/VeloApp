package com.duoc.velo.api;

import java.time.Duration;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.duoc.velo.api.Modelos.*;

@Service
class Cuentas {
    private static final RowMapper<Usuario> USUARIO = (filas, numero) -> new Usuario(
            filas.getString("rut"), filas.getString("nombre"), filas.getString("apellido"),
            filas.getString("email"), filas.getString("telefono"), Rol.valueOf(filas.getString("rol")),
            filas.getString("foto_perfil"));
    private final JdbcTemplate sql;
    private final String claveDeComprobacion = CredencialesSeguras.hashClave(CredencialesSeguras.token());

    Cuentas(JdbcTemplate sql) {
        this.sql = sql;
    }

    public Usuario registrar(Registro datos) {
        String rut = Validacion.rut(datos.rut());
        String nombre = Validacion.texto(datos.nombre(), 80, "nombre", true);
        String apellido = Validacion.texto(datos.apellido(), 80, "apellido", true);
        String email = Validacion.email(datos.email());
        String telefono = Validacion.texto(datos.telefono(), 32, "teléfono", true);
        String password = datos.password();
        if (password == null || password.isBlank() || password.length() < 8 || password.length() > 128) {
            throw Fallo.entrada("La contraseña debe tener entre 8 y 128 caracteres.");
        }
        if (datos.rol() == null) throw Fallo.entrada("Selecciona un rol.");
        String foto = Validacion.fotoPerfil(datos.fotoPerfil());
        try {
            sql.update("""
                    INSERT INTO usuarios (rut, nombre, apellido, email, telefono, clave, rol, foto_perfil)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, rut, nombre, apellido, email, telefono, CredencialesSeguras.hashClave(password),
                    datos.rol().name(), foto);
        } catch (DuplicateKeyException error) {
            throw Fallo.conflicto("El RUT o el correo ya están registrados.");
        }
        return new Usuario(rut, nombre, apellido, email, telefono, datos.rol(), foto);
    }

    @Transactional
    public Sesion iniciar(Credenciales datos) {
        if (datos.identificador() == null || datos.identificador().isBlank() || datos.identificador().length() > 254
                || datos.password() == null || datos.password().length() > 128) {
            throw Fallo.autenticacion();
        }
        String identificador = datos.identificador().strip();
        var filas = sql.query("SELECT * FROM usuarios WHERE rut = ? OR email = ?",
                (fila, i) -> new Cuenta(USUARIO.mapRow(fila, i), fila.getString("clave")),
                Validacion.rutNormalizado(identificador), identificador.toLowerCase(Locale.ROOT));
        Cuenta cuenta = filas.isEmpty() ? null : filas.getFirst();
        boolean coincide = CredencialesSeguras.coincide(datos.password(),
                cuenta == null ? claveDeComprobacion : cuenta.clave());
        if (!coincide || cuenta == null) throw Fallo.autenticacion();

        long ahora = System.currentTimeMillis();
        String token = CredencialesSeguras.token();
        sql.update("DELETE FROM sesiones WHERE expira <= ?", ahora);
        sql.update("INSERT INTO sesiones (token_hash, usuario, expira) VALUES (?, ?, ?)",
                CredencialesSeguras.hashToken(token), cuenta.usuario().rut(), ahora + Duration.ofHours(24).toMillis());
        return new Sesion(token, cuenta.usuario());
    }

    public Usuario autenticar(String encabezado) {
        if (encabezado == null || !encabezado.startsWith("Bearer ") || encabezado.length() != 50) {
            throw Fallo.autenticacion();
        }
        String token = encabezado.substring(7);
        var filas = sql.query("""
                SELECT u.rut, u.nombre, u.apellido, u.email, u.telefono, u.rol, NULL AS foto_perfil
                FROM usuarios u JOIN sesiones s ON s.usuario = u.rut
                WHERE s.token_hash = ? AND s.expira > ?
                """, USUARIO, CredencialesSeguras.hashToken(token), System.currentTimeMillis());
        if (filas.isEmpty()) throw Fallo.autenticacion();
        return filas.getFirst();
    }

    public Usuario actualizarFoto(Usuario usuario, FotoPerfil datos) {
        if (datos.fotoPerfil() == null) throw Fallo.entrada("Selecciona una foto de perfil.");
        String foto = Validacion.fotoPerfil(datos.fotoPerfil());
        sql.update("UPDATE usuarios SET foto_perfil = ? WHERE rut = ?", foto, usuario.rut());
        return new Usuario(usuario.rut(), usuario.nombre(), usuario.apellido(), usuario.email(),
                usuario.telefono(), usuario.rol(), foto);
    }
}
