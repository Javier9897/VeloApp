package com.duoc.velo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.velo.repository.OpenMeteoRepository
import com.duoc.velo.viewmodel.ClimaViewModel
import com.duoc.velo.viewmodel.LecturaClima

private val climaViewModelFactory = viewModelFactory {
    initializer { ClimaViewModel(OpenMeteoRepository()) }
}

@Composable
internal fun ClimaScreen(onVolver: () -> Unit) {
    val modelo: ClimaViewModel = viewModel(factory = climaViewModelFactory)
    val estado by modelo.estado.collectAsStateWithLifecycle()
    val foco = LocalFocusManager.current
    val navegador = LocalUriHandler.current
    val buscar = {
        foco.clearFocus()
        modelo.buscar()
    }

    Pagina(titulo = "Clima del sector", volver = onVolver) {
        OutlinedTextField(
            value = estado.consulta,
            onValueChange = modelo::editarConsulta,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Comuna o ciudad") },
            placeholder = { Text("Ej. Curicó") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (!estado.cargando) buscar() })
        )
        Accion(texto = "Buscar ubicación", enabled = estado.consulta.isNotBlank() && !estado.cargando, click = buscar)

        if (estado.cargando) {
            Row(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Consultando…", style = MaterialTheme.typography.bodyMedium)
            }
        }
        estado.mensaje?.let { mensaje ->
            Text(
                mensaje,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        estado.lectura?.let { lectura ->
            PanelClima(lectura)
            TextButton(onClick = { navegador.openUri("https://open-meteo.com/") }) {
                Text("Datos de Open-Meteo")
            }
        }
        if (estado.lugares.isNotEmpty()) {
            Text("Ubicaciones encontradas", style = MaterialTheme.typography.titleMedium)
            estado.lugares.forEach { lugar ->
                OutlinedButton(
                    onClick = { modelo.consultar(lugar) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(lugar.nombre, style = MaterialTheme.typography.titleSmall)
                            if (lugar.ubicacion.isNotBlank()) {
                                Text(
                                    lugar.ubicacion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
            TextButton(onClick = { navegador.openUri("https://www.geonames.org/") }) {
                Text("Ubicaciones de GeoNames")
            }
        }
    }
}

@Composable
private fun PanelClima(lectura: LecturaClima) {
    val lugar = lectura.lugar
    val clima = lectura.condiciones
    val hora = clima.horaLocal.substringAfter('T').take(5)
    Panel {
        Text(lugar.nombre, style = MaterialTheme.typography.titleLarge)
        if (lugar.ubicacion.isNotBlank()) {
            Text(lugar.ubicacion, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${numero(clima.temperatura)} °C",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineLarge
        )
        Text("Condiciones actuales · $hora hora local", style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        MedicionClima("Viento", "${numero(clima.viento)} km/h")
        MedicionClima("Ráfagas", "${numero(clima.rafagas)} km/h")
        MedicionClima("Precipitación reciente", "${numero(clima.precipitacion)} mm")
    }
}

@Composable
private fun MedicionClima(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(etiqueta, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(valor, style = MaterialTheme.typography.titleSmall)
    }
}
