package com.duoc.velo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.duoc.velo.model.Usuario
import com.duoc.velo.repository.AccesoUsuario

internal class SesionViewModel : ViewModel() {
    private var acceso by mutableStateOf<AccesoUsuario?>(null)
    val usuarioAutenticado: Usuario? get() = acceso?.usuario
    val token: String? get() = acceso?.token

    fun abrirSesion(accesoUsuario: AccesoUsuario) {
        acceso = accesoUsuario
    }

    fun cerrarSesion() {
        acceso = null
    }

    fun aplicarFotoPerfil(usuarioOriginal: Usuario, foto: ByteArray) {
        val sesionActual = acceso ?: return
        if (sesionActual.usuario === usuarioOriginal) {
            acceso = sesionActual.copy(usuario = usuarioOriginal.copy(fotoPerfil = foto))
        }
    }
}
