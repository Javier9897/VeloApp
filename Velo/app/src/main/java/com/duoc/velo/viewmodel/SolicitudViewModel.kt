package com.duoc.velo.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.velo.model.Rol
import com.duoc.velo.model.Solicitud
import com.duoc.velo.model.SolicitudConOfertas
import com.duoc.velo.model.Usuario
import com.duoc.velo.repository.MAX_FOTOS_SOLICITUD
import com.duoc.velo.repository.SolicitudesRepository
import com.duoc.velo.repository.mensajeApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

internal val productosSolicitud = listOf("Granulado", "Líquido")
internal val opcionesAguaSolicitud = listOf("Sí", "No")
internal val turnosSolicitud = listOf("Diurna", "Nocturna")
internal val tiposObstaculosSolicitud = listOf("Antenas", "Cables", "Torres de alta tensión", "Otros")

internal data class FormularioSolicitud(
    val producto: String = "",
    val superficie: String = "",
    val agua: String = opcionesAguaSolicitud.first(),
    val turno: String = turnosSolicitud.first(),
    val obstaculos: List<String> = emptyList(),
    val fotos: List<String> = emptyList(),
    val comentario: String = ""
)

internal data class EstadoListadoSolicitudes(
    val solicitudes: List<SolicitudConOfertas> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

internal data class EstadoDetalleSolicitud(
    val solicitud: Solicitud? = null,
    val cargando: Boolean = true,
    val error: String? = null
)

internal data class EstadoPublicacionSolicitud(
    val guardando: Boolean = false,
    val creada: Boolean = false,
    val error: String? = null
)

internal data class EstadoEdicionSolicitud(
    val cargando: Boolean = false,
    val error: String? = null
)

internal data class EstadoEliminacionSolicitud(
    val eliminando: Boolean = false,
    val eliminada: Boolean = false,
    val error: String? = null
)

internal class SolicitudViewModel(
    private val repositorio: SolicitudesRepository,
    private val usuario: Usuario,
    private val solicitudId: String?,
    private val estadoGuardado: SavedStateHandle,
    val modoEdicion: Boolean = false
) : ViewModel() {
    val esProductor = usuario.rol == Rol.PRODUCTOR

    val listado: StateFlow<EstadoListadoSolicitudes> by lazy {
        repositorio.solicitudes(usuario.rut, esProductor)
            .map { resultado ->
                resultado.fold(
                    onSuccess = { EstadoListadoSolicitudes(solicitudes = it, cargando = false) },
                    onFailure = {
                        EstadoListadoSolicitudes(
                            cargando = false,
                            error = it.mensajeApi("No se pudieron cargar las solicitudes.")
                        )
                    }
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoListadoSolicitudes())
    }

    val detalle: StateFlow<EstadoDetalleSolicitud> by lazy {
        repositorio.solicitud(checkNotNull(solicitudId))
            .map { resultado ->
                resultado.fold(
                    onSuccess = { solicitud ->
                        EstadoDetalleSolicitud(solicitud = solicitud?.takeIf(::puedeVer), cargando = false)
                    },
                    onFailure = {
                        EstadoDetalleSolicitud(
                            cargando = false,
                            error = it.mensajeApi("No se pudo cargar la solicitud.")
                        )
                    }
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDetalleSolicitud())
    }

    private val formularioMutable = MutableStateFlow(restaurarFormulario())
    val formulario = formularioMutable.asStateFlow()

    private val publicacionMutable = MutableStateFlow(
        EstadoPublicacionSolicitud(error = estadoGuardado["error_publicacion"])
    )
    val publicacion = publicacionMutable.asStateFlow()

    private val edicionMutable = MutableStateFlow(EstadoEdicionSolicitud(cargando = modoEdicion))
    val edicion = edicionMutable.asStateFlow()

    private val eliminacionMutable = MutableStateFlow(EstadoEliminacionSolicitud())
    val eliminacion = eliminacionMutable.asStateFlow()

    private var solicitudOriginal: Solicitud? = null
    private var cargaEdicion: Job? = null

    init {
        if (modoEdicion) cargarEdicion()
    }

    fun cargarEdicion() {
        val id = solicitudId ?: return
        if (!modoEdicion || cargaEdicion?.isActive == true) return
        edicionMutable.value = EstadoEdicionSolicitud(cargando = true)
        cargaEdicion = viewModelScope.launch {
            try {
                val actual = repositorio.solicitud(id).first().getOrThrow()
                if (actual == null || !esProductor || actual.productor != usuario.rut) {
                    edicionMutable.value = EstadoEdicionSolicitud(error = "Solicitud no disponible para edición.")
                    return@launch
                }
                if (actual.aceptada != null || repositorio.ofertas(id).first().getOrThrow().isNotEmpty()) {
                    edicionMutable.value = EstadoEdicionSolicitud(error = "La solicitud ya tiene ofertas.")
                    return@launch
                }
                solicitudOriginal = actual
                val fotosOriginales = fotosDe(actual)
                val borrador = if (estadoGuardado.get<Boolean>("formulario_iniciado") == true) {
                    val guardado = restaurarFormulario()
                    guardado.copy(fotos = guardado.fotos.mapNotNull { foto ->
                        if (foto.startsWith(PREFIJO_FOTO)) {
                            foto.removePrefix(PREFIJO_FOTO).toIntOrNull()?.let(fotosOriginales::getOrNull)
                        } else {
                            foto
                        }
                    })
                } else {
                    FormularioSolicitud(
                        producto = actual.producto,
                        superficie = actual.hectareas.toBigDecimal().stripTrailingZeros().toPlainString(),
                        agua = if (actual.agua) opcionesAguaSolicitud[0] else opcionesAguaSolicitud[1],
                        turno = actual.turno,
                        obstaculos = actual.obstaculos.split(", ").filter(String::isNotBlank),
                        fotos = fotosOriginales,
                        comentario = actual.comentario
                    )
                }
                guardarFormulario(borrador)
                edicionMutable.value = EstadoEdicionSolicitud()
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                edicionMutable.value = EstadoEdicionSolicitud(
                    error = error.mensajeApi("No se pudo cargar la solicitud para editarla.")
                )
            }
        }
    }

    fun editarFormulario(cambio: FormularioSolicitud.() -> FormularioSolicitud) {
        val estado = publicacion.value
        if (estado.guardando || estado.creada || edicion.value.cargando || edicion.value.error != null) return
        guardarFormulario(formulario.value.cambio())
    }

    fun agregarFoto(uri: String) {
        if (uri in formulario.value.fotos) return
        if (formulario.value.fotos.size >= MAX_FOTOS_SOLICITUD) {
            errorPublicacion("Puedes adjuntar hasta $MAX_FOTOS_SOLICITUD fotos del terreno.")
            return
        }
        editarFormulario { copy(fotos = fotos + uri) }
        errorPublicacion(null)
    }

    fun informarErrorFoto() {
        errorPublicacion("Selecciona otra imagen para adjuntarla.")
    }

    fun publicar() {
        val estado = publicacion.value
        val original = solicitudOriginal
        if (!esProductor || estado.guardando || estado.creada || (modoEdicion && original == null)) return
        val borrador = formulario.value
        val hectareas = borrador.superficie.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it > 0 && it <= 1_000_000.0 }
        if (borrador.producto !in productosSolicitud || hectareas == null) {
            errorPublicacion("Selecciona un producto e ingresa una superficie mayor que 0 y hasta 1.000.000 ha.")
            return
        }

        val id = original?.id ?: estadoGuardado.get<String>("id_solicitud_nueva") ?: UUID.randomUUID().toString().also {
            estadoGuardado["id_solicitud_nueva"] = it
        }
        val solicitud = Solicitud(
            id = id,
            productor = usuario.rut,
            producto = borrador.producto,
            hectareas = hectareas,
            agua = borrador.agua == opcionesAguaSolicitud.first(),
            turno = borrador.turno,
            obstaculos = tiposObstaculosSolicitud.filter { it in borrador.obstaculos }.joinToString(),
            comentario = borrador.comentario.trim(),
            fotos = borrador.fotos.joinToString("\n"),
            creada = original?.creada ?: System.currentTimeMillis()
        )
        errorPublicacion(null)
        publicacionMutable.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                if (modoEdicion) {
                    if (!repositorio.actualizar(solicitud)) {
                        errorPublicacion("La solicitud cambió y ya no se puede editar.")
                        return@launch
                    }
                } else {
                    repositorio.crear(solicitud)
                }
                publicacionMutable.update { it.copy(creada = true) }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                errorPublicacion(error.mensajeApi("No se pudo guardar la solicitud. Vuelve a intentarlo."))
            } finally {
                publicacionMutable.update { it.copy(guardando = false) }
            }
        }
    }

    fun eliminar() {
        val id = solicitudId ?: return
        if (!esProductor || eliminacion.value.eliminando || eliminacion.value.eliminada) return
        eliminacionMutable.value = EstadoEliminacionSolicitud(eliminando = true)
        viewModelScope.launch {
            try {
                eliminacionMutable.value = if (repositorio.eliminar(id, usuario.rut)) {
                    EstadoEliminacionSolicitud(eliminada = true)
                } else {
                    EstadoEliminacionSolicitud(error = "La solicitud cambió y ya no se puede eliminar.")
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                eliminacionMutable.value = EstadoEliminacionSolicitud(
                    error = error.mensajeApi("No se pudo eliminar la solicitud. Vuelve a intentarlo.")
                )
            }
        }
    }

    private fun puedeVer(solicitud: Solicitud): Boolean = if (esProductor) {
        solicitud.productor == usuario.rut
    } else {
        solicitud.productor != usuario.rut && (solicitud.aceptada == null || solicitud.aceptada == usuario.rut)
    }

    private fun errorPublicacion(error: String?) {
        estadoGuardado["error_publicacion"] = error
        publicacionMutable.update { it.copy(error = error) }
    }

    private fun guardarFormulario(borrador: FormularioSolicitud) {
        val fotosOriginales = solicitudOriginal?.let(::fotosDe).orEmpty()
        // Guarda referencias a las fotos cargadas del servidor, fuera del límite de tamaño del estado Android.
        val referencias = borrador.fotos.map { foto ->
            val indiceRemoto = if (foto.startsWith("data:")) fotosOriginales.indexOf(foto) else -1
            if (indiceRemoto >= 0) "$PREFIJO_FOTO$indiceRemoto" else foto
        }
        estadoGuardado["formulario_iniciado"] = true
        estadoGuardado["formulario_producto"] = borrador.producto
        estadoGuardado["formulario_superficie"] = borrador.superficie
        estadoGuardado["formulario_agua"] = borrador.agua
        estadoGuardado["formulario_turno"] = borrador.turno
        estadoGuardado["formulario_obstaculos"] = ArrayList(borrador.obstaculos)
        estadoGuardado["formulario_fotos"] = ArrayList(referencias)
        estadoGuardado["formulario_comentario"] = borrador.comentario
        formularioMutable.value = borrador
    }

    private fun restaurarFormulario(): FormularioSolicitud = FormularioSolicitud(
        producto = estadoGuardado.get<String>("formulario_producto").orEmpty(),
        superficie = estadoGuardado.get<String>("formulario_superficie").orEmpty(),
        agua = estadoGuardado.get<String>("formulario_agua") ?: opcionesAguaSolicitud.first(),
        turno = estadoGuardado.get<String>("formulario_turno") ?: turnosSolicitud.first(),
        obstaculos = estadoGuardado.get<ArrayList<String>>("formulario_obstaculos").orEmpty(),
        fotos = estadoGuardado.get<ArrayList<String>>("formulario_fotos").orEmpty(),
        comentario = estadoGuardado.get<String>("formulario_comentario").orEmpty()
    )

    private fun fotosDe(solicitud: Solicitud): List<String> =
        solicitud.fotos.lineSequence().filter(String::isNotBlank).toList()

    private companion object {
        const val PREFIJO_FOTO = "foto-servidor:"
    }
}

@Suppress("FunctionName")
internal fun SolicitudViewModelFactory(
    repositorio: SolicitudesRepository,
    usuario: Usuario,
    solicitudId: String? = null,
    modoEdicion: Boolean = false
) = viewModelFactory {
    initializer { SolicitudViewModel(repositorio, usuario, solicitudId, createSavedStateHandle(), modoEdicion) }
}
