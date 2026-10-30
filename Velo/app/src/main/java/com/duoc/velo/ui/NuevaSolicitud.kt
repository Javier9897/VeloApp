package com.duoc.velo.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duoc.velo.R
import com.duoc.velo.viewmodel.SolicitudViewModel
import com.duoc.velo.viewmodel.opcionesAguaSolicitud
import com.duoc.velo.viewmodel.productosSolicitud
import com.duoc.velo.viewmodel.tiposObstaculosSolicitud
import com.duoc.velo.viewmodel.turnosSolicitud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun FotoTerreno(uri: String, modifier: Modifier = Modifier) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<Bitmap?>(null, resolver, uri) {
        value = withContext(Dispatchers.IO) {
            val fuente = Uri.parse(uri)
            val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            try {
                resolver.openInputStream(fuente)?.use { BitmapFactory.decodeStream(it, null, opciones) }
                opciones.inSampleSize = 1
                while (maxOf(opciones.outWidth, opciones.outHeight) / opciones.inSampleSize > 768) {
                    opciones.inSampleSize *= 2
                }
                opciones.inJustDecodeBounds = false
                resolver.openInputStream(fuente)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        val foto = bitmap
        if (foto == null) {
            Box(contentAlignment = Alignment.Center) { Simbolo(R.drawable.velo_imagen, 22) }
        } else {
            Image(foto.asImageBitmap(), "Foto del terreno", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
private fun Opciones(
    etiqueta: String,
    opciones: List<String>,
    valor: String,
    enabled: Boolean,
    elegir: (String) -> Unit
) {
    Column {
        Text(etiqueta, style = MaterialTheme.typography.titleSmall)
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            opciones.forEach { opcion ->
                Row(
                    Modifier.heightIn(min = 48.dp).selectable(
                        selected = opcion == valor,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { elegir(opcion) }
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(selected = opcion == valor, onClick = null, enabled = enabled)
                    Text(opcion, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
internal fun NuevaSolicitud(viewModel: SolicitudViewModel, volver: () -> Unit) {
    val formulario by viewModel.formulario.collectAsStateWithLifecycle()
    val publicacion by viewModel.publicacion.collectAsStateWithLifecycle()
    val bloqueado = publicacion.guardando || publicacion.creada
    var eligiendoProducto by remember { mutableStateOf(false) }
    val resolver = LocalContext.current.contentResolver
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && uri.toString() !in viewModel.formulario.value.fotos) {
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.agregarFoto(uri.toString())
            } catch (_: SecurityException) {
                viewModel.informarErrorFoto()
            }
        }
    }
    LaunchedEffect(publicacion.creada) {
        if (publicacion.creada) volver()
    }

    BackHandler(enabled = bloqueado) {}
    Pagina(
        titulo = "Nueva solicitud",
        volver = { if (!bloqueado) volver() },
        pie = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                publicacion.error?.let { error ->
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Accion(
                    if (bloqueado) "Guardando…" else "Crear solicitud",
                    enabled = !bloqueado,
                    click = viewModel::publicar
                )
            }
        }
    ) {
        Text("Cuéntanos qué necesitas para tu próxima aplicación.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Tipo de producto", style = MaterialTheme.typography.titleSmall)
            Box {
                OutlinedButton(
                    onClick = { eligiendoProducto = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    enabled = !bloqueado,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(formulario.producto.ifBlank { "Selecciona un producto" }, Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = eligiendoProducto, onDismissRequest = { eligiendoProducto = false }) {
                    productosSolicitud.forEach { opcion ->
                        DropdownMenuItem(text = { Text(opcion) }, onClick = {
                            viewModel.editarFormulario { copy(producto = opcion) }
                            eligiendoProducto = false
                        })
                    }
                }
            }
        }
        OutlinedTextField(
            value = formulario.superficie,
            onValueChange = { nuevoValor -> viewModel.editarFormulario { copy(superficie = nuevoValor) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Superficie (ha)") },
            singleLine = true,
            enabled = !bloqueado,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        Opciones("¿Tiene agua?", opcionesAguaSolicitud, formulario.agua, !bloqueado) { opcion ->
            viewModel.editarFormulario { copy(agua = opcion) }
        }
        Opciones("Aplicación", turnosSolicitud, formulario.turno, !bloqueado) { opcion ->
            viewModel.editarFormulario { copy(turno = opcion) }
        }
        Column {
            Text("Obstáculos", style = MaterialTheme.typography.titleSmall)
            tiposObstaculosSolicitud.forEach { opcion ->
                val marcado = opcion in formulario.obstaculos
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).toggleable(
                        value = marcado,
                        enabled = !bloqueado,
                        role = Role.Checkbox,
                        onValueChange = { seleccionado ->
                            viewModel.editarFormulario {
                                copy(obstaculos = if (seleccionado) obstaculos + opcion else obstaculos - opcion)
                            }
                        }
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(marcado, onCheckedChange = null, enabled = !bloqueado)
                    Text(opcion, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        OutlinedTextField(
            value = formulario.comentario,
            onValueChange = { nuevoValor -> viewModel.editarFormulario { copy(comentario = nuevoValor) } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Comentarios adicionales") },
            minLines = 3,
            enabled = !bloqueado,
            shape = RoundedCornerShape(12.dp)
        )
        Text("Fotos del terreno", style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, enabled = !bloqueado) {
                Simbolo(R.drawable.velo_imagen, 18)
                Text("Agregar foto", Modifier.padding(start = 8.dp))
            }
            formulario.fotos.forEach { uri ->
                Box {
                    FotoTerreno(uri, Modifier.size(84.dp))
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        IconButton(
                            onClick = { viewModel.editarFormulario { copy(fotos = fotos - uri) } },
                            enabled = !bloqueado
                        ) {
                            Icon(Icons.Filled.Close, "Quitar foto", Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}
