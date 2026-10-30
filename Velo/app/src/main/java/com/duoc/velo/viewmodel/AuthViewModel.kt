package com.duoc.velo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.velo.model.Rol
import com.duoc.velo.model.Usuario
import com.duoc.velo.repository.UsuarioRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: UsuarioRepository
) : ViewModel() {

    // Login
    var loginEmail by mutableStateOf("")
    var loginPassword by mutableStateOf("")
    var loginError by mutableStateOf<String?>(null)
    var loginMensaje by mutableStateOf<String?>(null)

    var loginLoading by mutableStateOf(false)
        private set

    // Registro
    var regRut by mutableStateOf("")
    var regNombre by mutableStateOf("")
    var regApellido by mutableStateOf("")
    var regEmail by mutableStateOf("")
    var regTelefono by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regConfirmPassword by mutableStateOf("")
    var regRol by mutableStateOf(Rol.PRODUCTOR)
    var regError by mutableStateOf<String?>(null)

    var regLoading by mutableStateOf(false)
        private set

    var usuarioAutenticado by mutableStateOf<Usuario?>(null)
        private set

    fun iniciarSesion(onSuccess: () -> Unit) {
        if (loginLoading) return

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
                val usuario = repository.veriflogin(
                    identificador,
                    password
                )

                if (usuario != null) {
                    usuarioAutenticado = usuario
                    loginPassword = ""
                    loginMensaje = null
                    onSuccess()
                } else {
                    loginError = "Correo, RUT o contraseña incorrectos"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                loginError = "No se pudo iniciar sesión. Inténtalo nuevamente"
            } finally {
                loginLoading = false
            }
        }
    }

    fun registrarUsuario(onSuccess: () -> Unit) {
        if (regLoading) return

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

        regLoading = true

        viewModelScope.launch {
            try {
                val usuarioExistente =
                    repository.obtenerUsuarioPorRut(nuevoUsuario.rut)

                if (usuarioExistente != null) {
                    regError = "El RUT ingresado ya está registrado"
                    return@launch
                }

                val correoExistente =
                    repository.obtenerUsuarioPorEmail(nuevoUsuario.email)

                if (correoExistente != null) {
                    regError = "El correo ingresado ya está registrado"
                    return@launch
                }

                repository.registrarUsuario(nuevoUsuario)

                loginEmail = nuevoUsuario.email
                loginPassword = ""
                loginError = null
                loginMensaje = "Cuenta creada correctamente. Ya puedes iniciar sesión"

                limpiarRegistro()
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("VELO", "Error al registrar", e)
                regError = "No se pudo crear la cuenta. Inténtalo nuevamente"
            }
        }
    }

    fun limpiarRegistro() {
        regRut = ""
        regNombre = ""
        regApellido = ""
        regEmail = ""
        regTelefono = ""
        regPassword = ""
        regConfirmPassword = ""
        regRol = Rol.PRODUCTOR
        regError = null
    }

    fun cerrarSesion() {
        usuarioAutenticado = null
        loginPassword = ""
        loginError = null
        loginMensaje = null
    }
}

class AuthViewModelFactory(
    private val repository: UsuarioRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}