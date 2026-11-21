package com.duoc.velo.model

internal fun validarRegistro(
    rut: String,
    nombre: String,
    apellido: String,
    email: String,
    telefono: String,
    password: String,
    confirmacion: String
): String? = when {
    listOf(rut, nombre, apellido, email, telefono, password, confirmacion).any(String::isBlank) ->
        "Tienes que rellenar todos los campos"
    password.length < 8 -> "La contraseña debe tener al menos 8 caracteres"
    password != confirmacion -> "Las contraseñas no coinciden"
    else -> null
}
