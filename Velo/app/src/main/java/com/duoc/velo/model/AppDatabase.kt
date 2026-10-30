package com.duoc.velo.model

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Usuario::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // El campo rol se agregó mientras la base todavía estaba en versión 1.
                val tieneRol = db.query("PRAGMA table_info(usuarios)").use { columnas ->
                    val indiceNombre = columnas.getColumnIndexOrThrow("name")
                    generateSequence { if (columnas.moveToNext()) columnas.getString(indiceNombre) else null }
                        .any { it == "rol" }
                }
                if (!tieneRol) {
                    db.execSQL("ALTER TABLE usuarios ADD COLUMN rol TEXT NOT NULL DEFAULT 'PRODUCTOR'")
                }
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE usuarios ADD COLUMN fotoPerfil BLOB")
            }
        }
    }
}
