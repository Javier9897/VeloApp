package com.duoc.velo.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.duoc.velo.R

@Composable
internal fun Simbolo(@DrawableRes id: Int, size: Int) {
    Image(painterResource(id), contentDescription = null, modifier = Modifier.size(size.dp))
}

@Composable
internal fun LogoVelo(grande: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(if (grande) 12.dp else 8.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.size(if (grande) 48.dp else 30.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Simbolo(R.drawable.velo_brote, if (grande) 26 else 16)
        }
    }
}

@Composable
internal fun VeloMarca(modifier: Modifier = Modifier, destacada: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LogoVelo(grande = destacada)
        Text(
            text = stringResource(R.string.app_name),
            style = if (destacada) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
internal fun Accion(texto: String, enabled: Boolean = true, click: () -> Unit) {
    Button(
        onClick = click,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    ) {
        Text(texto, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun Pagina(
    titulo: String,
    volver: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {},
    pie: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize().padding(horizontal = 24.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (volver != null) {
                    IconButton(onClick = volver, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
                VeloMarca(Modifier.weight(1f))
                acciones()
            }
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 10.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(titulo, style = MaterialTheme.typography.headlineMedium)
                contenido()
            }
            if (pie != null) Box(Modifier.fillMaxWidth().padding(bottom = 20.dp)) { pie() }
        }
    }
}

@Composable
internal fun Panel(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
