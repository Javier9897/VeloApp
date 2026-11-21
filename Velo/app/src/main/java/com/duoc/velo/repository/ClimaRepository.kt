package com.duoc.velo.repository

import com.duoc.velo.model.CondicionesClima
import com.duoc.velo.model.LugarClima
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

internal interface ClimaRepository {
    suspend fun buscar(consulta: String): List<LugarClima>
    suspend fun consultar(lugar: LugarClima): CondicionesClima
}

private interface GeocodificacionApi {
    @GET("v1/search?count=8&language=es")
    suspend fun buscar(@Query("name") consulta: String): ResponseBody
}

private interface PronosticoApi {
    @GET(
        "v1/forecast?current=temperature_2m,wind_speed_10m,wind_gusts_10m,precipitation" +
            "&temperature_unit=celsius&wind_speed_unit=kmh&precipitation_unit=mm&timezone=auto"
    )
    suspend fun consultar(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double
    ): ResponseBody
}

internal class OpenMeteoRepository(
    urlGeocodificacion: String = "https://geocoding-api.open-meteo.com/",
    urlPronostico: String = "https://api.open-meteo.com/"
) : ClimaRepository {
    private val cliente = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
    private val geocodificacion by lazy {
        retrofit(urlGeocodificacion).create(GeocodificacionApi::class.java)
    }
    private val pronostico by lazy {
        retrofit(urlPronostico).create(PronosticoApi::class.java)
    }

    override suspend fun buscar(consulta: String): List<LugarClima> = withContext(Dispatchers.IO) {
        val datos = geocodificacion.buscar(consulta).use { JSONObject(it.string()) }
        val resultados = datos.optJSONArray("results") ?: return@withContext emptyList()
        (0 until resultados.length()).mapNotNull { indice ->
            resultados.optJSONObject(indice)?.let(::leerLugar)
        }
    }

    override suspend fun consultar(lugar: LugarClima): CondicionesClima = withContext(Dispatchers.IO) {
        val datos = pronostico.consultar(lugar.latitud, lugar.longitud).use { JSONObject(it.string()) }
        val actual = datos.getJSONObject("current")
        CondicionesClima(
            horaLocal = actual.getString("time"),
            temperatura = actual.numero("temperature_2m"),
            viento = actual.numero("wind_speed_10m"),
            rafagas = actual.numero("wind_gusts_10m"),
            precipitacion = actual.numero("precipitation")
        )
    }

    private fun leerLugar(datos: JSONObject): LugarClima? {
        val nombre = datos.optString("name").trim()
        val latitud = datos.optDouble("latitude")
        val longitud = datos.optDouble("longitude")
        if (nombre.isBlank() || latitud !in -90.0..90.0 || longitud !in -180.0..180.0) return null
        return LugarClima(
            nombre = nombre,
            region = datos.optString("admin1"),
            pais = datos.optString("country"),
            latitud = latitud,
            longitud = longitud
        )
    }

    private fun JSONObject.numero(campo: String): Double =
        optDouble(campo).takeIf { it.isFinite() }
            ?: throw IOException("El proveedor no devolvió la medición solicitada.")

    private fun retrofit(url: String): Retrofit =
        Retrofit.Builder().baseUrl(url).client(cliente).build()
}
