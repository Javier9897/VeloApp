package com.duoc.velo.viewmodel

import android.graphics.Bitmap
import android.util.Log
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
import com.duoc.velo.repository.UsuarioRepository
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
        regError = null

        if (
            regRut.isBlank() ||
            regNombre.isBlank() ||
            regApellido.isBlank() ||
            regEmail.isBlank() ||
            regTelefono.isBlank() ||
            regPassword.isBlank() ||
            regConfirmPassword.isBlank()
        ) {
            regError = "Tienes que rellenar todos los campos"
            return
        }

        if (regPassword.length < 8) {
            regError = "La contraseña debe tener al menos 8 caracteres"
            return
        }

        if (regPassword != regConfirmPassword) {
            regError = "Las contraseñas no coinciden"
            return
        }

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
                if (repository.obtenerUsuarioPorRut(nuevoUsuario.rut) != null) {
                    regError = "El RUT ingresado ya está registrado"
                    return@launch
                }

                if (repository.obtenerUsuarioPorEmail(nuevoUsuario.email) != null) {
                    regError = "El correo ingresado ya está registrado"
                    return@launch
                }

                repository.registrarUsuario(nuevoUsuario.copy(fotoPerfil = foto?.let { codificarFoto(it) }))
                limpiarRegistro()
                emailRegistrado = nuevoUsuario.email
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                Log.e("VELO", "Error al registrar", error)
                regError = "No se pudo crear la cuenta. Inténtalo nuevamente"
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
