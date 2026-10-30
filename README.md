# Velo app (Proyecto de Desarrollo de Aplicaciones Móviles)
Velo es una aplicación móvil desarrollada en **Jetpack Compose**, esta app esta pensada para la empresa **TecMundo** la cual se dedica a la venta de tecnologías para la agricultura y silvicultura, especialmente drones, robots y tecnologías de agricultura de precisión.

## Antes de Comenzar
Para ejecutar el programa se recomienda el uso de **Android Studio** junto con un **emulador de Android** (preferiblemente el modelo **Pixel 7**) o un dispositivo físico. Finalmente clonar el proyecto y sincronizarlo de manera correcta.

## Modelos

Los modelos de la app están configuradas con **Room** (Es una base de datos local del emulador o dispositivo), la cual nos ayudara a crear los modelos de forma automática a la base de datos.

Para hacer que un modelo se transforme a una tabla de base de datos se utiliza **@Entity** que esta nos dice que el modelo será una entidad y se mapeara automáticamente como una tabla en la base de datos.

>---
>[Usuario]
> - rut(Primary Key) : String,
> - nombre : String,
> - apellido : String,
> - email : String,
> - telefono : String,
> - password : String,
> - rol : Rol
>
>---

## DAOs

El **DAO o Data Access Object** es donde se define todos los metodos SQL que tendra nuestro proyecto para que luego Room pueda "leerlo" e implementarlo automaticamente en la base de datos. Estos archivos se declaran con @Dao para que el programa detecte que es un archivo de este tipo.

Ejemplo:

```Kotlin
    // Si en el caso de que el rut ya exista se remplazara los datos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrarUsuario(usuario: Usuario)
```

## AppDatabase

Es el encargado de conectar todas las tablas (que estén definidas como @Entity) también accede a los DAOs del proyecto y además se asegura que haya solo una única conexión a la base de datos.

Ejemplo de nuestro proyecto:

```Kotlin
@Database(entities = [Usuario::class], version = 2) // Define las entidades (tablas) y la versión de la BD
abstract class AppDatabase : RoomDatabase() {       // Clase abstracta central que extiende de RoomDatabase
    
    // Provee la instancia del DAO para acceder a las consultas SQL
    abstract fun usuarioDao(): UsuarioDao
}
```

## Repository

Contiene la lógica de negocio y las reglas de procesamiento de datos de manera organizada, aislando las fuentes de datos para garantizar la escalabilidad y mantenibilidad del código a futuro.

## ViewModel

Este es el puente entre la interfaz (**UI**) y la lógica del proyecto (**repository**), reteniendo el estado de la UI frente a cambios de configuración y aislando los eventos de usuario mediante corrutinas de ciclo de vida (utilizando viewModelScope).

## UI

Contiene la interfaz de usuario gráfica desarrollada con **Jetpack Compose**. Agrupa las pantallas y componentes visuales (formularios, botones, textos y estilos) y reacciona en tiempo real a los cambios de estado provenientes del **ViewModel**

## Autores
- Javier Fuentealba
- Vicente Castro
