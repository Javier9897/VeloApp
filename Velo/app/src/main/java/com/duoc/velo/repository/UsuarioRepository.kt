package com.duoc.velo.repository

import com.duoc.velo.model.Usuario
import com.duoc.velo.model.UsuarioDao

class UsuarioRepository (private val dao: UsuarioDao) {

    // Reguistrar usuario a la base de datos
    suspend fun registrarUsuario(usuario: Usuario){
        dao.registrarUsuario(usuario)
    }

    // Verificar credenciales de inicio de sesion
    suspend fun veriflogin(email: String, password: String): Usuario?{
        return dao.veriflogin(email, password)
    }

    // Consultar si un usuario ya existe por su rut
    suspend fun obtenerUsuarioPorRut(rut: String): Usuario?{
        return dao.obtenerUsurioPorRut(rut)
    }
}