package com.duoc.velo.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duoc.velo.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
internal fun PerfilScreen(
    usuario: Usuario,
    fotoGuardando: Boolean,
    fotoError: String?,
    onGuardarFoto: (Bitmap) -> Unit,
    onCerrarSesion: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()
    var errorSeleccion by rememberSaveable(usuario.rut) { mutableStateOf<String?>(null) }
    var seleccionando by remember { mutableStateOf(false) }
    val ocupado = seleccionando || fotoGuardando
    val seleccionarFoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            seleccionando = true
            alcance.launch {
                try {
                    val foto = withContext(Dispatchers.IO) { cargarFotoPerfil(contexto, uri) }
                    if (foto != null) {
                        onGuardarFoto(foto)
                    } else {
                        errorSeleccion = "No se pudo abrir la imagen seleccionada."
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    errorSeleccion = "No se pudo abrir la imagen seleccionada."
                } finally {
                    seleccionando = false
                }
            }
        }
    }

    Pagina(
        titulo = "Mi perfil",
        volver = onVolver,
        modifier = modifier,
        pie = {
            FilledTonalButton(
                onClick = onCerrarSesion,
                enabled = !ocupado,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Cerrar sesión", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) {
        Panel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarPerfil(usuario, ocupado) {
                    errorSeleccion = null
                    seleccionarFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${usuario.nombre} ${usuario.apellido}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        usuario.rol.texto,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                CalificacionPerfil()
            }
        }
        (errorSeleccion ?: fotoError)?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Text("Datos", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DatoPerfil("Nombre", usuario.nombre)
            DatoPerfil("Apellido", usuario.apellido)
            DatoPerfil("RUT", usuario.rut)
            DatoPerfil("Email", usuario.email)
            DatoPerfil("Teléfono", usuario.telefono)
        }
    }
}

@Composable
private fun AvatarPerfil(usuario: Usuario, cargando: Boolean, onClick: () -> Unit) {
    val datos = usuario.fotoPerfil
    val foto by produceState<Bitmap?>(null, datos) {
        value = datos?.let { withContext(Dispatchers.Default) { BitmapFactory.decodeByteArray(it, 0, it.size) } }
    }
    val iniciales = usuario.nombre.take(1).uppercase() + usuario.apellido.take(1).uppercase()
    val etiqueta = if (datos == null) "Añadir foto de perfil" else "Cambiar foto de perfil"
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.size(48.dp).clip(CircleShape)
            .semantics { contentDescription = etiqueta }
            .clickable(enabled = !cargando, role = Role.Button, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val retrato = foto
            when {
                cargando -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                retrato != null -> Image(retrato.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else -> Text(iniciales, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun CalificacionPerfil() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFAA6A00), modifier = Modifier.size(16.dp))
            Text(
                "—",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { contentDescription = "Sin calificación" }
            )
        }
        Text("Calificación", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DatoPerfil(etiqueta: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelLarge)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp)
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                Text(valor, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun cargarFotoPerfil(contexto: Context, uri: Uri): Bitmap? {
    val resolver = contexto.contentResolver
    val ladoMaximo = 512
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
            val ancho = info.size.width
            val alto = info.size.height
            val escala = ladoMaximo.toDouble() / maxOf(ancho, alto)
            if (escala < 1) {
                decoder.setTargetSize((ancho * escala).roundToInt().coerceAtLeast(1), (alto * escala).roundToInt().coerceAtLeast(1))
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }
    val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
    val ladoOriginal = maxOf(opciones.outWidth, opciones.outHeight)
    if (ladoOriginal <= 0) return null
    opciones.inJustDecodeBounds = false
    opciones.inSampleSize = Integer.highestOneBit((ladoOriginal / ladoMaximo).coerceAtLeast(1))
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
}
