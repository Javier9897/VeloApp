package com.duoc.velo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duoc.velo.R
import com.duoc.velo.ui.theme.VerdeClaro
import com.duoc.velo.ui.theme.VerdeOscuro

@Composable
fun InicioScreen(onComenzar: () -> Unit){
    Box(modifier = Modifier.fillMaxSize()){

        // Configuracion de la imagen de fondo
        Image(
            painter = painterResource(id = R.drawable.fondo_inicio),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Filtro de fondo para que se vea mas oscuro
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xB3102A1B))
        )

        // Contenido
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo de la App
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = VerdeClaro,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Eco,
                        contentDescription = null,
                        tint = VerdeOscuro,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Velo",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White
            )

            Spacer(Modifier.height(32.dp))

            Text(
                "Agricultura y Silvicultura",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                letterSpacing = 0.8.sp
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "Tecnologia que conecta con tu campo.",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onComenzar,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Comenzar ya", fontWeight = FontWeight.SemiBold)
            }

        }
    }
}