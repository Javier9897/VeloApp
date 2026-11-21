package com.duoc.velo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.room.Room
import com.duoc.velo.model.AppDatabase
import com.duoc.velo.model.SolicitudesDatabase
import com.duoc.velo.repository.UsuarioRepository
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

    private val repository by lazy { UsuarioRepository(db.usuarioDao()) }
    private val solicitudesDao by lazy { SolicitudesDatabase.obtener(applicationContext).dao() }
    private val inicioSesionViewModel: InicioSesionViewModel by viewModels { InicioSesionViewModelFactory(repository) }
    private val sesionViewModel: SesionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeloTheme {
                Navegacion(inicioSesionViewModel, sesionViewModel, repository, solicitudesDao)
            }
        }
    }
}
