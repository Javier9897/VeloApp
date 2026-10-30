package com.duoc.velo.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.velo.model.Usuario
import com.duoc.velo.repository.UsuarioRepository
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PerfilViewModel(private val repository: UsuarioRepository) : ViewModel() {
    var fotoGuardando by mutableStateOf(false)
        private set
    var fotoError by mutableStateOf<String?>(null)
        private set

    fun actualizarFotoPerfil(
        usuario: Usuario,
        foto: Bitmap,
        onGuardada: (Usuario, ByteArray) -> Unit
    ) {
        if (fotoGuardando) return
        fotoGuardando = true
        fotoError = null

        viewModelScope.launch {
            try {
                val datos = codificarFoto(foto)
                check(repository.actualizarFotoPerfil(usuario.rut, datos) == 1)
                onGuardada(usuario, datos)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                fotoError = "No se pudo guardar tu foto de perfil. Inténtalo nuevamente."
            } finally {
                fotoGuardando = false
            }
        }
    }
}

internal class PerfilViewModelFactory(
    private val repository: UsuarioRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == PerfilViewModel::class.java) { "ViewModel desconocido" }
        return PerfilViewModel(repository) as T
    }
}

internal suspend fun codificarFoto(foto: Bitmap): ByteArray = withContext(Dispatchers.IO) {
    ByteArrayOutputStream().use { salida ->
        check(foto.compress(Bitmap.CompressFormat.JPEG, 88, salida))
        salida.toByteArray()
    }
}
