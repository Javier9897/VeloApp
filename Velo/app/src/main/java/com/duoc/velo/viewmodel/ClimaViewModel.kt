package com.duoc.velo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duoc.velo.model.CondicionesClima
import com.duoc.velo.model.LugarClima
import com.duoc.velo.repository.ClimaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class LecturaClima(val lugar: LugarClima, val condiciones: CondicionesClima)

internal data class EstadoClima(
    val consulta: String = "",
    val lugares: List<LugarClima> = emptyList(),
    val lectura: LecturaClima? = null,
    val cargando: Boolean = false,
    val mensaje: String? = null
)

internal class ClimaViewModel(private val repositorio: ClimaRepository) : ViewModel() {
    private val estadoMutable = MutableStateFlow(EstadoClima())
    val estado = estadoMutable.asStateFlow()
    private var solicitud: Job? = null

    fun editarConsulta(consulta: String) {
        if (consulta == estadoMutable.value.consulta) return
        solicitud?.cancel()
        estadoMutable.value = EstadoClima(consulta = consulta)
    }

    fun buscar() {
        val consulta = estadoMutable.value.consulta.trim()
        val busqueda = EstadoClima(consulta = consulta)
        if (consulta.length < 2) {
            solicitud?.cancel()
            estadoMutable.value = busqueda.copy(mensaje = "Escribe al menos dos caracteres.")
            return
        }

        cargar(busqueda, "No se pudo buscar la ubicación. Intenta de nuevo.") {
            val lugares = repositorio.buscar(consulta)
            busqueda.copy(
                lugares = lugares,
                mensaje = "No encontramos lugares con ese nombre.".takeIf { lugares.isEmpty() }
            )
        }
    }

    fun consultar(lugar: LugarClima) {
        val actual = estadoMutable.value
        if (lugar !in actual.lugares) return
        cargar(actual, "No se pudo consultar el clima. Selecciona la ubicación para reintentar.") {
            actual.copy(
                lectura = LecturaClima(lugar, repositorio.consultar(lugar)),
                cargando = false,
                mensaje = null
            )
        }
    }

    private fun cargar(actual: EstadoClima, error: String, obtener: suspend () -> EstadoClima) {
        solicitud?.cancel()
        estadoMutable.value = actual.copy(cargando = true, lectura = null, mensaje = null)
        solicitud = viewModelScope.launch {
            try {
                val resultado = obtener()
                ensureActive()
                estadoMutable.value = resultado
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                ensureActive()
                estadoMutable.update { it.copy(cargando = false, mensaje = error) }
            }
        }
    }
}
