package com.duoc.velo.model

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity
data class Solicitud(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val productor: String,
    val producto: String,
    val hectareas: Double,
    val agua: Boolean,
    val turno: String,
    val obstaculos: String,
    val comentario: String,
    val fotos: String,
    val creada: Long = System.currentTimeMillis(),
    val aceptada: String? = null
)

@Entity(primaryKeys = ["solicitud", "aplicador"])
data class Oferta(
    val solicitud: String,
    val aplicador: String,
    val nombre: String,
    val telefono: String,
    val precio: Long
)

data class SolicitudConOfertas(
    @Embedded val solicitud: Solicitud,
    val cantidadOfertas: Int
)

@Dao
abstract class SolicitudesDao {
    @Query(
        """
        SELECT Solicitud.*, (SELECT COUNT(*) FROM Oferta WHERE Oferta.solicitud = Solicitud.id) AS cantidadOfertas
        FROM Solicitud
        WHERE (:esProductor AND productor = :rut)
           OR (NOT :esProductor AND productor <> :rut AND (aceptada IS NULL OR aceptada = :rut))
        ORDER BY creada DESC, id
        """
    )
    abstract fun solicitudes(rut: String, esProductor: Boolean): Flow<List<SolicitudConOfertas>>

    @Query("SELECT * FROM Solicitud WHERE id = :id")
    abstract fun solicitud(id: String): Flow<Solicitud?>

    @Query("SELECT * FROM Oferta WHERE solicitud = :id ORDER BY precio, nombre")
    abstract fun ofertas(id: String): Flow<List<Oferta>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun crear(solicitud: Solicitud)

    @Query("SELECT * FROM Solicitud WHERE id = :id")
    protected abstract suspend fun buscar(id: String): Solicitud?

    @Insert
    protected abstract suspend fun insertar(oferta: Oferta)

    @Transaction
    open suspend fun ofertar(oferta: Oferta) {
        val solicitud = requireNotNull(buscar(oferta.solicitud))
        require(solicitud.aceptada == null && solicitud.productor != oferta.aplicador && oferta.precio > 0)
        insertar(oferta)
    }

    @Query(
        """
        UPDATE Solicitud SET aceptada = :aplicador
        WHERE id = :id AND productor = :productor AND aceptada IS NULL
        AND EXISTS (SELECT 1 FROM Oferta WHERE solicitud = :id AND aplicador = :aplicador)
        """
    )
    abstract suspend fun aceptar(id: String, productor: String, aplicador: String): Int
}

@Database(entities = [Solicitud::class, Oferta::class], version = 1, exportSchema = false)
abstract class SolicitudesDatabase : RoomDatabase() {
    abstract fun dao(): SolicitudesDao

    companion object {
        @Volatile
        private var instancia: SolicitudesDatabase? = null

        fun obtener(context: Context): SolicitudesDatabase = instancia ?: synchronized(this) {
            instancia ?: Room.databaseBuilder(
                context.applicationContext,
                SolicitudesDatabase::class.java,
                "solicitudes.db"
            ).build().also { instancia = it }
        }
    }
}
