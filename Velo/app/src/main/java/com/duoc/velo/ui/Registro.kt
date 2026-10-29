package com.duoc.velo.ui

import com.duoc.velo.model.Rol
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.duoc.velo.viewmodel.AuthViewModel

@Composable
fun RegistroScreen(
    viewModel: AuthViewModel,
    onRegistroExitoso: () -> Unit,
    onVolver: () -> Unit
) {
    var mostrarPassword by rememberSaveable { mutableStateOf(false) }
    val cargando = viewModel.regLoading

    BackHandler {
        if (!cargando) {
            onVolver()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "TecMundo Connect",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Crea tu cuenta",
                style = MaterialTheme.typography.headlineMedium
            )

            Text("Completa tus datos para registrarte.")

            Text("Tipo de cuenta")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = viewModel.regRol == Rol.PRODUCTOR,
                    onClick = {
                        viewModel.regRol = Rol.PRODUCTOR
                    },
                    label = { Text("Productor") },
                    enabled = !cargando,
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = viewModel.regRol == Rol.APLICADOR,
                    onClick = {
                        viewModel.regRol = Rol.APLICADOR
                    },
                    label = { Text("Aplicador") },
                    enabled = !cargando,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = viewModel.regNombre,
                onValueChange = { viewModel.regNombre = it },
                label = { Text("Nombre") },
                singleLine = true,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.regApellido,
                onValueChange = { viewModel.regApellido = it },
                label = { Text("Apellido") },
                singleLine = true,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.regRut,
                onValueChange = { viewModel.regRut = it },
                label = { Text("RUT") },
                placeholder = { Text("12.345.678-5") },
                singleLine = true,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.regEmail,
                onValueChange = { viewModel.regEmail = it },
                label = { Text("Correo electrónico") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                singleLine = true,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.regTelefono,
                onValueChange = { viewModel.regTelefono = it },
                label = { Text("Teléfono") },
                placeholder = { Text("+56 9 1234 5678") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                singleLine = true,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.regPassword,
                onValueChange = { viewModel.regPassword = it },
                label = { Text("Contraseña") },
                supportingText = { Text("Mínimo 8 caracteres") },
                visualTransformation = if (mostrarPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                singleLine = true,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.regConfirmPassword,
                onValueChange = { viewModel.regConfirmPassword = it },
                label = { Text("Confirmar contraseña") },
                visualTransformation = if (mostrarPassword) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                singleLine = true,
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = mostrarPassword,
                    onCheckedChange = { mostrarPassword = it },
                    enabled = !cargando
                )
                Text("Mostrar contraseñas")
            }

            viewModel.regError?.let { mensaje ->
                Text(
                    text = mensaje,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    viewModel.registrarUsuario(onRegistroExitoso)
                },
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    if (cargando) "Registrando..." else "Registrarse"
                )
            }

            TextButton(
                onClick = onVolver,
                enabled = !cargando,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("¿Ya tienes cuenta? Inicia sesión")
            }
        }
    }
}