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

    private val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "velo_db"
        ).build()
    }
    private val repository by lazy { UsuarioRepository(db.usuarioDao()) }
    private val viewModelCanciones by lazy { AuthViewModel(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeloTheme{ // <- aquí va el nombre de TU TEMA, lo puedes encontrar en ui/theme/Theme.kt
                Navegacion(viewModelCanciones)
            }
        }
    }
}