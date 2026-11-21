package com.duoc.velo.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONTokener
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.Url
import java.io.IOException

internal class VeloApiException(val status: Int, message: String) : IOException(message)

internal class VeloApi(baseUrl: String) {
    private val servicio = Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/').removeSuffix("/api/v1") + "/api/v1/")
        .build()
        .create(ServicioVelo::class.java)

    suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null
    ): Any? {
        require(path.startsWith("/") && !path.startsWith("//"))
        val ruta = path.drop(1)
        val autorizacion = token?.let { "Bearer $it" }
        val respuesta = when (method) {
            "GET" -> servicio.get(ruta, autorizacion)
            "POST" -> servicio.post(ruta, autorizacion, requireNotNull(body).toString().toRequestBody(json))
            "PUT" -> servicio.put(ruta, autorizacion, requireNotNull(body).toString().toRequestBody(json))
            "DELETE" -> servicio.delete(ruta, autorizacion)
            else -> error("Método HTTP no disponible: $method")
        }

        if (!respuesta.isSuccessful) {
            val mensaje = respuesta.errorBody()?.use {
                runCatching { JSONObject(it.string()).optString("error") }.getOrNull()
            }
            throw VeloApiException(
                respuesta.code(),
                mensaje?.takeIf { it.isNotBlank() } ?: "El servidor respondió con un error (${respuesta.code()})."
            )
        }
        return respuesta.body()?.use {
            it.string().takeIf(String::isNotBlank)?.let { texto -> JSONTokener(texto).nextValue() }
        }
    }

    private companion object {
        val json = "application/json; charset=utf-8".toMediaType()
    }
}

internal interface ServicioVelo {
    @GET
    suspend fun get(@Url ruta: String, @Header("Authorization") token: String?): Response<ResponseBody>

    @POST
    suspend fun post(
        @Url ruta: String,
        @Header("Authorization") token: String?,
        @Body datos: RequestBody
    ): Response<ResponseBody>

    @PUT
    suspend fun put(
        @Url ruta: String,
        @Header("Authorization") token: String?,
        @Body datos: RequestBody
    ): Response<ResponseBody>

    @DELETE
    suspend fun delete(@Url ruta: String, @Header("Authorization") token: String?): Response<ResponseBody>
}
