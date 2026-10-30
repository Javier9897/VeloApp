package com.duoc.velo.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.duoc.velo.model.Oferta
import com.duoc.velo.model.Rol
import com.duoc.velo.model.Solicitud
import com.duoc.velo.model.Usuario
import com.duoc.velo.viewmodel.OfertaViewModel
import com.duoc.velo.viewmodel.SolicitudViewModel
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

internal fun numero(n: Number): String = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CL")).format(n)

private fun pesos(n: Long): String = "${'$'}${numero(n)}"

internal fun Solicitud.estado(cantidadOfertas: Int): String = when {
    aceptada != null -> "Finalizada"
    cantidadOfertas > 0 -> "Con ofertas"
    else -> "Pendiente"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InformacionSolicitud(solicitud: Solicitud) {
    Panel {
        Text("Más información", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("¿Tiene agua?", style = MaterialTheme.typography.bodyMedium)
            Text(if (solicitud.agua) "Sí" else "No", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Text("Obstáculos", style = MaterialTheme.typography.labelLarge)
        if (solicitud.obstaculos.isBlank()) {
            Text("Ninguno indicado", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                solicitud.obstaculos.split(", ").forEach { obstaculo ->
                    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(obstaculo, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (solicitud.fotos.isNotBlank()) {
            Text("Fotos del terreno", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                solicitud.fotos.lineSequence().filter(String::isNotBlank).forEach { uri ->
                    FotoTerreno(uri, Modifier.size(84.dp))
                }
            }
        }
        Text("Comentarios adicionales", style = MaterialTheme.typography.labelLarge)
        Text(solicitud.comentario.ifBlank { "Sin comentarios" }, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TarjetaOferta(oferta: Oferta, aceptada: String?, enviando: Boolean, aceptar: () -> Unit, contactar: () -> Unit) {
    Panel {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(oferta.nombre, style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.StarBorder, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Sin calificaciones", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(pesos(oferta.precio), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("CLP por ha", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (aceptada == oferta.aplicador) Text("Oferta aceptada", color = MaterialTheme.colorScheme.primary)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (aceptada == null) {
                Button(
                    onClick = aceptar,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    enabled = !enviando,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) { Text("Aceptar oferta") }
            }
            if (aceptada == null || aceptada == oferta.aplicador) {
                Button(
                    onClick = contactar,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) { Text("Comunicarse") }
            }
        }
    }
}

@Composable
internal fun Detalle(
    u: Usuario,
    solicitudesViewModel: SolicitudViewModel,
    ofertasViewModel: OfertaViewModel,
    volver: () -> Unit
) {
    val productor = u.rol == Rol.PRODUCTOR
    val detalle by solicitudesViewModel.detalle.collectAsState()
    val estadoOfertas by ofertasViewModel.estado.collectAsState()
    val operacion by ofertasViewModel.operacion.collectAsState()
    var precio by rememberSaveable { mutableStateOf("") }
    var telefonoAlternativo by rememberSaveable { mutableStateOf<String?>(null) }
    var aplicadorPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val actual = detalle.solicitud
    val lista = estadoOfertas.ofertas
    val enviando = operacion.enviando
    val enviada = lista?.firstOrNull { it.aplicador == u.rut }
    val monto = precio.toLongOrNull() ?: 0L
    val pie: (@Composable () -> Unit)? = if (!productor && actual != null && actual.aceptada == null && lista != null && enviada == null) {
        {
            Accion(if (enviando) "Enviando…" else "Enviar oferta", enabled = monto > 0 && !enviando) {
                ofertasViewModel.enviar(u, monto)
            }
        }
    } else {
        null
    }

    Pagina(titulo = if (productor) "Detalle solicitud" else "Detalle de solicitud", volver = volver, pie = pie) {
        if (detalle.error != null) {
            Text(detalle.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            return@Pagina
        }
        if (detalle.cargando) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Pagina
        }
        if (actual == null) {
            Text("Solicitud no disponible.")
            return@Pagina
        }
        if (estadoOfertas.error != null) {
            Text(estadoOfertas.error.orEmpty(), color = MaterialTheme.colorScheme.error)
            return@Pagina
        }
        if (lista == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            return@Pagina
        }
        Panel { Resumen(actual, if (productor || actual.aceptada != null) actual.estado(lista.size) else "Sin ubicación") }
        if (productor) {
            Text("Ofertas (${lista.size})", style = MaterialTheme.typography.titleLarge)
            if (lista.isEmpty()) Text("Las ofertas de los aplicadores aparecerán aquí.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            lista.forEach { oferta ->
                key(oferta.aplicador) {
                    TarjetaOferta(oferta, actual.aceptada, enviando, aceptar = { aplicadorPendiente = oferta.aplicador }) {
                        telefonoAlternativo = null
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", oferta.telefono, null)))
                        } catch (_: ActivityNotFoundException) {
                            telefonoAlternativo = "Teléfono de contacto: ${oferta.telefono}"
                        }
                    }
                }
            }
            InformacionSolicitud(actual)
        } else {
            InformacionSolicitud(actual)
            if (enviada != null) {
                Panel {
                    Text(if (actual.aceptada == u.rut) "Tu oferta fue aceptada" else "Oferta enviada", style = MaterialTheme.typography.titleMedium)
                    Text("${pesos(enviada.precio)} CLP por ha")
                }
            } else {
                Panel {
                    Text("Ofertar", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = precio,
                        onValueChange = { if (it.length <= 10 && it.all { digit -> digit in '0'..'9' }) precio = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Precio por ha (CLP)") },
                        singleLine = true,
                        enabled = !enviando,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Text("Precio total: ${pesos((monto * actual.hectareas).roundToLong())} CLP + IVA", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        (telefonoAlternativo ?: operacion.mensaje)?.let { Text(it) }
    }

    val pendiente = if (productor && actual != null && actual.aceptada == null && !enviando) {
        lista?.firstOrNull { it.aplicador == aplicadorPendiente }
    } else {
        null
    }
    pendiente?.let { oferta ->
        AlertDialog(
            onDismissRequest = { aplicadorPendiente = null },
            title = { Text("¿Aceptar esta oferta?") },
            text = { Text("${oferta.nombre} aplicará por ${pesos(oferta.precio)} CLP por ha.") },
            dismissButton = { TextButton(onClick = { aplicadorPendiente = null }) { Text("Cancelar") } },
            confirmButton = {
                TextButton(onClick = {
                    aplicadorPendiente = null
                    telefonoAlternativo = null
                    ofertasViewModel.aceptar(u, oferta)
                }) { Text("Aceptar") }
            }
        )
    }
}
