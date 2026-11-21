package com.duoc.velo.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.duoc.velo.model.Rol
import com.duoc.velo.model.Usuario
import com.duoc.velo.model.validarRegistro
import com.duoc.velo.repository.RegistroDuplicadoException
import com.duoc.velo.repository.UsuarioRepository
import com.duoc.velo.repository.mensajeApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal class RegistroViewModel(private val repository: UsuarioRepository) : ViewModel() {
    var regRut by mutableStateOf("")
    var regNombre by mutableStateOf("")
    var regApellido by mutableStateOf("")
    var regEmail by mutableStateOf("")
    var regTelefono by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regConfirmPassword by mutableStateOf("")
    var regRol by mutableStateOf(Rol.PRODUCTOR)
    var regFoto by mutableStateOf<Bitmap?>(null)
    var regError by mutableStateOf<String?>(null)
        private set
    var regLoading by mutableStateOf(false)
        private set
    var emailRegistrado by mutableStateOf<String?>(null)
        private set

    fun registrarUsuario() {
        if (regLoading || emailRegistrado != null) return
        regError = validarRegistro(regRut, regNombre, regApellido, regEmail, regTelefono, regPassword, regConfirmPassword)
        if (regError != null) return

        val nuevoUsuario = Usuario(
            rut = regRut.trim(),
            nombre = regNombre.trim(),
            apellido = regApellido.trim(),
            email = regEmail.trim(),
            telefono = regTelefono.trim(),
            password = regPassword,
            rol = regRol
        )
        val foto = regFoto
        regLoading = true

        viewModelScope.launch {
            try {
                repository.registrarUsuario(nuevoUsuario.copy(fotoPerfil = foto?.let { codificarFoto(it) }))
                limpiarRegistro()
                emailRegistrado = nuevoUsuario.email
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: RegistroDuplicadoException) {
                regError = error.message
            } catch (error: Exception) {
                regError = error.mensajeApi("No se pudo crear la cuenta. Inténtalo nuevamente")
            } finally {
                regLoading = false
            }
        }
    }

    fun consumirRegistroExitoso(): String? {
        val email = emailRegistrado
        emailRegistrado = null
        return email
    }

    private fun limpiarRegistro() {
        regRut = ""
        regNombre = ""
        regApellido = ""
        regEmail = ""
        regTelefono = ""
        regPassword = ""
        regConfirmPassword = ""
        regRol = Rol.PRODUCTOR
        regFoto = null
        regError = null
    }
}

@Suppress("FunctionName")
internal fun RegistroViewModelFactory(repository: UsuarioRepository): ViewModelProvider.Factory =
    viewModelFactory { initializer { RegistroViewModel(repository) } }
