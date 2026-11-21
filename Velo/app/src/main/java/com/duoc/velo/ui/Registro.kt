package com.duoc.velo.ui

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.duoc.velo.model.Rol
import com.duoc.velo.viewmodel.RegistroViewModel

@Composable
internal fun RegistroScreen(
    viewModel: RegistroViewModel,
    onRegistroExitoso: (String) -> Unit,
    onVolver: () -> Unit
) {
    val cargando = viewModel.regLoading
    var errorCamara by rememberSaveable { mutableStateOf(false) }
    val registrar = viewModel::registrarUsuario
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { foto ->
        if (foto != null) viewModel.regFoto = foto
    }

    LaunchedEffect(viewModel.emailRegistrado) {
        viewModel.consumirRegistroExitoso()?.let(onRegistroExitoso)
    }

    BackHandler { if (!cargando) onVolver() }

    ColumnaAuth {
        VeloMarca()
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Crea tu cuenta",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Conecta con una nueva forma de trabajar el campo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("¿Cómo quieres participar?", style = MaterialTheme.typography.labelLarge)
            Row(
                Modifier.fillMaxWidth().selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TipoCuenta(
                    etiqueta = "Soy un productor",
                    icono = Icons.Outlined.Eco,
                    seleccionado = viewModel.regRol == Rol.PRODUCTOR,
                    habilitado = !cargando,
                    modifier = Modifier.weight(1f)
                ) { viewModel.regRol = Rol.PRODUCTOR }
                TipoCuenta(
                    etiqueta = "Soy un aplicador",
                    icono = Icons.Outlined.Flight,
                    seleccionado = viewModel.regRol == Rol.APLICADOR,
                    habilitado = !cargando,
                    modifier = Modifier.weight(1f)
                ) { viewModel.regRol = Rol.APLICADOR }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CampoAuth("Nombre", "Tu nombre", viewModel.regNombre, !cargando, { viewModel.regNombre = it })
            CampoAuth("Apellido", "Tu apellido", viewModel.regApellido, !cargando, { viewModel.regApellido = it })
            CampoAuth("RUT", "12.345.678-5", viewModel.regRut, !cargando, { viewModel.regRut = it })
            CampoAuth(
                "Email", "nombre@correo.cl", viewModel.regEmail, !cargando, { viewModel.regEmail = it },
                teclado = KeyboardType.Email
            )
            CampoAuth(
                "Teléfono", "+56 9 1234 5678", viewModel.regTelefono, !cargando, { viewModel.regTelefono = it },
                teclado = KeyboardType.Phone
            )
            CampoAuth(
                "Contraseña", "Mínimo 8 caracteres", viewModel.regPassword, !cargando, { viewModel.regPassword = it },
                teclado = KeyboardType.Password
            )
            CampoAuth(
                "Confirmar contraseña", "Repite tu contraseña", viewModel.regConfirmPassword, !cargando,
                cambiar = { viewModel.regConfirmPassword = it },
                teclado = KeyboardType.Password,
                accion = ImeAction.Done,
                onDone = registrar
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Foto de perfil (opcional)", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                viewModel.regFoto?.let { foto ->
                    Image(
                        foto.asImageBitmap(),
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.size(52.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                OutlinedButton(
                    onClick = {
                        errorCamara = false
                        try {
                            camara.launch(null)
                        } catch (_: ActivityNotFoundException) {
                            errorCamara = true
                        }
                    },
                    enabled = !cargando
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (viewModel.regFoto == null) "Tomar foto" else "Cambiar foto")
                }
            }
            if (errorCamara) MensajeAuth("La cámara no está disponible. Puedes registrarte sin foto.", error = true)
        }

        viewModel.regError?.let { MensajeAuth(it, error = true) }
        Accion(if (cargando) "Creando cuenta…" else "Registrarme", enabled = !cargando, click = registrar)
        TextButton(
            onClick = onVolver,
            enabled = !cargando,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}

@Composable
private fun TipoCuenta(
    etiqueta: String,
    icono: ImageVector,
    seleccionado: Boolean,
    habilitado: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val colores = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.selectable(
            selected = seleccionado,
            enabled = habilitado,
            role = Role.RadioButton,
            onClick = onClick
        ),
        shape = RoundedCornerShape(12.dp),
        color = if (seleccionado) colores.primaryContainer else colores.surface,
        border = BorderStroke(1.dp, if (seleccionado) colores.primary else colores.outline)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icono, contentDescription = null, Modifier.size(22.dp), tint = colores.primary)
                Icon(
                    if (seleccionado) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    Modifier.size(18.dp),
                    tint = if (seleccionado) colores.primary else colores.onSurfaceVariant
                )
            }
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
