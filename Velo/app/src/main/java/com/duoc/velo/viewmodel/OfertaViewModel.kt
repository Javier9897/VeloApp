package com.duoc.velo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.velo.model.Oferta
import com.duoc.velo.model.Rol
import com.duoc.velo.model.SolicitudesDao
import com.duoc.velo.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class EstadoOfertas(
    val ofertas: List<Oferta>? = null,
    val error: String? = null
)

internal data class EstadoOperacionOferta(
    val enviando: Boolean = false,
    val mensaje: String? = null
)

internal class OfertaViewModel(
    private val dao: SolicitudesDao,
    private val solicitudId: String
) : ViewModel() {
    val estado: StateFlow<EstadoOfertas> = dao.ofertas(solicitudId)
        .map { EstadoOfertas(ofertas = it) }
        .catch { emit(EstadoOfertas(error = "No se pudieron cargar las ofertas.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoOfertas())

    private val operacionMutable = MutableStateFlow(EstadoOperacionOferta())
    val operacion = operacionMutable.asStateFlow()

    fun enviar(usuario: Usuario, precio: Long) {
        if (usuario.rol != Rol.APLICADOR || precio !in 1L..9_999_999_999L) return
        ejecutar("No se pudo enviar la oferta. Vuelve a intentarlo.") {
            try {
                dao.ofertar(
                    Oferta(
                        solicitud = solicitudId,
                        aplicador = usuario.rut,
                        nombre = "${usuario.nombre} ${usuario.apellido}",
                        telefono = usuario.telefono,
                        precio = precio
                    )
                )
                null
            } catch (_: IllegalArgumentException) {
                "La solicitud ya no recibe ofertas."
            }
        }
    }

    fun aceptar(usuario: Usuario, oferta: Oferta) {
        if (usuario.rol != Rol.PRODUCTOR || oferta.solicitud != solicitudId) return
        ejecutar("No se pudo aceptar la oferta. Vuelve a intentarlo.") {
            if (dao.aceptar(solicitudId, usuario.rut, oferta.aplicador) == 0) {
                "La solicitud ya cambió de estado."
            } else {
                null
            }
        }
    }

    private fun ejecutar(error: String, accion: suspend () -> String?) {
        val anterior = operacionMutable.value
        if (anterior.enviando || !operacionMutable.compareAndSet(anterior, EstadoOperacionOferta(enviando = true))) return

        viewModelScope.launch {
            var mensaje: String? = null
            try {
                mensaje = accion()
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                mensaje = error
            } finally {
                operacionMutable.value = EstadoOperacionOferta(mensaje = mensaje)
            }
        }
    }
}

internal class OfertaViewModelFactory(
    private val dao: SolicitudesDao,
    private val solicitudId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == OfertaViewModel::class.java)
        return OfertaViewModel(dao, solicitudId) as T
    }
}
