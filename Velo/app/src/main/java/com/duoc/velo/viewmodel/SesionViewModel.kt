package com.duoc.velo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.duoc.velo.model.Usuario

internal class SesionViewModel : ViewModel() {
    var usuarioAutenticado by mutableStateOf<Usuario?>(null)
        private set

    fun abrirSesion(usuario: Usuario) {
        usuarioAutenticado = usuario
    }

    fun cerrarSesion() {
        usuarioAutenticado = null
    }

    fun aplicarFotoPerfil(usuarioOriginal: Usuario, foto: ByteArray) {
        if (usuarioAutenticado === usuarioOriginal) {
            usuarioAutenticado = usuarioOriginal.copy(fotoPerfil = foto)
        }
    }
}
