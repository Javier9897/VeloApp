package com.duoc.velo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.duoc.velo.model.Usuario
import com.duoc.velo.repository.UsuarioRepository
import kotlinx.coroutines.launch
import com.duoc.velo.model.Rol

class AuthViewModel(private val repository: UsuarioRepository): ViewModel(){

    // Login
    var loginEmail by mutableStateOf("")   // acepta email o rut
    var loginPassword by mutableStateOf("")
    var loginError by mutableStateOf<String?>(null)

    // Registro
    var regRut by mutableStateOf("")
    var regNombre by mutableStateOf("")
    var regApellido by mutableStateOf("")
    var regEmail by mutableStateOf("")
    var regTelefono by mutableStateOf("")
    var regPassword by mutableStateOf("")
    var regRol by mutableStateOf(Rol.PRODUCTOR)
    var regError by mutableStateOf<String?>(null)

    var usuarioAutenticado by mutableStateOf<Usuario?>(null)
        private set

    fun iniciarSesion(onSuccess : () -> Unit){
        if(loginEmail.isBlank() || loginPassword.isBlank()){
            loginError = "Porfavor, complete todos los campos"
            return
        }

        viewModelScope.launch {
            val usuario = repository.veriflogin(loginEmail.trim(), loginPassword.trim())
            if(usuario != null){
                usuarioAutenticado = usuario
                loginError = null
                onSuccess()
            }else{
                loginError = "Correo o contraseña incorrectas"
            }
        }
    }

    fun registrarUsuario(onSuccess: () -> Unit){
        if(regRut.isBlank() || regNombre.isBlank() || regApellido.isBlank() || regEmail.isBlank() ||
            regTelefono.isBlank() || regPassword.isBlank()
        ){
            regError = "Tienes que rellenar todos los campos"
            return
        }
        viewModelScope.launch {
            val usuarioExistente = repository.obtenerUsuarioPorRut(regRut.trim())
            if(usuarioExistente != null){
                regError = "El Rut ingresado ya esta registrado"
                return@launch
            }
            if(repository.obtenerUsuarioPorEmail(regEmail.trim()) != null){
                regError = "El correo ingresado ya esta registrado"
                return@launch
            }
            val nuevoUsuario = Usuario(
                rut = regRut.trim(),
                nombre = regNombre.trim(),
                apellido = regApellido.trim(),
                email = regEmail.trim(),
                telefono = regTelefono.trim(),
                password = regPassword.trim(),
                rol = regRol
            )
            repository.registrarUsuario(nuevoUsuario)
            regError = null
            regRut = ""; regNombre = ""; regApellido = ""
            regEmail = ""; regTelefono = ""; regPassword = ""; regRol = Rol.PRODUCTOR
            onSuccess()
        }
    }

    fun cerrarSesion(){
        usuarioAutenticado = null
    }
}

// Esta clase hace que viewmodel tome el repository obligatoriamente
class AuthViewModelFactory(private val repository: UsuarioRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
}