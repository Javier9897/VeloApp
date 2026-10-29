package com.duoc.velo.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.duoc.velo.viewmodel.AuthViewModel

@Composable
fun Navegacion(viewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "bienvenida") {

        composable("bienvenida") {
            InicioScreen(
                onComenzar = {
                    navController.navigate("login") {
                        popUpTo("bienvenida") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                viewModel = viewModel,
                onLoginExitoso = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onIrARegistro = { navController.navigate("registro") } // Falta por crear
            )
        }
    }
}