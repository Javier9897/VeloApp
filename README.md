# Velo App

> Esta rama ejecuta Velo en modo local con Room. Las secciones de API y firma documentan los pasos de la versión de Experiencia 3 en preparación.

Velo conecta a productores agrícolas y aplicadores. Un productor puede publicar una solicitud de aplicación con su superficie, turno, obstáculos y fotos. Un aplicador consulta las solicitudes disponibles y ofrece un precio por hectárea. El productor revisa las ofertas y acepta una.

## Arquitectura

- `Velo/` contiene la aplicación Android en Kotlin con Jetpack Compose y navegación por rol.
- `ui` presenta las pantallas, observa el estado y gestiona acciones propias del dispositivo. Los ViewModels administran formularios, sesión, carga y errores según su responsabilidad.
- Los repositorios conectan los ViewModels con Room en modo local o con la API REST cuando se configura un servidor. Ambos modos usan almacenes independientes.
- `backend/` guarda cuentas, solicitudes, ofertas y fotos en SQLite. La API autentica mediante un token temporal y comprueba el rol y la propiedad en cada operación protegida.

La cámara se usa para tomar una foto durante el registro. El selector de archivos de Android permite adjuntar fotos del terreno con acceso persistente. También se puede cambiar la foto de perfil desde el selector de fotos. El lanzador usa un ícono adaptativo propio.

## Configuración y prueba

1. Abre `Velo/` en Android Studio con JDK 21 y SDK 37.1.
2. Para usar la API de desarrollo, inicia el servidor desde la raíz del repositorio.

   ```powershell
   python backend/velo_api.py --port 8080 --db backend/data/velo.sqlite3
   ```

3. En otra terminal, conecta el teléfono por ADB y habilita el reenvío al servidor del equipo.

   ```powershell
   adb reverse tcp:8080 tcp:8080
   cd Velo
   .\gradlew.bat :app:assembleDebug "-PveloApiUrl=http://127.0.0.1:8080"
   ```

   En el emulador oficial también puedes usar `-PveloApiUrl=http://10.0.2.2:8080`. Si omites la propiedad, la app usa Room sin conexión al servidor.

4. Registra un productor y un aplicador con cuentas distintas. Con el productor publica una solicitud. Cambia al aplicador, abre la solicitud y envía una oferta. Vuelve a la cuenta del productor y acepta la oferta.

## API

Android consume nueve operaciones en siete rutas distintas bajo `/api/v1`. El servidor incluye una ruta adicional de diagnóstico.

| Método | Ruta | Uso en la app |
| --- | --- | --- |
| POST | `/auth/register` | Crear cuenta |
| POST | `/auth/login` | Iniciar sesión |
| GET | `/solicitudes` | Listar solicitudes |
| POST | `/solicitudes` | Publicar solicitud |
| GET | `/solicitudes/{id}` | Consultar el detalle |
| GET | `/solicitudes/{id}/ofertas` | Consultar ofertas |
| POST | `/solicitudes/{id}/ofertas` | Enviar oferta |
| POST | `/solicitudes/{id}/aceptacion` | Aceptar oferta |
| POST | `/perfil/foto` | Cambiar foto de perfil |

La documentación del directorio backend describirá los datos de las rutas, el puerto y el volumen persistente para el despliegue.

## Pruebas y APK firmado

Pruebas unitarias Android desde `Velo/`.

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Pruebas de integración HTTP desde `backend/`.

```powershell
python -m unittest -v
```

Para un APK de entrega, configura una API HTTPS accesible desde el teléfono y un almacén de claves de release. Completa `VELO_SIGNING_STORE_FILE`, `VELO_SIGNING_STORE_PASSWORD`, `VELO_SIGNING_KEY_ALIAS` y `VELO_SIGNING_KEY_PASSWORD` con variables de entorno locales.

```powershell
.\gradlew.bat :app:assembleRelease "-PveloApiUrl=https://tu-servidor.example"
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

El APK firmado se genera en `Velo/app/build/outputs/apk/release/app-release.apk`. Conserva el almacén de claves fuera del repositorio para firmar futuras actualizaciones con el mismo certificado.

[Tablero VeloApp en Trello](https://trello.com/b/gBI1NmLN)

## Autor

- Vicente Castro
