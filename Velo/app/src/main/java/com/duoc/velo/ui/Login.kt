package com.duoc.velo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.velo.model.Usuario
import com.duoc.velo.viewmodel.InicioSesionViewModel

@Composable
internal fun LoginScreen(
    viewModel: InicioSesionViewModel,
    onLoginExitoso: (Usuario) -> Unit,
    onIrARegistro: () -> Unit
) {
    val usuarioVerificado = viewModel.usuarioVerificado
    val cargando = viewModel.loginLoading || usuarioVerificado != null
    var recuperar by rememberSaveable { mutableStateOf(false) }
    val iniciarSesion = viewModel::iniciarSesion

    LaunchedEffect(viewModel, usuarioVerificado) {
        if (usuarioVerificado != null) {
            viewModel.consumirUsuarioVerificado()?.let(onLoginExitoso)
        }
    }

    BackHandler(enabled = cargando) {}

    if (recuperar) {
        AlertDialog(
            onDismissRequest = { recuperar = false },
            title = { Text("Recuperar contraseña") },
            text = {
                Text(
                    "Las cuentas se guardan en este dispositivo y la recuperación por correo aún no está disponible. " +
                        "Si olvidaste tu usuario, prueba con el RUT o el correo que registraste."
                )
            },
            confirmButton = {
                TextButton(onClick = { recuperar = false }) { Text("Volver a iniciar sesión") }
            }
        )
    }

    ColumnaAuth {
        VeloMarca(destacada = true)

        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "AGRICULTURA Y SILVICULTURA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.8.sp
            )
            Text(
                "Tecnología que conecta con tu campo.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Agricultura de precisión, drones y robots en un solo lugar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge)

            CampoAuth(
                etiqueta = "Usuario",
                sugerencia = "Ingresa tu usuario",
                valor = viewModel.loginEmail,
                habilitado = !cargando,
                cambiar = {
                    viewModel.loginEmail = it
                    viewModel.loginError = null
                },
                teclado = KeyboardType.Email
            )
            CampoAuth(
                etiqueta = "Contraseña",
                sugerencia = "Ingresa tu contraseña",
                valor = viewModel.loginPassword,
                habilitado = !cargando,
                cambiar = {
                    viewModel.loginPassword = it
                    viewModel.loginError = null
                },
                teclado = KeyboardType.Password,
                accion = ImeAction.Done,
                onDone = iniciarSesion
            )
            TextButton(
                onClick = { recuperar = true },
                enabled = !cargando,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Recuperar contraseña")
            }
            viewModel.loginError?.let { MensajeAuth(it, error = true) }
                ?: viewModel.loginMensaje?.let { MensajeAuth(it) }

            Button(
                onClick = iniciarSesion,
                enabled = !cargando,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                if (cargando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Outlined.Login, contentDescription = null, Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(if (cargando) "Ingresando…" else "Iniciar sesión", fontWeight = FontWeight.SemiBold)
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            Text("o", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
        }

        Button(
            onClick = onIrARegistro,
            enabled = !cargando,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Text("Crear una cuenta", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun ColumnaAuth(contenido: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = contenido
            )
        }
    }
}

@Composable
internal fun CampoAuth(
    etiqueta: String,
    sugerencia: String,
    valor: String,
    habilitado: Boolean,
    cambiar: (String) -> Unit,
    teclado: KeyboardType = KeyboardType.Text,
    accion: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {}
) {
    val secreto = teclado == KeyboardType.Password
    var visible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = valor,
            onValueChange = cambiar,
            enabled = habilitado,
            placeholder = { Text(sugerencia) },
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = etiqueta },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = teclado, imeAction = accion),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            visualTransformation =
                if (secreto && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (secreto) {
                {
                    IconButton(onClick = { visible = !visible }, enabled = habilitado) {
                        Icon(
                            if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            } else {
                null
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}

@Composable
internal fun MensajeAuth(mensaje: String, error: Boolean = false) {
    Text(
        mensaje,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    )
}
