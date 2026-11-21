package com.duoc.velo

import com.duoc.velo.model.Rol
import com.duoc.velo.model.Oferta
import com.duoc.velo.model.Solicitud
import com.duoc.velo.repository.RegistroDuplicadoException
import com.duoc.velo.repository.SolicitudesRepositoryRemoto
import com.duoc.velo.repository.UsuarioRepositoryRemoto
import com.duoc.velo.repository.VeloApi
import com.duoc.velo.repository.VeloApiException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ApiVeloTest {
    private val servidor = MockWebServer()
    private lateinit var api: VeloApi

    @Before
    fun iniciarServidor() {
        servidor.start()
        api = VeloApi(servidor.url("/api/v1/").toString())
    }

    @After
    fun detenerServidor() {
        servidor.shutdown()
    }

    @Test
    fun registroYLoginUsanElContratoYConservanElRol() = runBlocking {
        val usuarioJson = """
            {"rut":"12345678-5","nombre":"Ana","apellido":"Pérez","email":"persona@ejemplo.test",
             "telefono":"912345678","rol":"APLICADOR","fotoPerfil":null}
        """.trimIndent()
        responder(201, usuarioJson)
        responder(200, """{"token":"token-prueba","usuario":$usuarioJson}""")

        val repositorio = UsuarioRepositoryRemoto(api) { null }
        val cuenta = usuarioDePrueba(Rol.APLICADOR).copy(rut = "12.345.678-5", email = "PERSONA@EJEMPLO.TEST")
        repositorio.registrarUsuario(cuenta)
        val login = requireNotNull(repositorio.veriflogin(" PERSONA@EJEMPLO.TEST ", "password-prueba"))

        val registro = tomarLlamada()
        assertEquals("POST", registro.metodo)
        assertEquals("/api/v1/auth/register", registro.ruta)
        assertNull(registro.token)
        assertEquals("12345678-5", JSONObject(registro.cuerpo).getString("rut"))
        assertEquals("persona@ejemplo.test", JSONObject(registro.cuerpo).getString("email"))
        assertEquals("APLICADOR", JSONObject(registro.cuerpo).getString("rol"))
        val llamadaLogin = tomarLlamada()
        assertEquals("/api/v1/auth/login", llamadaLogin.ruta)
        assertEquals("persona@ejemplo.test", JSONObject(llamadaLogin.cuerpo).getString("identificador"))
        assertEquals("token-prueba", login.token)
        assertEquals(Rol.APLICADOR, login.usuario.rol)
        assertEquals("", login.usuario.password)
        assertNull(login.usuario.fotoPerfil)
    }

    @Test
    fun repositorioDistingueDuplicadoYCredencialesIncorrectas() = runBlocking {
        responder(409, """{"error":"El correo ya está registrado"}""")
        responder(401, """{"error":"Credenciales inválidas"}""")
        val repositorio = UsuarioRepositoryRemoto(api) { null }

        val duplicado = runCatching { repositorio.registrarUsuario(usuarioDePrueba()) }.exceptionOrNull()
        assertTrue(duplicado is RegistroDuplicadoException)
        assertEquals("El correo ya está registrado", duplicado?.message)
        assertNull(repositorio.veriflogin("12345678-5", "incorrecta"))
    }

    @Test
    fun laActualizacionYEliminacionAutenticadasRespetanMetodoYRespuesta() = runBlocking {
        responder(200, """{"id":"solicitud","hectareas":4}""")
        responder(204)
        responder(403, """{"error":"Solicitud ajena"}""")
        val detalle = api.request("PUT", "/solicitudes/solicitud", JSONObject().put("hectareas", 4), "token-prueba")
        assertEquals(4, (detalle as JSONObject).getInt("hectareas"))
        assertNull(api.request("DELETE", "/solicitudes/solicitud", token = "token-prueba"))
        val error = runCatching { api.request("GET", "/solicitudes/ajena", token = "token-prueba") }.exceptionOrNull()

        assertEquals(403, (error as VeloApiException).status)
        assertEquals("Solicitud ajena", error.message)
        val editar = tomarLlamada()
        assertEquals("PUT", editar.metodo)
        assertEquals("/api/v1/solicitudes/solicitud", editar.ruta)
        assertEquals("Bearer token-prueba", editar.token)
        assertEquals(4, JSONObject(editar.cuerpo).getInt("hectareas"))
        val borrar = tomarLlamada()
        assertEquals("DELETE", borrar.metodo)
        assertEquals("Bearer token-prueba", borrar.token)
        assertEquals("GET", tomarLlamada().metodo)
    }

    @Test
    fun repositorioLeeListadoDetalleYOfertasYTrataEl404ComoAusencia() = runBlocking {
        val solicitud = """{"id":"s1","productor":"12345678-5","producto":"Granulado","hectareas":12.5,
            "agua":true,"turno":"Diurna","obstaculos":"Cables","comentario":"campo","fotos":"","creada":100,"aceptada":null}"""
        responder(200, """[{"solicitud":$solicitud,"cantidadOfertas":2}]""")
        responder(200, solicitud)
        responder(404, """{"error":"Solicitud no disponible"}""")
        responder(200, """[{"solicitud":"s1","aplicador":"22222222-2","nombre":"Alicia","telefono":"912345678","precio":2500}]""")
        responder(200, "{}")
        val repo = SolicitudesRepositoryRemoto(api, { it }) { "token-prueba" }

        val lista = repo.solicitudes("12345678-5", true).first().getOrThrow()
        assertEquals(2, lista.single().cantidadOfertas)
        assertEquals(12.5, lista.single().solicitud.hectareas, 0.0)
        assertEquals(lista.single().solicitud, repo.solicitud("s1").first().getOrThrow())
        assertNull(repo.solicitud("ausente").first().getOrThrow())
        assertEquals(2_500L, repo.ofertas("s1").first().getOrThrow().single().precio)
        assertTrue(repo.solicitudes("12345678-5", true).first().isFailure)
        assertEquals("/api/v1/solicitudes", tomarLlamada().ruta)
        assertEquals("/api/v1/solicitudes/s1", tomarLlamada().ruta)
        assertEquals("/api/v1/solicitudes/ausente", tomarLlamada().ruta)
        assertEquals("/api/v1/solicitudes/s1/ofertas", tomarLlamada().ruta)
    }

    @Test
    fun repositorioEscribeCrudYOfertasConTokenYReportaConflicto() = runBlocking {
        responder(201, "{}")
        responder(200, "{}")
        responder(204)
        responder(201, "{}")
        responder(409, """{"error":"La solicitud cambió"}""")
        responder(200, "{}")
        val foto = "data:image/jpeg;base64,AAAA"
        val repo = SolicitudesRepositoryRemoto(api, { foto }) { "token-prueba" }
        val solicitud = Solicitud(
            id = "s1", productor = "12345678-5", producto = "Granulado", hectareas = 12.5,
            agua = true, turno = "Diurna", obstaculos = "Cables", comentario = "campo", fotos = "content://terreno"
        )

        repo.crear(solicitud)
        assertTrue(repo.actualizar(solicitud.copy(hectareas = 14.0)))
        assertTrue(repo.eliminar("s1", "12345678-5"))
        repo.ofertar(Oferta("s1", "22222222-2", "Alicia", "912345678", 2500))
        assertEquals(false, repo.aceptar("s1", "12345678-5", "22222222-2"))
        assertTrue(repo.aceptar("s1", "12345678-5", "22222222-2"))

        val crear = tomarLlamada()
        assertEquals("Bearer token-prueba", crear.token)
        assertEquals("s1", JSONObject(crear.cuerpo).getString("id"))
        assertEquals(foto, JSONObject(crear.cuerpo).getString("fotos"))
        val editar = tomarLlamada()
        assertEquals("PUT", editar.metodo)
        assertEquals(14.0, JSONObject(editar.cuerpo).getDouble("hectareas"), 0.0)
        assertEquals("DELETE", tomarLlamada().metodo)
        val oferta = tomarLlamada()
        assertEquals("/api/v1/solicitudes/s1/ofertas", oferta.ruta)
        assertEquals(2500, JSONObject(oferta.cuerpo).getInt("precio"))
        val aceptar = tomarLlamada()
        assertEquals("/api/v1/solicitudes/s1/aceptacion", aceptar.ruta)
        assertEquals("22222222-2", JSONObject(aceptar.cuerpo).getString("aplicador"))
    }

    private fun responder(codigo: Int, json: String = "") {
        servidor.enqueue(MockResponse().setResponseCode(codigo)
            .setHeader("Content-Type", "application/json; charset=utf-8").setBody(json))
    }

    private fun tomarLlamada(): Llamada {
        val llamada = requireNotNull(servidor.takeRequest(5, TimeUnit.SECONDS))
        return Llamada(
            requireNotNull(llamada.method), requireNotNull(llamada.requestUrl).encodedPath,
            llamada.getHeader("Authorization"), llamada.body.readUtf8()
        )
    }

    private data class Llamada(val metodo: String, val ruta: String, val token: String?, val cuerpo: String)
}
