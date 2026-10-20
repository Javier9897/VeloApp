package com.duoc.velo.model

import androidx.room.*

@Dao
interface UsuarioDao {

    // Si en el caso de que el rut ya exista se remplazara los datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarUsuario(usuario: Usuario)

    // Busca al usuario por su email y contraseña para el inicio de sesion
    @Query("SELECT * FROM usuarios WHERE email = :email AND password = :password LIMIT 1")
    suspend fun veriflogin(email: String, password: String): Usuario?

    // Verifica si un rut ya esta registrado
    @Query("SELECT * FROM usuarios WHERE rut = :rut LIMIT 1")
    suspend fun obtenerUsurioPorRut(rut: String): Usuario?

}