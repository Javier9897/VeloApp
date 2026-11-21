package com.duoc.velo

import com.duoc.velo.model.CondicionesClima
import com.duoc.velo.model.LugarClima
import com.duoc.velo.repository.OpenMeteoRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

class ClimaRepositoryTest {
    private val servidor = MockWebServer()
    private lateinit var repositorio: OpenMeteoRepository
    private val rancagua = LugarClima("Rancagua", "O'Higgins", "Chile", -34.17, -70.74)

    @Before
    fun iniciarServidorDePrueba() {
        servidor.start()
        val url = servidor.url("/").toString()
        repositorio = OpenMeteoRepository(url, url)
    }

    @After
    fun detenerServidorDePrueba() {
        servidor.shutdown()
    }

    @Test
    fun geocodificaConGetConservandoAcentosYOmitiendoCoordenadasInvalidas() = runBlocking {
        responder("""
            {
              "results": [
                {"name":"Rancagua","admin1":"O'Higgins","country":"Chile","latitude":-34.17,"longitude":-70.74},
                {"name":"Coordenada inválida","latitude":-123,"longitude":-70}
              ]
            }
        """.trimIndent())

        assertEquals(listOf(rancagua), repositorio.buscar("Rancagua, región de O'Higgins"))
        assertEquals(
            Peticion("GET", "/v1/search", mapOf(
                "count" to "8",
                "language" to "es",
                "name" to "Rancagua, región de O'Higgins"
            )),
            peticion()
        )
    }

    @Test
    fun unaBusquedaSinCoincidenciasDevuelveUnaListaVacia() = runBlocking {
        responder("""{"generationtime_ms":0.24}""")
        assertTrue(repositorio.buscar("sin coincidencias").isEmpty())
    }

    @Test
    fun elPronosticoConsultaMetricasActualesEnUnidadesEsperadas() = runBlocking {
        responder("""
            {
              "current": {
                "time":"2026-11-21T14:00",
                "temperature_2m":21.4,
                "wind_speed_10m":12.6,
                "wind_gusts_10m":18,
                "precipitation":0.3
              }
            }
        """.trimIndent())

        assertEquals(CondicionesClima("2026-11-21T14:00", 21.4, 12.6, 18.0, 0.3), repositorio.consultar(rancagua))
        assertEquals(
            Peticion("GET", "/v1/forecast", mapOf(
                "current" to "temperature_2m,wind_speed_10m,wind_gusts_10m,precipitation",
                "temperature_unit" to "celsius",
                "wind_speed_unit" to "kmh",
                "precipitation_unit" to "mm",
                "timezone" to "auto",
                "latitude" to "-34.17",
                "longitude" to "-70.74"
            )),
            peticion()
        )
    }

    @Test
    fun siElProveedorOmiteUnaMedicionLaRespuestaNoSePresentaComoClimaValido() {
        responder("""
            {"current":{"time":"2026-11-21T14:00","temperature_2m":21.4,
            "wind_speed_10m":12.6,"wind_gusts_10m":18,"precipitation":null}}
        """.trimIndent())
        assertThrows(IOException::class.java) {
            runBlocking { repositorio.consultar(rancagua) }
        }
    }

    @Test
    fun unJsonInvalidoPropagaElErrorDeConsulta() {
        responder("respuesta no válida")
        assertThrows(JSONException::class.java) {
            runBlocking { repositorio.buscar("Rancagua") }
        }
    }

    @Test
    fun unErrorHttpMantieneSuCodigoParaElConsumidorDelRepositorio() {
        responder("""{"error":true}""", codigo = 503)
        val error = assertThrows(HttpException::class.java) {
            runBlocking { repositorio.consultar(rancagua) }
        }
        assertEquals(503, error.code())
    }

    private fun responder(json: String, codigo: Int = 200) {
        servidor.enqueue(MockResponse().setResponseCode(codigo)
            .setHeader("Content-Type", "application/json; charset=utf-8").setBody(json))
    }

    private fun peticion(): Peticion {
        val peticion = requireNotNull(servidor.takeRequest(5, TimeUnit.SECONDS))
        val url = requireNotNull(peticion.requestUrl)
        return Peticion(
            requireNotNull(peticion.method), url.encodedPath,
            url.queryParameterNames.associateWith { url.queryParameter(it).orEmpty() }
        )
    }

    private data class Peticion(
        val metodo: String,
        val ruta: String,
        val parametros: Map<String, String>
    )
}
