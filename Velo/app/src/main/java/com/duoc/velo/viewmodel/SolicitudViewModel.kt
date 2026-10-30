package com.duoc.velo.viewmodel

import android.database.SQLException
import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.velo.model.Rol
import com.duoc.velo.model.Solicitud
import com.duoc.velo.model.SolicitudConOfertas
import com.duoc.velo.model.SolicitudesDao
import com.duoc.velo.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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

internal class SolicitudViewModel(
    private val dao: SolicitudesDao,
    private val usuario: Usuario,
    private val solicitudId: String?,
    private val estadoGuardado: SavedStateHandle
) : ViewModel() {
    val esProductor = usuario.rol == Rol.PRODUCTOR

    val listado: StateFlow<EstadoListadoSolicitudes> by lazy {
        dao.solicitudes(usuario.rut, esProductor)
            .map { EstadoListadoSolicitudes(solicitudes = it, cargando = false) }
            .catch {
                emit(EstadoListadoSolicitudes(cargando = false, error = "No se pudieron cargar las solicitudes."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoListadoSolicitudes())
    }

    val detalle: StateFlow<EstadoDetalleSolicitud> by lazy {
        dao.solicitud(checkNotNull(solicitudId))
            .map { solicitud ->
                val visible = solicitud?.takeIf {
                    if (esProductor) {
                        it.productor == usuario.rut
                    } else {
                        it.productor != usuario.rut && (it.aceptada == null || it.aceptada == usuario.rut)
                    }
                }
                EstadoDetalleSolicitud(solicitud = visible, cargando = false)
            }
            .catch {
                emit(EstadoDetalleSolicitud(cargando = false, error = "No se pudo cargar la solicitud."))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDetalleSolicitud())
    }

    private val formularioMutable = MutableStateFlow(restaurarFormulario())
    val formulario = formularioMutable.asStateFlow()

    private val publicacionMutable = MutableStateFlow(
        EstadoPublicacionSolicitud(error = estadoGuardado["error_publicacion"])
    )
    val publicacion = publicacionMutable.asStateFlow()

    fun editarFormulario(cambio: FormularioSolicitud.() -> FormularioSolicitud) {
        val estado = publicacion.value
        if (estado.guardando || estado.creada) return

        val formularioEditado = formulario.value.cambio()
        estadoGuardado["formulario_solicitud"] = Bundle().apply {
            putString("producto", formularioEditado.producto)
            putString("superficie", formularioEditado.superficie)
            putString("agua", formularioEditado.agua)
            putString("turno", formularioEditado.turno)
            putStringArrayList("obstaculos", ArrayList(formularioEditado.obstaculos))
            putStringArrayList("fotos", ArrayList(formularioEditado.fotos))
            putString("comentario", formularioEditado.comentario)
        }
        formularioMutable.value = formularioEditado
    }

    fun agregarFoto(uri: String) {
        if (uri in formulario.value.fotos) return
        editarFormulario { copy(fotos = fotos + uri) }
        errorPublicacion(null)
    }

    fun informarErrorFoto() {
        errorPublicacion("Selecciona otra imagen para adjuntarla.")
    }

    fun publicar() {
        val estado = publicacion.value
        if (!esProductor || estado.guardando || estado.creada) return

        val borrador = formulario.value
        val hectareas = borrador.superficie.trim().replace(',', '.').toDoubleOrNull()
            ?.takeIf { it > 0 && it <= 1_000_000.0 }
        if (borrador.producto !in productosSolicitud || hectareas == null) {
            errorPublicacion("Selecciona un producto e ingresa una superficie mayor que 0 y hasta 1.000.000 ha.")
            return
        }

        val id = estadoGuardado.get<String>("id_solicitud_nueva") ?: UUID.randomUUID().toString().also {
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
            fotos = borrador.fotos.joinToString("\n")
        )
        errorPublicacion(null)
        publicacionMutable.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                dao.crear(solicitud)
                publicacionMutable.update { it.copy(creada = true) }
            } catch (_: SQLException) {
                errorPublicacion("No se pudo guardar la solicitud. Vuelve a intentarlo.")
            } finally {
                publicacionMutable.update { it.copy(guardando = false) }
            }
        }
    }

    private fun errorPublicacion(error: String?) {
        estadoGuardado["error_publicacion"] = error
        publicacionMutable.update { it.copy(error = error) }
    }

    private fun restaurarFormulario(): FormularioSolicitud {
        val borrador = estadoGuardado.get<Bundle>("formulario_solicitud") ?: return FormularioSolicitud()
        return FormularioSolicitud(
            producto = borrador.getString("producto").orEmpty(),
            superficie = borrador.getString("superficie").orEmpty(),
            agua = borrador.getString("agua") ?: opcionesAguaSolicitud.first(),
            turno = borrador.getString("turno") ?: turnosSolicitud.first(),
            obstaculos = borrador.getStringArrayList("obstaculos").orEmpty(),
            fotos = borrador.getStringArrayList("fotos").orEmpty(),
            comentario = borrador.getString("comentario").orEmpty()
        )
    }
}

@Suppress("FunctionName")
internal fun SolicitudViewModelFactory(dao: SolicitudesDao, usuario: Usuario, solicitudId: String? = null) =
    viewModelFactory {
        initializer { SolicitudViewModel(dao, usuario, solicitudId, createSavedStateHandle()) }
    }
