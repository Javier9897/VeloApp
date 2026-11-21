package com.duoc.velo

import androidx.lifecycle.SavedStateHandle
import com.duoc.velo.model.Oferta
import com.duoc.velo.model.Rol
import com.duoc.velo.model.Solicitud
import com.duoc.velo.model.SolicitudConOfertas
import com.duoc.velo.repository.SolicitudesRepository
import com.duoc.velo.viewmodel.OfertaViewModel
import com.duoc.velo.viewmodel.SolicitudViewModel
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SolicitudesViewModelTest {
    @Before
    fun prepararDispatcher() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun restaurarDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun publicacionSeValidaYConservaDatosDelProductor() = runTest {
        val repositorio = RepositorioSolicitudesPrueba()
        val vm = SolicitudViewModel(repositorio, usuarioDePrueba(), null, SavedStateHandle())

        vm.publicar()
        assertNotNull(vm.publicacion.value.error)
        assertNull(repositorio.creada)

        vm.editarFormulario { copy(producto = "Granulado", superficie = "0", comentario = " terreno ") }
        vm.publicar()
        assertNull(repositorio.creada)

        vm.editarFormulario {
            copy(superficie = "12,5", agua = "No", turno = "Nocturna", obstaculos = listOf("Cables"))
        }
        vm.publicar()
        vm.publicar()
        assertTrue(vm.publicacion.value.guardando)
        advanceUntilIdle()

        val publicada = requireNotNull(repositorio.creada)
        assertEquals(1, repositorio.intentosPublicar)
        assertEquals("12345678-5", publicada.productor)
        assertEquals(12.5, publicada.hectareas, 0.0)
        assertEquals("Nocturna", publicada.turno)
        assertEquals("Cables", publicada.obstaculos)
        assertEquals("terreno", publicada.comentario)
        assertFalse(publicada.agua)
        assertTrue(vm.publicacion.value.creada)
    }

    @Test
    fun estadoGuardadoRecuperaBorradorYUnReintentoUsaLaMismaSolicitud() = runTest {
        val repositorio = RepositorioSolicitudesPrueba()
        val guardado = SavedStateHandle()
        val primeraPantalla = SolicitudViewModel(repositorio, usuarioDePrueba(), null, guardado)
        primeraPantalla.editarFormulario { copy(producto = "Líquido", superficie = "4") }
        primeraPantalla.agregarFoto("content://test/imagen")
        primeraPantalla.agregarFoto("content://test/imagen")

        val recuperada = SolicitudViewModel(repositorio, usuarioDePrueba(), null, guardado)
        assertEquals("4", recuperada.formulario.value.superficie)
        assertEquals(listOf("content://test/imagen"), recuperada.formulario.value.fotos)
        repositorio.errorCreacion = IOException("Conexión interrumpida")
        recuperada.publicar()
        advanceUntilIdle()
        val primerId = repositorio.creada?.id
        assertNotNull(primerId)
        assertNotNull(recuperada.publicacion.value.error)
        assertFalse(recuperada.publicacion.value.creada)

        repositorio.errorCreacion = null
        recuperada.publicar()
        advanceUntilIdle()
        assertEquals(primerId, repositorio.creada?.id)
        assertTrue(recuperada.publicacion.value.creada)
        assertNull(recuperada.publicacion.value.error)
    }

    @Test
    fun listadoEntregaResultadosYExponeErrorDelRepositorio() = runTest {
        val repositorio = RepositorioSolicitudesPrueba()
        val vm = SolicitudViewModel(repositorio, usuarioDePrueba(), null, SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.listado.collect {} }
        val fila = SolicitudConOfertas(solicitudDePrueba(), 1)
        repositorio.lista.value = Result.success(listOf(fila))
        runCurrent()

        assertFalse(vm.listado.value.cargando)
        assertEquals(listOf(fila), vm.listado.value.solicitudes)

        repositorio.lista.value = Result.failure(IOException("Sin conexión"))
        runCurrent()
        assertNotNull(vm.listado.value.error)
    }

    @Test
    fun detalleFiltraSolicitudesAjenasYPermitePropia() = runTest {
        val repositorio = RepositorioSolicitudesPrueba()
        val vm = SolicitudViewModel(repositorio, usuarioDePrueba(), "solicitud", SavedStateHandle())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.detalle.collect {} }

        repositorio.detalle.value = Result.success(solicitudDePrueba(productor = "ajeno"))
        runCurrent()
        assertNull(vm.detalle.value.solicitud)
        assertFalse(vm.detalle.value.cargando)

        val propia = solicitudDePrueba()
        repositorio.detalle.value = Result.success(propia)
        runCurrent()
        assertEquals(propia, vm.detalle.value.solicitud)
    }

    @Test
    fun seEditaUnaSolicitudPropiaSinPerderIdFechaNiFotos() = runTest {
        val repositorio = RepositorioSolicitudesPrueba()
        val foto = "data:image/jpeg;base64,AAAA"
        val original = solicitudDePrueba().copy(fotos = foto, creada = 100, agua = false)
        repositorio.detalle.value = Result.success(original)
        val guardado = SavedStateHandle()
        val vm = SolicitudViewModel(repositorio, usuarioDePrueba(), original.id, guardado, modoEdicion = true)
        assertTrue(vm.edicion.value.cargando)
        advanceUntilIdle()

        assertFalse(vm.edicion.value.cargando)
        assertEquals("3", vm.formulario.value.superficie)
        assertEquals("No", vm.formulario.value.agua)
        assertEquals(listOf(foto), vm.formulario.value.fotos)
        assertEquals(listOf("foto-servidor:0"), guardado.get<ArrayList<String>>("formulario_fotos"))
        vm.editarFormulario { copy(superficie = "8,5") }

        val recuperada = SolicitudViewModel(repositorio, usuarioDePrueba(), original.id, guardado, modoEdicion = true)
        advanceUntilIdle()
        assertEquals("8,5", recuperada.formulario.value.superficie)
        assertEquals(listOf(foto), recuperada.formulario.value.fotos)
        recuperada.publicar()
        advanceUntilIdle()

        val editada = requireNotNull(repositorio.editada)
        assertEquals(original.id, editada.id)
        assertEquals(original.creada, editada.creada)
        assertEquals(foto, editada.fotos)
        assertEquals(8.5, editada.hectareas, 0.0)
        assertTrue(recuperada.publicacion.value.creada)
        assertNull(repositorio.creada)
    }

    @Test
    fun conflictoDeEdicionConservaBorradorYUnaOfertaImpideReabrirlo() = runTest {
        val repositorio = RepositorioSolicitudesPrueba()
        val original = solicitudDePrueba()
        repositorio.detalle.value = Result.success(original)
        val vm = SolicitudViewModel(repositorio, usuarioDePrueba(), original.id, SavedStateHandle(), modoEdicion = true)
        advanceUntilIdle()
        repositorio.permiteEditar = false
        vm.editarFormulario { copy(comentario = "nuevo") }
        vm.publicar()
        advanceUntilIdle()

        assertNotNull(vm.publicacion.value.error)
        assertEquals("nuevo", vm.formulario.value.comentario)
        assertFalse(vm.publicacion.value.creada)

        repositorio.listaOfertas.value = Result.success(listOf(Oferta(original.id, "aplicador", "Elena", "912345678", 2_000)))
        val bloqueada = SolicitudViewModel(repositorio, usuarioDePrueba(), original.id, SavedStateHandle(), modoEdicion = true)
        advanceUntilIdle()
        assertEquals("La solicitud ya tiene ofertas.", bloqueada.edicion.value.error)
        bloqueada.editarFormulario { copy(comentario = "no editable") }
        assertEquals("", bloqueada.formulario.value.comentario)
    }

    @Test
    fun eliminacionRespetaRolYConfirmaElResultadoAntesDeCerrar() = runTest {
        val repositorio = RepositorioSolicitudesPrueba()
        val aplicador = SolicitudViewModel(repositorio, usuarioDePrueba(Rol.APLICADOR), "solicitud", SavedStateHandle())
        aplicador.eliminar()
        advanceUntilIdle()
        assertEquals(0, repositorio.intentosEliminar)
        val productor = SolicitudViewModel(repositorio, usuarioDePrueba(), "solicitud", SavedStateHandle())

        repositorio.permiteEliminar = false
        productor.eliminar()
        assertTrue(productor.eliminacion.value.eliminando)
        advanceUntilIdle()
        assertFalse(productor.eliminacion.value.eliminada)
        assertNotNull(productor.eliminacion.value.error)

        repositorio.permiteEliminar = true
        productor.eliminar()
        productor.eliminar()
        advanceUntilIdle()
        assertEquals(2, repositorio.intentosEliminar)
        assertEquals("solicitud", repositorio.eliminada)
        assertTrue(productor.eliminacion.value.eliminada)
        assertNull(productor.eliminacion.value.error)
    }

    @Test
    fun ofertasBloqueanRolIncorrectoImporteInvalidoYEnvioDuplicado() = runTest {
        val repositorio = RepositorioSolicitudesPrueba()
        val vm = OfertaViewModel(repositorio, "solicitud")
        val productor = usuarioDePrueba()
        val aplicador = usuarioDePrueba(Rol.APLICADOR).copy(rut = "22222222-2")
        vm.enviar(productor, 1_000)
        vm.enviar(aplicador, 0)
        assertNull(repositorio.oferta)

        vm.enviar(aplicador, 1_000)
        vm.enviar(aplicador, 2_000)
        assertTrue(vm.operacion.value.enviando)
        advanceUntilIdle()
        assertEquals(1_000L, repositorio.oferta?.precio)
        assertEquals("22222222-2", repositorio.oferta?.aplicador)
        assertFalse(vm.operacion.value.enviando)

        val oferta = requireNotNull(repositorio.oferta)
        vm.aceptar(aplicador, oferta)
        vm.aceptar(productor, oferta.copy(solicitud = "otra"))
        assertNull(repositorio.aplicadorAceptado)

        repositorio.permiteAceptar = false
        vm.aceptar(productor, oferta)
        advanceUntilIdle()
        assertNotNull(vm.operacion.value.mensaje)

        repositorio.permiteAceptar = true
        vm.aceptar(productor, oferta)
        advanceUntilIdle()
        assertEquals(oferta.aplicador, repositorio.aplicadorAceptado)
        assertNull(vm.operacion.value.mensaje)
    }

    private fun solicitudDePrueba(productor: String = "12345678-5") = Solicitud(
        id = "solicitud", productor = productor, producto = "Granulado",
        hectareas = 3.0, agua = true, turno = "Diurna", obstaculos = "",
        comentario = "", fotos = ""
    )
}

private class RepositorioSolicitudesPrueba : SolicitudesRepository {
    val lista = MutableStateFlow(Result.success(emptyList<SolicitudConOfertas>()))
    val detalle = MutableStateFlow<Result<Solicitud?>>(Result.success(null))
    val listaOfertas = MutableStateFlow(Result.success(emptyList<Oferta>()))
    var creada: Solicitud? = null
    var errorCreacion: IOException? = null
    var intentosPublicar = 0
    var editada: Solicitud? = null
    var permiteEditar = true
    var eliminada: String? = null
    var permiteEliminar = true
    var intentosEliminar = 0
    var oferta: Oferta? = null
    var permiteAceptar = true
    var aplicadorAceptado: String? = null

    override fun solicitudes(rut: String, esProductor: Boolean) = lista
    override fun solicitud(id: String) = detalle
    override fun ofertas(id: String) = listaOfertas

    override suspend fun crear(solicitud: Solicitud) {
        intentosPublicar++
        creada = solicitud
        errorCreacion?.let { throw it }
    }

    override suspend fun actualizar(solicitud: Solicitud): Boolean {
        if (permiteEditar) editada = solicitud
        return permiteEditar
    }

    override suspend fun eliminar(id: String, productor: String): Boolean {
        intentosEliminar++
        if (permiteEliminar) eliminada = id
        return permiteEliminar
    }

    override suspend fun ofertar(oferta: Oferta) {
        this.oferta = oferta
        listaOfertas.value = Result.success(listOf(oferta))
    }

    override suspend fun aceptar(id: String, productor: String, aplicador: String): Boolean {
        if (permiteAceptar) aplicadorAceptado = aplicador
        return permiteAceptar
    }
}
