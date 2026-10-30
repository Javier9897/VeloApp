package com.duoc.velo.repository

import com.duoc.velo.model.Usuario
import com.duoc.velo.model.UsuarioDao

class UsuarioRepository (private val dao: UsuarioDao) {

    // Deja el rut siempre igual: "12.345.678-k" -> "12345678-K"
    private fun normalizarRut(rut: String): String =
        rut.replace(".", "").replace(" ", "").uppercase()

    // Registrar usuario a la base de datos (rut sin puntos y email en minusculas)
    suspend fun registrarUsuario(usuario: Usuario){
        dao.registrarUsuario(
            usuario.copy(
                rut = normalizarRut(usuario.rut),
                email = usuario.email.lowercase()
            )
        )
    }

    // Verificar credenciales de inicio de sesion (el usuario puede ser email o rut)
    suspend fun veriflogin(usuario: String, password: String): Usuario?{
        val identificador = if ("@" in usuario) usuario.lowercase() else normalizarRut(usuario)
        return dao.veriflogin(identificador, password)
    }

    // Consultar si un usuario ya existe por su rut
    suspend fun obtenerUsuarioPorRut(rut: String): Usuario?{
        return dao.obtenerUsuarioPorRut(normalizarRut(rut))
    }

    // Consultar si un usuario ya existe por su email
    suspend fun obtenerUsuarioPorEmail(email: String): Usuario?{
        return dao.obtenerUsuarioPorEmail(email.lowercase())
    }

    suspend fun actualizarFotoPerfil(rut: String, foto: ByteArray): Int =
        dao.actualizarFotoPerfil(rut, foto)
}
