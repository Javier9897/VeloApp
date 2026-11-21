package com.duoc.velo.repository

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

internal const val MAX_FOTOS_SOLICITUD = 6
private const val MAX_LADO_FOTO = 768
private const val MAX_BYTES_ENVIO = 160 * 1024
private const val MAX_CARACTERES_FOTO = 6 * 1024 * 1024
private const val JPEG_BASE64 = "data:image/jpeg;base64,"

internal fun leerFotoTerreno(resolver: ContentResolver, fuente: String): Bitmap? {
    val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val decode: (BitmapFactory.Options) -> Bitmap?
    if (fuente.startsWith("data:")) {
        val encabezado = fuente.substringBefore(',')
        if (encabezado !in listOf("data:image/jpeg;base64", "data:image/png;base64", "data:image/webp;base64")) {
            return null
        }
        if (fuente.length > MAX_CARACTERES_FOTO) return null
        val bytes = try {
            Base64.decode(fuente.substringAfter(','), Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            return null
        }
        decode = { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, it) }
    } else {
        val uri = Uri.parse(fuente)
        decode = { resolver.openInputStream(uri)?.use { flujo -> BitmapFactory.decodeStream(flujo, null, it) } }
    }
    return try {
        decode(opciones)
        if (opciones.outWidth <= 0 || opciones.outHeight <= 0) return null
        while (maxOf(opciones.outWidth, opciones.outHeight) / opciones.inSampleSize.coerceAtLeast(1) > MAX_LADO_FOTO) {
            opciones.inSampleSize = opciones.inSampleSize.coerceAtLeast(1) * 2
        }
        opciones.inJustDecodeBounds = false
        decode(opciones)
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }
}

internal class FotoTerrenoException(message: String) : IOException(message)

internal suspend fun codificarFotosTerreno(resolver: ContentResolver, fotos: String): String =
    withContext(Dispatchers.IO) {
        val fuentes = fotos.lineSequence().filter(String::isNotBlank).toList()
        if (fuentes.size > MAX_FOTOS_SOLICITUD) {
            throw FotoTerrenoException("Puedes adjuntar hasta $MAX_FOTOS_SOLICITUD fotos del terreno.")
        }
        fuentes.joinToString("\n") { fuente ->
            val bitmap = leerFotoTerreno(resolver, fuente)
                ?: throw FotoTerrenoException("No se pudo leer una foto del terreno. Vuelve a seleccionarla.")
            try {
                val salida = ByteArrayOutputStream()
                for (calidad in listOf(78, 58, 38)) {
                    salida.reset()
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, calidad, salida)) {
                        salida.reset()
                    } else if (salida.size() <= MAX_BYTES_ENVIO) {
                        break
                    }
                }
                if (salida.size() == 0 || salida.size() > MAX_BYTES_ENVIO) {
                    throw FotoTerrenoException("Una foto supera el tamaño permitido. Selecciona otra imagen.")
                }
                JPEG_BASE64 + Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
            } finally {
                bitmap.recycle()
            }
        }
    }
