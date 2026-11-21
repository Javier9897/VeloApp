package com.duoc.velo

import com.duoc.velo.model.Rol
import com.duoc.velo.model.Usuario
import com.duoc.velo.repository.AccesoUsuario
import com.duoc.velo.repository.UsuarioRepository
import com.duoc.velo.viewmodel.InicioSesionViewModel
import com.duoc.velo.viewmodel.RegistroViewModel
import com.duoc.velo.viewmodel.SesionViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutenticacionViewModelTest {
    @Before
    fun prepararDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun restaurarDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun inicioDeSesionNoConsultaSinCredenciales() = runTest {
        val repositorio = RepositorioUsuariosPrueba()
        val inicio = InicioSesionViewModel(repositorio)

        inicio.iniciarSesion()

        assertEquals(0, repositorio.intentos)
        assertNotNull(inicio.loginError)
        assertFalse(inicio.loginLoading)
    }

    @Test
    fun inicioDeSesionEntregaCuentaUnaVezYLimpiaLaContrasena() = runTest {
        val acceso = AccesoUsuario(usuarioDePrueba(), "token-prueba")
        val repositorio = RepositorioUsuariosPrueba(acceso = acceso)
        val inicio = InicioSesionViewModel(repositorio)
        inicio.loginEmail = "  persona@ejemplo.test  "
        inicio.loginPassword = "password-prueba"

        inicio.iniciarSesion()
        inicio.iniciarSesion()
        assertTrue(inicio.loginLoading)
        advanceUntilIdle()

        assertEquals(1, repositorio.intentos)
        assertEquals("persona@ejemplo.test", repositorio.identificador)
        assertEquals(acceso, inicio.consumirUsuarioVerificado())
        assertNull(inicio.consumirUsuarioVerificado())
        assertEquals("", inicio.loginPassword)
        assertNull(inicio.loginError)
        assertFalse(inicio.loginLoading)
    }

    @Test
    fun credencialesIncorrectasYErrorDeRedDejanReintentar() = runTest {
        val repositorio = RepositorioUsuariosPrueba()
        val inicio = InicioSesionViewModel(repositorio)
        inicio.prepararTrasRegistro("persona@ejemplo.test")
        assertNotNull(inicio.loginMensaje)
        inicio.loginPassword = "password-prueba"

        inicio.iniciarSesion()
        advanceUntilIdle()
        assertEquals("Correo, RUT o contraseña incorrectos", inicio.loginError)

        repositorio.falla = IOException("Servidor desconectado")
        inicio.iniciarSesion()
        advanceUntilIdle()
        assertNotNull(inicio.loginError)
        assertFalse(inicio.loginLoading)
        assertNull(inicio.consumirUsuarioVerificado())

        inicio.limpiarTrasCerrarSesion()
        assertNull(inicio.loginError)
        assertNull(inicio.loginMensaje)
        assertEquals("", inicio.loginPassword)
    }

    @Test
    fun registroRechazaCamposVaciosClaveCortaYConfirmacionDistinta() = runTest {
        val repositorio = RepositorioUsuariosPrueba()
        val registro = RegistroViewModel(repositorio)
        registro.registrarUsuario()
        assertEquals("Tienes que rellenar todos los campos", registro.regError)

        registro.completarCampos()
        registro.regPassword = "corta"
        registro.registrarUsuario()
        assertEquals("La contraseña debe tener al menos 8 caracteres", registro.regError)

        registro.regPassword = "password-prueba"
        registro.regConfirmPassword = "distinta"
        registro.registrarUsuario()
        assertEquals("Las contraseñas no coinciden", registro.regError)
        assertNull(repositorio.cuentaRegistrada)
    }

    @Test
    fun registroExitosoConservaRolYEntregaCorreoUnaVez() = runTest {
        val repositorio = RepositorioUsuariosPrueba()
        val registro = RegistroViewModel(repositorio)
        registro.completarCampos()
        registro.regRol = Rol.APLICADOR

        registro.registrarUsuario()
        assertTrue(registro.regLoading)
        advanceUntilIdle()

        val cuenta = requireNotNull(repositorio.cuentaRegistrada)
        assertEquals(Rol.APLICADOR, cuenta.rol)
        assertEquals("Ana", cuenta.nombre)
        assertEquals("persona@ejemplo.test", registro.consumirRegistroExitoso())
        assertNull(registro.consumirRegistroExitoso())
        assertEquals("", registro.regPassword)
        assertFalse(registro.regLoading)
    }

    @Test
    fun cierreDeSesionYActualizacionDeFotoRespetanCuentaActiva() {
        val sesion = SesionViewModel()
        val cuenta = usuarioDePrueba()
        val foto = byteArrayOf(1, 2, 3)
        sesion.abrirSesion(AccesoUsuario(cuenta, "token-prueba"))

        sesion.aplicarFotoPerfil(cuenta.copy(), foto)
        assertNull(sesion.usuarioAutenticado?.fotoPerfil)

        sesion.aplicarFotoPerfil(cuenta, foto)
        assertArrayEquals(foto, sesion.usuarioAutenticado?.fotoPerfil)
        assertEquals("token-prueba", sesion.token)

        sesion.cerrarSesion()
        assertNull(sesion.usuarioAutenticado)
        assertNull(sesion.token)
    }

    private fun RegistroViewModel.completarCampos() {
        regRut = "12345678-5"
        regNombre = " Ana "
        regApellido = "Pérez"
        regEmail = "persona@ejemplo.test"
        regTelefono = "912345678"
        regPassword = "password-prueba"
        regConfirmPassword = regPassword
    }
}

internal fun usuarioDePrueba(rol: Rol = Rol.PRODUCTOR): Usuario = Usuario(
    rut = "12345678-5",
    nombre = "Ana",
    apellido = "Pérez",
    email = "persona@ejemplo.test",
    telefono = "912345678",
    password = "",
    rol = rol
)

private class RepositorioUsuariosPrueba(var acceso: AccesoUsuario? = null) : UsuarioRepository {
    var intentos = 0
    var identificador: String? = null
    var cuentaRegistrada: Usuario? = null
    var falla: Exception? = null

    override suspend fun registrarUsuario(usuario: Usuario) {
        cuentaRegistrada = usuario
    }

    override suspend fun veriflogin(usuario: String, password: String): AccesoUsuario? {
        intentos++
        identificador = usuario
        falla?.let { throw it }
        return acceso
    }

    override suspend fun actualizarFotoPerfil(rut: String, foto: ByteArray) = 1
}
