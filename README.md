# Velo App

Velo conecta a productores agrícolas y aplicadores. Un productor publica una solicitud de aplicación con la superficie, el turno, los obstáculos y las fotos del terreno. Un aplicador consulta las solicitudes disponibles y ofrece un precio por hectárea. El productor revisa las ofertas y acepta una.

## Arquitectura

- `Velo/` contiene la app Android en Kotlin y Jetpack Compose. Las pantallas manejan acciones del dispositivo y los ViewModels administran formularios, sesión y estados de carga.
- Los repositorios conectan los ViewModels con Room en modo local o con la API propia mediante Retrofit. Cada modo tiene su propio almacén.
- `backend/` contiene la API Spring Boot y la base H2 persistente. El servidor autentica con tokens temporales y valida el rol y la propiedad en las operaciones protegidas.
- La consulta de clima usa Retrofit con Open-Meteo de forma independiente de la API propia. Desde el formulario de solicitud se abre la pantalla **Clima** para buscar una localidad y consultar sus condiciones y viento.

La app usa la cámara durante el registro, el selector de documentos para adjuntar fotos del terreno y el selector de fotos para editar el perfil. El lanzador utiliza el ícono adaptativo de Velo.

## Ejecutar en desarrollo

1. Abre `Velo/` en Android Studio con JDK 21 y SDK 37.1. Para trabajar únicamente con Room, ejecuta la app con la configuración predeterminada.
2. Inicia el servidor en otra terminal. El servicio escucha en el puerto `8081`.

   ```powershell
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

3. Para probar la app en modo remoto con un teléfono conectado por ADB, configura el reenvío y pasa la URL al proyecto Android.

   ```powershell
   adb reverse tcp:8081 tcp:8081
   cd Velo
   .\gradlew.bat :app:installDebug "-PveloApiUrl=http://127.0.0.1:8081"
   ```

   En el emulador oficial puedes usar `-PveloApiUrl=http://10.0.2.2:8081`. Para un servidor externo, usa su URL HTTPS. La app agrega el prefijo `/api/v1`.

4. Registra cuentas distintas para productor y aplicador. Con el productor, publica una solicitud, edítala mientras esté libre de ofertas y prueba eliminar otra solicitud de prueba. Con el aplicador, abre la solicitud activa y envía una oferta. En la cuenta del productor, revisa las ofertas y acepta una.

## Endpoints

La app llama a la API propia bajo `/api/v1`. El [README del backend](backend/README.md) incluye el contrato HTTP y la configuración de la base de datos.

| Método | Ruta | Uso |
| --- | --- | --- |
| POST | `/auth/register` | Crear cuenta |
| POST | `/auth/login` | Iniciar sesión |
| GET | `/solicitudes` | Listar solicitudes |
| POST | `/solicitudes` | Publicar solicitud |
| GET | `/solicitudes/{id}` | Consultar el detalle |
| PUT | `/solicitudes/{id}` | Editar una solicitud propia sin ofertas |
| DELETE | `/solicitudes/{id}` | Eliminar una solicitud propia sin ofertas |
| GET | `/solicitudes/{id}/ofertas` | Consultar ofertas |
| POST | `/solicitudes/{id}/ofertas` | Enviar oferta |
| POST | `/solicitudes/{id}/aceptacion` | Aceptar oferta |
| POST | `/perfil/foto` | Cambiar foto de perfil |

La consulta externa usa [Geocoding API](https://open-meteo.com/en/docs/geocoding-api) con `GET /v1/search` para buscar localidades y [Weather Forecast API](https://open-meteo.com/en/docs) con `GET /v1/forecast` para mostrar el tiempo actual.

## Pruebas

Desde `Velo/`, ejecuta las pruebas unitarias y genera el informe de cobertura de Android.

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:createDebugUnitTestCoverageReport
```

El informe se genera bajo `Velo/app/build/reports/coverage/`.

Desde `backend/`, ejecuta las pruebas HTTP y de persistencia del servicio Spring Boot.

```powershell
.\mvnw.cmd test
```

[Tablero VeloApp en Trello](https://trello.com/b/gBI1NmLN)

## Autor

- Vicente Castro
