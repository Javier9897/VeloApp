package com.duoc.velo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.velo.R
import com.duoc.velo.model.Solicitud
import com.duoc.velo.viewmodel.SolicitudViewModel

@Composable
private fun EstadoSolicitud(estado: String) {
    val (fondo, texto) = when (estado) {
        "Pendiente" -> Color(0xFFFBF2DD) to Color(0xFF74510D)
        "Finalizada" -> Color(0xFFECF2F5) to Color(0xFF345067)
        else -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(shape = RoundedCornerShape(20.dp), color = fondo, contentColor = texto) {
        Text(estado, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
internal fun Resumen(solicitud: Solicitud, estado: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (solicitud.producto == "Granulado") {
            Icon(Icons.Outlined.Grain, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        } else {
            Simbolo(R.drawable.velo_liquido, 22)
        }
        Text(solicitud.producto, Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleMedium)
        EstadoSolicitud(estado)
    }
    HorizontalDivider()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Superficie", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${numero(solicitud.hectareas)} ha", style = MaterialTheme.typography.bodyMedium)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Aplicación", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Simbolo(if (solicitud.turno == "Diurna") R.drawable.velo_diurna else R.drawable.velo_nocturna, 16)
                Text(solicitud.turno, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun Listado(
    viewModel: SolicitudViewModel,
    perfil: () -> Unit,
    nueva: () -> Unit,
    abrir: (String) -> Unit
) {
    val productor = viewModel.esProductor
    val listado by viewModel.listado.collectAsStateWithLifecycle()
    val pie: (@Composable () -> Unit)? = if (productor) {
        { Accion("+ Nueva solicitud", click = nueva) }
    } else {
        null
    }
    Pagina(
        titulo = if (productor) "Solicitudes públicas" else "Solicitudes cerca de ti",
        acciones = {
            IconButton(onClick = perfil) { Icon(Icons.Outlined.PersonOutline, "Mi perfil") }
        },
        pie = pie
    ) {
        if (productor) {
            Text("Consulta las necesidades de aplicación en el campo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val estado = listado
        if (estado.cargando) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Pagina
        }
        if (estado.error != null) {
            Text(estado.error, color = MaterialTheme.colorScheme.error)
            return@Pagina
        }
        val visibles = estado.solicitudes
        val conteo = if (visibles.size == 1) "1 solicitud" else "${visibles.size} solicitudes"
        Text(conteo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (visibles.isEmpty()) {
            Panel {
                Text(if (productor) "Todavía no has publicado solicitudes." else "Todavía no hay solicitudes disponibles.")
            }
        }
        visibles.forEach { item ->
            val solicitud = item.solicitud
            key(solicitud.id) {
                val estadoSolicitud = if (productor || solicitud.aceptada != null) {
                    solicitud.estado(item.cantidadOfertas)
                } else {
                    "Sin ubicación"
                }
                Column(Modifier.fillMaxWidth().clickable(onClickLabel = "Ver detalle de solicitud") { abrir(solicitud.id) }) {
                    Panel { Resumen(solicitud, estadoSolicitud) }
                }
            }
        }
    }
}
