package com.duoc.velo.repository

import android.util.Base64
import com.duoc.velo.model.Rol
import com.duoc.velo.model.Usuario
import com.duoc.velo.model.UsuarioDao
import org.json.JSONObject

internal data class AccesoUsuario(val usuario: Usuario, val token: String? = null)

internal class RegistroDuplicadoException(message: String) : IllegalArgumentException(message)

internal interface UsuarioRepository {
    suspend fun registrarUsuario(usuario: Usuario)
    suspend fun veriflogin(usuario: String, password: String): AccesoUsuario?
    suspend fun actualizarFotoPerfil(rut: String, foto: ByteArray): Int
}

internal class UsuarioRepositoryLocal(private val dao: UsuarioDao) : UsuarioRepository {
    override suspend fun registrarUsuario(usuario: Usuario) {
        val datos = usuario.copy(
            rut = normalizarIdentificador(usuario.rut),
            email = normalizarIdentificador(usuario.email)
        )
        if (dao.obtenerUsuarioPorRut(datos.rut) != null) {
            throw RegistroDuplicadoException("El RUT ingresado ya está registrado")
        }
        if (dao.obtenerUsuarioPorEmail(datos.email) != null) {
            throw RegistroDuplicadoException("El correo ingresado ya está registrado")
        }
        dao.registrarUsuario(datos)
    }

    override suspend fun veriflogin(usuario: String, password: String): AccesoUsuario? =
        dao.veriflogin(normalizarIdentificador(usuario), password)?.let { AccesoUsuario(it.copy(password = "")) }

    override suspend fun actualizarFotoPerfil(rut: String, foto: ByteArray): Int =
        dao.actualizarFotoPerfil(rut, foto)
}

internal class UsuarioRepositoryRemoto(
    private val api: VeloApi,
    private val obtenerToken: () -> String?
) : UsuarioRepository {
    override suspend fun registrarUsuario(usuario: Usuario) {
        val cuerpo = JSONObject().apply {
            put("rut", normalizarIdentificador(usuario.rut))
            put("nombre", usuario.nombre)
            put("apellido", usuario.apellido)
            put("email", normalizarIdentificador(usuario.email))
            put("telefono", usuario.telefono)
            put("password", usuario.password)
            put("rol", usuario.rol.name)
            usuario.fotoPerfil?.let { put("fotoPerfil", Base64.encodeToString(it, Base64.NO_WRAP)) }
        }
        try {
            api.request("POST", "/auth/register", cuerpo)
        } catch (error: VeloApiException) {
            if (error.status == 409) {
                throw RegistroDuplicadoException(error.message ?: "Ya existe una cuenta con estos datos")
            }
            throw error
        }
    }

    override suspend fun veriflogin(usuario: String, password: String): AccesoUsuario? {
        val respuesta = try {
            val cuerpo = JSONObject().put("identificador", normalizarIdentificador(usuario)).put("password", password)
            api.request("POST", "/auth/login", cuerpo) as JSONObject
        } catch (error: VeloApiException) {
            if (error.status == 401) return null
            throw error
        }
        val datos = respuesta.getJSONObject("usuario")
        return AccesoUsuario(
            usuario = Usuario(
                rut = datos.getString("rut"),
                nombre = datos.getString("nombre"),
                apellido = datos.getString("apellido"),
                email = datos.getString("email"),
                telefono = datos.getString("telefono"),
                password = "",
                rol = Rol.valueOf(datos.getString("rol")),
                fotoPerfil = if (datos.isNull("fotoPerfil")) null
                    else Base64.decode(datos.getString("fotoPerfil"), Base64.DEFAULT)
            ),
            token = respuesta.getString("token")
        )
    }

    override suspend fun actualizarFotoPerfil(rut: String, foto: ByteArray): Int {
        val token = obtenerToken() ?: throw VeloApiException(401, "Vuelve a iniciar sesión.")
        val cuerpo = JSONObject().put("fotoPerfil", Base64.encodeToString(foto, Base64.NO_WRAP))
        api.request("POST", "/perfil/foto", cuerpo, token)
        return 1
    }
}

private fun normalizarIdentificador(valor: String): String = when {
    '@' in valor -> valor.trim().lowercase()
    else -> valor.trim().replace(".", "").replace(" ", "").uppercase()
}
