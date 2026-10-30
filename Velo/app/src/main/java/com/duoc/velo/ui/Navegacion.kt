package com.duoc.velo.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.duoc.velo.model.Rol
import com.duoc.velo.model.SolicitudesDao
import com.duoc.velo.repository.UsuarioRepository
import com.duoc.velo.viewmodel.AuthViewModel
import com.duoc.velo.viewmodel.OfertaViewModel
import com.duoc.velo.viewmodel.OfertaViewModelFactory
import com.duoc.velo.viewmodel.PerfilViewModel
import com.duoc.velo.viewmodel.PerfilViewModelFactory
import com.duoc.velo.viewmodel.SolicitudViewModel
import com.duoc.velo.viewmodel.SolicitudViewModelFactory

private object Ruta {
    const val Bienvenida = "bienvenida"
    const val Login = "login"
    const val Registro = "registro"
    const val Solicitudes = "solicitudes"
    const val Nueva = "nueva-solicitud"
    const val Perfil = "perfil"
    const val Detalle = "detalle/{id}"
    fun detalle(id: String) = "detalle/$id"
}

@Composable
internal fun Navegacion(
    authViewModel: AuthViewModel,
    usuarioRepository: UsuarioRepository,
    solicitudesDao: SolicitudesDao
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val view = LocalView.current
    val entrada by nav.currentBackStackEntryAsState()
    val ruta = entrada?.destination?.route
    val usuario by rememberUpdatedState(authViewModel.usuarioAutenticado)
    val volver: () -> Unit = { nav.popBackStack() }

    SideEffect {
        val window = (context as? Activity)?.window
        if (window != null) {
            val fondoClaro = ruta != null && ruta != Ruta.Bienvenida
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = fondoClaro
                isAppearanceLightNavigationBars = fondoClaro
            }
        }
    }
    LaunchedEffect(ruta, usuario?.rut) {
        if (usuario == null && ruta in listOf(Ruta.Solicitudes, Ruta.Nueva, Ruta.Perfil, Ruta.Detalle)) {
            nav.abrirRaiz(Ruta.Login)
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(navController = nav, startDestination = Ruta.Bienvenida) {
            composable(Ruta.Bienvenida) {
                InicioScreen { nav.navigate(Ruta.Login) { launchSingleTop = true } }
            }
            composable(Ruta.Login) {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginExitoso = { nav.abrirRaiz(Ruta.Solicitudes) },
                    onIrARegistro = {
                        authViewModel.limpiarRegistro()
                        nav.navigate(Ruta.Registro) { launchSingleTop = true }
                    }
                )
            }
            composable(Ruta.Registro) {
                RegistroScreen(
                    viewModel = authViewModel,
                    onRegistroExitoso = { nav.popBackStack(Ruta.Login, inclusive = false) },
                    onVolver = volver
                )
            }
            composable(Ruta.Solicitudes) {
                val cuenta = usuario
                if (cuenta != null) {
                    val solicitudes: SolicitudViewModel = viewModel(
                        factory = SolicitudViewModelFactory(solicitudesDao, cuenta)
                    )
                    Listado(
                        viewModel = solicitudes,
                        perfil = { nav.navigate(Ruta.Perfil) { launchSingleTop = true } },
                        nueva = { nav.navigate(Ruta.Nueva) { launchSingleTop = true } },
                        abrir = { id -> nav.navigate(Ruta.detalle(id)) { launchSingleTop = true } }
                    )
                }
            }
            composable(Ruta.Nueva) {
                val cuenta = usuario
                if (cuenta?.rol == Rol.PRODUCTOR) {
                    val solicitud: SolicitudViewModel = viewModel(
                        factory = SolicitudViewModelFactory(solicitudesDao, cuenta)
                    )
                    NuevaSolicitud(solicitud, volver)
                }
            }
            composable(Ruta.Detalle, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                val id = entry.arguments?.getString("id")
                val cuenta = usuario
                if (cuenta != null && id != null) {
                    val solicitud: SolicitudViewModel = viewModel(
                        factory = SolicitudViewModelFactory(solicitudesDao, cuenta, id)
                    )
                    val ofertas: OfertaViewModel = viewModel(
                        factory = OfertaViewModelFactory(solicitudesDao, id)
                    )
                    Detalle(cuenta, solicitud, ofertas, volver)
                }
            }
            composable(Ruta.Perfil) {
                val cuenta = usuario
                if (cuenta != null) {
                    val perfil: PerfilViewModel = viewModel(factory = PerfilViewModelFactory(usuarioRepository))
                    BackHandler(enabled = perfil.fotoGuardando) {}
                    PerfilScreen(
                        usuario = cuenta,
                        fotoGuardando = perfil.fotoGuardando,
                        fotoError = perfil.fotoError,
                        onGuardarFoto = { foto ->
                            perfil.actualizarFotoPerfil(cuenta, foto, authViewModel::aplicarFotoPerfil)
                        },
                        onCerrarSesion = {
                            authViewModel.cerrarSesion()
                            nav.abrirRaiz(Ruta.Login)
                        },
                        onVolver = { if (!perfil.fotoGuardando) volver() }
                    )
                }
            }
        }
    }
}

private fun NavHostController.abrirRaiz(ruta: String) {
    navigate(ruta) {
        popUpTo(graph.id)
        launchSingleTop = true
    }
}
