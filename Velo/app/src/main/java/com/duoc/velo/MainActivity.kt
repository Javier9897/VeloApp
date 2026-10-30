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
import com.duoc.velo.viewmodel.AuthViewModel
import com.duoc.velo.viewmodel.AuthViewModelFactory

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
    private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeloTheme {
                Navegacion(authViewModel, repository, solicitudesDao)
            }
        }
    }
}
