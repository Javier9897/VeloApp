package com.duoc.velo.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
class Usuario(
    @PrimaryKey
    val rut: String,
    var nombre: String,
    var apellido: String,
    var email: String,
    var telefono: String,
    var password: String
) {
}