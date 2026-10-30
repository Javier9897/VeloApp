package com.duoc.velo.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Usuario::class], version = 2) // se crea la configuracion de la base de datos
abstract class AppDatabase : RoomDatabase() {       // Usuario::class define que tablas va tener
    // Se conecta con usuarioDao y esta devuelve todas las Query en ella
    abstract fun usuarioDao(): UsuarioDao
}