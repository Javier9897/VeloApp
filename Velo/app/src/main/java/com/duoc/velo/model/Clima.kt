package com.duoc.velo.model

internal data class LugarClima(
    val nombre: String,
    val region: String,
    val pais: String,
    val latitud: Double,
    val longitud: Double
) {
    val ubicacion: String
        get() = listOf(region, pais).filter { it.isNotBlank() && it != nombre }.distinct().joinToString(", ")
}

internal data class CondicionesClima(
    val horaLocal: String,
    val temperatura: Double,
    val viento: Double,
    val rafagas: Double,
    val precipitacion: Double
)
