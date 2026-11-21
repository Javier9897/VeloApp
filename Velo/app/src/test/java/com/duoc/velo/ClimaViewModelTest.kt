package com.duoc.velo

import com.duoc.velo.model.CondicionesClima
import com.duoc.velo.model.LugarClima
import com.duoc.velo.repository.ClimaRepository
import com.duoc.velo.viewmodel.ClimaViewModel
import com.duoc.velo.viewmodel.LecturaClima
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ClimaViewModelTest {
    private val curico = LugarClima("Curicó", "Maule", "Chile", -34.98, -71.23)
    private val talca = LugarClima("Talca", "Maule", "Chile", -35.43, -71.66)
    private val clima = CondicionesClima("2026-11-21T14:00", 21.5, 13.6, 18.0, 0.0)

    @Test
    fun unaConsultaMuyCortaNoLlamaAlProveedor() = prueba {
        val repositorio = RepositorioClimaDePrueba()
        val modelo = ClimaViewModel(repositorio)

        modelo.editarConsulta("  C  ")
        modelo.buscar()
        advanceUntilIdle()

        assertTrue(repositorio.consultas.isEmpty())
        assertFalse(modelo.estado.value.cargando)
        assertNotNull(modelo.estado.value.mensaje)
    }

    @Test
    fun buscaPorNombreYLuegoConsultaLasCondicionesDelLugarElegido() = prueba {
        val repositorio = RepositorioClimaDePrueba(
            buscarLugar = { listOf(curico, talca) },
            consultarLugar = { clima }
        )
        val modelo = ClimaViewModel(repositorio)

        modelo.editarConsulta("  Curicó  ")
        modelo.buscar()
        assertTrue(modelo.estado.value.cargando)
        advanceUntilIdle()

        assertEquals(listOf("Curicó"), repositorio.consultas)
        assertEquals(listOf(curico, talca), modelo.estado.value.lugares)
        assertNull(modelo.estado.value.lectura)

        modelo.consultar(talca)
        assertTrue(modelo.estado.value.cargando)
        advanceUntilIdle()

        assertEquals(listOf(talca), repositorio.lugaresConsultados)
        assertEquals(LecturaClima(talca, clima), modelo.estado.value.lectura)
        assertFalse(modelo.estado.value.cargando)
        assertNull(modelo.estado.value.mensaje)
    }

    @Test
    fun informaCuandoLaBusquedaNoTieneResultados() = prueba {
        val modelo = ClimaViewModel(RepositorioClimaDePrueba())

        modelo.editarConsulta("Comuna inexistente")
        modelo.buscar()
        advanceUntilIdle()

        assertTrue(modelo.estado.value.lugares.isEmpty())
        assertNull(modelo.estado.value.lectura)
        assertNotNull(modelo.estado.value.mensaje)
        assertFalse(modelo.estado.value.cargando)
    }

    @Test
    fun unErrorDeBusquedaPermiteReintentar() = prueba {
        var intentos = 0
        val modelo = ClimaViewModel(RepositorioClimaDePrueba(buscarLugar = {
            intentos++
            if (intentos == 1) throw IOException("Sin conexión")
            listOf(curico)
        }))

        modelo.editarConsulta("Curicó")
        modelo.buscar()
        advanceUntilIdle()
        assertNotNull(modelo.estado.value.mensaje)
        assertFalse(modelo.estado.value.cargando)

        modelo.buscar()
        advanceUntilIdle()
        assertEquals(listOf(curico), modelo.estado.value.lugares)
        assertNull(modelo.estado.value.mensaje)
    }

    @Test
    fun siFallaElPronosticoConservaLosLugaresParaReintentar() = prueba {
        var intentos = 0
        val modelo = ClimaViewModel(RepositorioClimaDePrueba(
            buscarLugar = { listOf(curico) },
            consultarLugar = {
                intentos++
                if (intentos == 1) throw IOException("Sin conexión")
                clima
            }
        ))
        modelo.editarConsulta("Curicó")
        modelo.buscar()
        advanceUntilIdle()

        modelo.consultar(curico)
        advanceUntilIdle()
        assertEquals(listOf(curico), modelo.estado.value.lugares)
        assertNotNull(modelo.estado.value.mensaje)
        assertNull(modelo.estado.value.lectura)

        modelo.consultar(curico)
        advanceUntilIdle()
        assertEquals(LecturaClima(curico, clima), modelo.estado.value.lectura)
        assertNull(modelo.estado.value.mensaje)
    }

    @Test
    fun cambiarLaConsultaCancelaUnaBusquedaAnterior() = prueba {
        val primeraRespuesta = CompletableDeferred<List<LugarClima>>()
        val repositorio = RepositorioClimaDePrueba(buscarLugar = { consulta ->
            if (consulta == "Curicó") primeraRespuesta.await() else listOf(talca)
        })
        val modelo = ClimaViewModel(repositorio)

        modelo.editarConsulta("Curicó")
        modelo.buscar()
        advanceUntilIdle()
        assertTrue(modelo.estado.value.cargando)

        modelo.editarConsulta("Talca")
        modelo.buscar()
        advanceUntilIdle()
        primeraRespuesta.complete(listOf(curico))
        advanceUntilIdle()

        assertEquals(listOf("Curicó", "Talca"), repositorio.consultas)
        assertEquals("Talca", modelo.estado.value.consulta)
        assertEquals(listOf(talca), modelo.estado.value.lugares)
        assertFalse(modelo.estado.value.cargando)
    }

    private fun prueba(caso: suspend TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            caso()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class RepositorioClimaDePrueba(
        private val buscarLugar: suspend (String) -> List<LugarClima> = { emptyList() },
        private val consultarLugar: suspend (LugarClima) -> CondicionesClima = {
            error("La prueba no debería consultar el clima.")
        }
    ) : ClimaRepository {
        val consultas = mutableListOf<String>()
        val lugaresConsultados = mutableListOf<LugarClima>()

        override suspend fun buscar(consulta: String): List<LugarClima> {
            consultas.add(consulta)
            return buscarLugar(consulta)
        }

        override suspend fun consultar(lugar: LugarClima): CondicionesClima {
            lugaresConsultados.add(lugar)
            return consultarLugar(lugar)
        }
    }
}
