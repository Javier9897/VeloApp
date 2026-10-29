package com.duoc.velo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.room.Room
import com.duoc.velo.model.AppDatabase
import com.duoc.velo.repository.UsuarioRepository
import com.duoc.velo.ui.Navegacion
import com.duoc.velo.ui.theme.VeloTheme
import com.duoc.velo.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {

    // Se construye la base de datos local con Room
    private val db by lazy {
        Room.databaseBuilder(
            applicationContext, // Contexto de la app
            AppDatabase::class.java, // Se concenta con AppDataBase -> DAOs existentes
            "velo_db" // Nombre de la base de datos local
        ).build() // Finalmente se construye
    }

    // Le pide datos al repository y recibe el DAO para usar sus herramientas
    private val repository by lazy { UsuarioRepository(db.usuarioDao()) }
    // Controla la logica de la interfaz (si aprietas iniciar sesion se inicia)
    private val viewModelVelo by lazy { AuthViewModel(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeloTheme{ // <- aquí va el nombre de TU TEMA, lo puedes encontrar en ui/theme/Theme.kt
                Navegacion(viewModelVelo)
            }
        }
    }
}