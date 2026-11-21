package com.duoc.velo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.velo.repository.AccesoUsuario
import com.duoc.velo.repository.UsuarioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal class InicioSesionViewModel(
    private val repository: UsuarioRepository
) : ViewModel() {
    var loginEmail by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    var loginError by mutableStateOf<String?>(null)
    var loginMensaje by mutableStateOf<String?>(null)
        private set
    var loginLoading by mutableStateOf(false)
        private set
    var usuarioVerificado by mutableStateOf<AccesoUsuario?>(null)
        private set

    fun iniciarSesion() {
        if (loginLoading || usuarioVerificado != null) return
        loginError = null

        if (loginEmail.isBlank() || loginPassword.isBlank()) {
            loginError = "Completa el correo o RUT y la contraseña"
            return
        }

        val identificador = loginEmail.trim()
        val password = loginPassword
        loginLoading = true

        viewModelScope.launch {
            try {
                val usuario = repository.veriflogin(identificador, password)
                if (usuario != null) {
                    loginPassword = ""
                    loginMensaje = null
                    usuarioVerificado = usuario
                } else {
                    loginError = "Correo, RUT o contraseña incorrectos"
                }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (_: Exception) {
                loginError = "No se pudo iniciar sesión. Inténtalo nuevamente"
            } finally {
                loginLoading = false
            }
        }
    }

    fun consumirUsuarioVerificado(): AccesoUsuario? {
        val usuario = usuarioVerificado
        usuarioVerificado = null
        return usuario
    }

    fun prepararTrasRegistro(email: String) {
        loginEmail = email
        loginPassword = ""
        loginError = null
        loginMensaje = "Cuenta creada correctamente. Ya puedes iniciar sesión"
    }

    fun limpiarTrasCerrarSesion() {
        loginPassword = ""
        loginError = null
        loginMensaje = null
        usuarioVerificado = null
    }
}

@Suppress("FunctionName")
internal fun InicioSesionViewModelFactory(repository: UsuarioRepository) = viewModelFactory {
    initializer { InicioSesionViewModel(repository) }
}
