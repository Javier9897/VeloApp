package com.duoc.velo.repository

import com.duoc.velo.model.Oferta
import com.duoc.velo.model.Solicitud
import com.duoc.velo.model.SolicitudConOfertas
import com.duoc.velo.model.SolicitudesDao
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

internal interface SolicitudesRepository {
    fun solicitudes(rut: String, esProductor: Boolean): Flow<Result<List<SolicitudConOfertas>>>
    fun solicitud(id: String): Flow<Result<Solicitud?>>
    fun ofertas(id: String): Flow<Result<List<Oferta>>>
    suspend fun crear(solicitud: Solicitud)
    suspend fun actualizar(solicitud: Solicitud): Boolean
    suspend fun eliminar(id: String, productor: String): Boolean
    suspend fun ofertar(oferta: Oferta)
    suspend fun aceptar(id: String, productor: String, aplicador: String): Boolean
}

internal class SolicitudesRepositoryLocal(private val dao: SolicitudesDao) : SolicitudesRepository {
    override fun solicitudes(rut: String, esProductor: Boolean) = dao.solicitudes(rut, esProductor).comoResultados()
    override fun solicitud(id: String) = dao.solicitud(id).comoResultados()
    override fun ofertas(id: String) = dao.ofertas(id).comoResultados()

    override suspend fun crear(solicitud: Solicitud) = dao.crear(solicitud)
    override suspend fun actualizar(solicitud: Solicitud) =
        dao.actualizar(
            solicitud.id, solicitud.productor, solicitud.producto, solicitud.hectareas,
            solicitud.agua, solicitud.turno, solicitud.obstaculos, solicitud.comentario, solicitud.fotos
        ) != 0

    override suspend fun eliminar(id: String, productor: String) = dao.eliminar(id, productor) != 0

    override suspend fun ofertar(oferta: Oferta) = dao.ofertar(oferta)
    override suspend fun aceptar(id: String, productor: String, aplicador: String) =
        dao.aceptar(id, productor, aplicador) != 0

    private fun <T> Flow<T>.comoResultados(): Flow<Result<T>> =
        map { Result.success(it) }.catch { emit(Result.failure(it)) }
}

internal class SolicitudesRepositoryRemoto(
    private val api: VeloApi,
    private val codificarFotos: suspend (String) -> String,
    private val proveedorToken: () -> String?
) : SolicitudesRepository {
    private val cambios = MutableStateFlow(0L)

    override fun solicitudes(rut: String, esProductor: Boolean) = observar {
        listaJson(api.request("GET", "/solicitudes", token = tokenActual())) { fila ->
            SolicitudConOfertas(
                solicitud = parseSolicitud(fila.getJSONObject("solicitud")),
                cantidadOfertas = fila.getInt("cantidadOfertas")
            )
        }
    }

    override fun solicitud(id: String): Flow<Result<Solicitud?>> = observar(intervalo = 60_000) {
        try {
            parseSolicitud(objetoJson(api.request("GET", rutaSolicitud(id), token = tokenActual())))
        } catch (error: VeloApiException) {
            if (error.status == 404) null else throw error
        }
    }

    override fun ofertas(id: String) = observar {
        val respuesta = api.request("GET", "${rutaSolicitud(id)}/ofertas", token = tokenActual())
        listaJson(respuesta) { oferta ->
            Oferta(
                solicitud = oferta.getString("solicitud"),
                aplicador = oferta.getString("aplicador"),
                nombre = oferta.getString("nombre"),
                telefono = oferta.getString("telefono"),
                precio = oferta.getLong("precio")
            )
        }
    }

    override suspend fun crear(solicitud: Solicitud) {
        val token = tokenActual()
        val datos = cuerpoSolicitud(solicitud).put("id", solicitud.id)
        api.request("POST", "/solicitudes", datos, token)
        invalidarConsultas()
    }

    override suspend fun actualizar(solicitud: Solicitud): Boolean {
        val token = tokenActual()
        api.request("PUT", rutaSolicitud(solicitud.id), cuerpoSolicitud(solicitud), token)
        invalidarConsultas()
        return true
    }

    override suspend fun eliminar(id: String, productor: String): Boolean {
        api.request("DELETE", rutaSolicitud(id), token = tokenActual())
        invalidarConsultas()
        return true
    }

    private suspend fun cuerpoSolicitud(solicitud: Solicitud): JSONObject =
        JSONObject()
            .put("producto", solicitud.producto)
            .put("hectareas", solicitud.hectareas)
            .put("agua", solicitud.agua)
            .put("turno", solicitud.turno)
            .put("obstaculos", solicitud.obstaculos)
            .put("comentario", solicitud.comentario)
            .put("fotos", codificarFotos(solicitud.fotos))

    override suspend fun ofertar(oferta: Oferta) {
        api.request(
            "POST",
            "${rutaSolicitud(oferta.solicitud)}/ofertas",
            JSONObject().put("precio", oferta.precio),
            tokenActual()
        )
        invalidarConsultas()
    }

    override suspend fun aceptar(id: String, productor: String, aplicador: String): Boolean {
        try {
            api.request(
                "POST",
                "${rutaSolicitud(id)}/aceptacion",
                JSONObject().put("aplicador", aplicador),
                tokenActual()
            )
        } catch (error: VeloApiException) {
            if (error.status != 409) throw error
            invalidarConsultas()
            return false
        }
        invalidarConsultas()
        return true
    }

    private fun <T> observar(intervalo: Long = 25_000, cargar: suspend () -> T): Flow<Result<T>> {
        val refresco = flow {
            while (true) {
                delay(intervalo)
                emit(Unit)
            }
        }
        return merge(cambios.map { Unit }, refresco).map {
            try {
                Result.success(cargar())
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Result.failure(error)
            }
        }
    }

    private fun invalidarConsultas() {
        cambios.update { it + 1 }
    }

    private fun tokenActual(): String = proveedorToken()?.takeIf { it.isNotBlank() }
        ?: throw VeloApiException(401, "Tu sesión no está disponible. Vuelve a iniciar sesión.")

    private fun rutaSolicitud(id: String) =
        "/solicitudes/${URLEncoder.encode(id, Charsets.UTF_8.name()).replace("+", "%20")}"

    private fun parseSolicitud(json: JSONObject) = Solicitud(
        id = json.getString("id"),
        productor = json.getString("productor"),
        producto = json.getString("producto"),
        hectareas = json.getDouble("hectareas"),
        agua = json.getBoolean("agua"),
        turno = json.getString("turno"),
        obstaculos = json.getString("obstaculos"),
        comentario = json.getString("comentario"),
        fotos = json.optString("fotos"),
        creada = json.getLong("creada"),
        aceptada = if (json.isNull("aceptada")) null else json.getString("aceptada")
    )

    private fun objetoJson(respuesta: Any?): JSONObject = respuesta as? JSONObject
        ?: throw IOException("La API no devolvió el objeto esperado.")

    private fun <T> listaJson(respuesta: Any?, transformar: (JSONObject) -> T): List<T> {
        val lista = respuesta as? JSONArray ?: throw IOException("La API no devolvió la lista esperada.")
        return List(lista.length()) { indice -> transformar(lista.getJSONObject(indice)) }
    }
}

internal fun Throwable.mensajeApi(alternativo: String): String = when (this) {
    is VeloApiException -> if (status == 401) {
        "Tu sesión no está disponible. Vuelve a iniciar sesión."
    } else {
        message ?: alternativo
    }
    is FotoTerrenoException -> message ?: alternativo
    else -> alternativo
}
