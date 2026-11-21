package com.duoc.velo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.room.Room
import com.duoc.velo.model.AppDatabase
import com.duoc.velo.model.SolicitudesDatabase
import com.duoc.velo.repository.SolicitudesRepository
import com.duoc.velo.repository.SolicitudesRepositoryLocal
import com.duoc.velo.repository.SolicitudesRepositoryRemoto
import com.duoc.velo.repository.UsuarioRepository
import com.duoc.velo.repository.UsuarioRepositoryLocal
import com.duoc.velo.repository.UsuarioRepositoryRemoto
import com.duoc.velo.repository.VeloApi
import com.duoc.velo.repository.codificarFotosTerreno
import com.duoc.velo.ui.Navegacion
import com.duoc.velo.ui.theme.VeloTheme
import com.duoc.velo.viewmodel.InicioSesionViewModel
import com.duoc.velo.viewmodel.InicioSesionViewModelFactory
import com.duoc.velo.viewmodel.SesionViewModel

class MainActivity : ComponentActivity() {

    // Se construye la base de datos local con Room
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext, // Contexto de la app
            AppDatabase::class.java, // Se concenta con AppDataBase -> DAOs existentes
            "velo_db" // Nombre de la base de datos local
        ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build() // Finalmente se construye
    }

    private val sesionViewModel: SesionViewModel by viewModels()
    private val api by lazy { BuildConfig.VELO_API_URL.takeIf(String::isNotBlank)?.let(::VeloApi) }
    private val repository: UsuarioRepository by lazy {
        val sesion = sesionViewModel
        val remota = api
        if (remota == null) UsuarioRepositoryLocal(db.usuarioDao())
        else UsuarioRepositoryRemoto(remota) { sesion.token }
    }
    private val solicitudesRepository: SolicitudesRepository by lazy {
        val sesion = sesionViewModel
        val remota = api
        if (remota == null) {
            SolicitudesRepositoryLocal(SolicitudesDatabase.obtener(applicationContext).dao())
        } else {
            val resolver = applicationContext.contentResolver
            SolicitudesRepositoryRemoto(remota, { fotos -> codificarFotosTerreno(resolver, fotos) }) { sesion.token }
        }
    }
    private val inicioSesionViewModel: InicioSesionViewModel by viewModels { InicioSesionViewModelFactory(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeloTheme {
                Navegacion(inicioSesionViewModel, sesionViewModel, repository, solicitudesRepository)
            }
        }
    }
}
