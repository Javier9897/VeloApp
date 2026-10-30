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



## AppDatabase



## Autores
- Javier Fuentealba
- Vicente Castro
