package com.duoc.velo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duoc.velo.R

@Composable
fun InicioScreen(onComenzar: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painterResource(R.drawable.velo_campo),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    0f to Color(0x66182E22),
                    0.5f to Color(0xD11B3426),
                    1f to Color(0x7A102418)
                )
            )
        )
        Column(
            Modifier.safeDrawingPadding()
                .widthIn(max = 390.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LogoVelo(grande = true)
            Text("TecMundo Connect", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(
                "AGRICULTURA Y SILVICULTURA",
                modifier = Modifier.padding(top = 12.dp),
                color = Color(0xFFE5F2E1),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                "Tecnología que conecta con tu campo.",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Conecta con soluciones de agricultura de precisión, drones y robots para el campo y los bosques.",
                color = Color(0xFFF0F5EF),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Box(Modifier.padding(top = 18.dp)) {
                Accion("Comenzar ya", click = onComenzar)
            }
        }
    }
}
