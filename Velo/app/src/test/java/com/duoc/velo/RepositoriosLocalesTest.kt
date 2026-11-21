package com.duoc.velo

import com.duoc.velo.model.Oferta
import com.duoc.velo.model.Solicitud
import com.duoc.velo.model.SolicitudConOfertas
import com.duoc.velo.model.SolicitudesDao
import com.duoc.velo.model.Usuario
import com.duoc.velo.model.UsuarioDao
import com.duoc.velo.repository.RegistroDuplicadoException
import com.duoc.velo.repository.SolicitudesRepositoryLocal
import com.duoc.velo.repository.UsuarioRepositoryLocal
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoriosLocalesTest {
    @Test
    fun cuentasLocalesNormalizanIdentificadoresYOmitenLaClaveEnLaSesion() = runBlocking {
        val repo = UsuarioRepositoryLocal(UsuariosDaoDePrueba())
        val usuario = usuarioDePrueba().copy(
            rut = "12.345.678-5", email = " PERSONA@EJEMPLO.TEST ", password = "clave-de-prueba"
        )

        repo.registrarUsuario(usuario)
        assertNull(repo.veriflogin(usuario.email, "clave-incorrecta"))
        val acceso = requireNotNull(repo.veriflogin("12.345.678-5", usuario.password))
        assertEquals("12345678-5", acceso.usuario.rut)
        assertEquals("persona@ejemplo.test", acceso.usuario.email)
        assertEquals("", acceso.usuario.password)
        assertNull(acceso.token)
        assertEquals(1, repo.actualizarFotoPerfil(acceso.usuario.rut, byteArrayOf(1, 2)))
        assertArrayEquals(byteArrayOf(1, 2), repo.veriflogin(usuario.email, usuario.password)?.usuario?.fotoPerfil)
        assertEquals(0, repo.actualizarFotoPerfil("otra", byteArrayOf(3)))
    }

    @Test
    fun cuentasLocalesDetectanDuplicadosDeRutYCorreoAntesDeGuardar() = runBlocking {
        val dao = UsuariosDaoDePrueba()
        val repo = UsuarioRepositoryLocal(dao)
        val usuario = usuarioDePrueba()
        repo.registrarUsuario(usuario)

        val rut = runCatching { repo.registrarUsuario(usuario.copy(email = "otro@ejemplo.test")) }.exceptionOrNull()
        val correo = runCatching { repo.registrarUsuario(usuario.copy(rut = "11111111-1")) }.exceptionOrNull()
        assertTrue(rut is RegistroDuplicadoException)
        assertEquals("El RUT ingresado ya está registrado", rut?.message)
        assertTrue(correo is RegistroDuplicadoException)
        assertEquals("El correo ingresado ya está registrado", correo?.message)
        assertEquals(1, dao.escrituras)
    }

    @Test
    fun solicitudesLocalesConviertenConsultasYFilasAfectadasEnResultados() = runBlocking {
        val dao = SolicitudesDaoDePrueba()
        val repo = SolicitudesRepositoryLocal(dao)
        val solicitud = Solicitud(
            id = "s1", productor = "12345678-5", producto = "Granulado", hectareas = 8.0,
            agua = true, turno = "Diurna", obstaculos = "Cables", comentario = "", fotos = ""
        )

        repo.crear(solicitud)
        assertEquals(solicitud, repo.solicitud("s1").first().getOrThrow())
        assertEquals(listOf(SolicitudConOfertas(solicitud, 0)), repo.solicitudes(solicitud.productor, true).first().getOrThrow())
        assertTrue(repo.actualizar(solicitud.copy(hectareas = 10.0)))
        assertEquals(10.0, dao.actual?.hectareas ?: 0.0, 0.0)

        val oferta = Oferta("s1", "22222222-2", "Sofía", "912345678", 2500)
        assertTrue(runCatching { repo.ofertar(oferta.copy(aplicador = solicitud.productor)) }.isFailure)
        repo.ofertar(oferta)
        assertEquals(listOf(oferta), repo.ofertas("s1").first().getOrThrow())
        assertTrue(repo.aceptar("s1", solicitud.productor, oferta.aplicador))

        dao.filasAfectadas = 0
        assertFalse(repo.eliminar("s1", solicitud.productor))
        assertFalse(repo.aceptar("s1", solicitud.productor, oferta.aplicador))
        dao.filasAfectadas = 1
        assertTrue(repo.eliminar("s1", solicitud.productor))
        assertNull(repo.solicitud("s1").first().getOrThrow())
        dao.errorLectura = IOException("Error de almacenamiento")
        assertEquals(dao.errorLectura, repo.solicitudes(solicitud.productor, true).first().exceptionOrNull())
    }
}

private class UsuariosDaoDePrueba : UsuarioDao {
    private val cuentas = mutableMapOf<String, Usuario>()
    var escrituras = 0
        private set

    override suspend fun registrarUsuario(usuario: Usuario) {
        cuentas[usuario.rut] = usuario
        escrituras++
    }

    override suspend fun veriflogin(usuario: String, password: String) =
        cuentas.values.find { (it.rut == usuario || it.email == usuario) && it.password == password }

    override suspend fun obtenerUsuarioPorRut(rut: String) = cuentas[rut]
    override suspend fun obtenerUsuarioPorEmail(email: String) = cuentas.values.find { it.email == email }

    override suspend fun actualizarFotoPerfil(rut: String, foto: ByteArray): Int {
        val cuenta = cuentas[rut] ?: return 0
        cuentas[rut] = cuenta.copy(fotoPerfil = foto)
        return 1
    }
}

private class SolicitudesDaoDePrueba : SolicitudesDao() {
    var actual: Solicitud? = null
        private set
    private var oferta: Oferta? = null
    var filasAfectadas = 1
    var errorLectura: IOException? = null

    override fun solicitudes(rut: String, esProductor: Boolean) = flow {
        errorLectura?.let { throw it }
        emit(listOfNotNull(actual?.let { SolicitudConOfertas(it, if (oferta == null) 0 else 1) }))
    }

    override fun solicitud(id: String) = flowOf(actual?.takeIf { it.id == id })
    override fun ofertas(id: String) = flowOf(listOfNotNull(oferta?.takeIf { it.solicitud == id }))

    override suspend fun crear(solicitud: Solicitud) {
        actual = solicitud
    }

    override suspend fun actualizar(
        id: String, productor: String, producto: String, hectareas: Double,
        agua: Boolean, turno: String, obstaculos: String, comentario: String, fotos: String
    ): Int {
        if (filasAfectadas != 0) {
            actual = Solicitud(id, productor, producto, hectareas, agua, turno, obstaculos, comentario, fotos)
        }
        return filasAfectadas
    }

    override suspend fun eliminar(id: String, productor: String): Int {
        if (filasAfectadas != 0) actual = null
        return filasAfectadas
    }

    override suspend fun buscar(id: String) = actual?.takeIf { it.id == id }

    override suspend fun insertar(oferta: Oferta) {
        this.oferta = oferta
    }

    override suspend fun aceptar(id: String, productor: String, aplicador: String) = filasAfectadas
}
