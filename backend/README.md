# API de Velo

Backend en Spring Boot 4.0.6 y Java 21. Persiste cuentas, sesiones, solicitudes y ofertas en H2. Las tablas se crean al iniciar el servidor.

## Ejecutar localmente

Desde `backend/`, con JDK 21 y el Maven Wrapper incluido.

```powershell
.\mvnw.cmd spring-boot:run
```

El servidor escucha en `http://127.0.0.1:8081` y escribe la base en `data/velo.mv.db`. Utiliza `PORT`, `VELO_HOST`, `VELO_DB_URL`, `VELO_DB_USER` y `VELO_DB_PASSWORD` para configurar el entorno. Para servir a teléfonos fuera del equipo de desarrollo, configura un proxy HTTPS y conserva el archivo de la base entre reinicios.

```powershell
Invoke-RestMethod http://127.0.0.1:8081/api/v1/salud
.\mvnw.cmd test
```

Las pruebas HTTP usan una base en memoria. Ejercitan cuentas y sesiones, foto de perfil, CRUD, aislamiento de roles y dos aceptaciones concurrentes.

## Contrato JSON

Todas las rutas tienen el prefijo `/api/v1`. Las rutas protegidas reciben `Authorization: Bearer <token>`. El token vence en 24 horas. Los errores de validación y acceso devuelven `{"error":"mensaje"}`.

| Método | Ruta | Datos o resultado |
|---|---|---|
| GET | `/salud` | Comprueba la disponibilidad de la base de datos. |
| POST | `/auth/register` | `rut`, `nombre`, `apellido`, `email`, `telefono`, `password`, `rol` y `fotoPerfil` opcional. |
| POST | `/auth/login` | `identificador` y `password`. Devuelve `token` y `usuario`. |
| POST | `/perfil/foto` | `fotoPerfil` en Base64. |
| GET | `/solicitudes` | Lista visible para el rol. Cada elemento incluye la solicitud y `cantidadOfertas`. |
| POST | `/solicitudes` | Publica una solicitud con `id` UUID, `producto`, `hectareas`, `agua`, `turno`, `obstaculos`, `comentario` y `fotos`. |
| GET | `/solicitudes/{id}` | Consulta la solicitud y sus fotos. |
| PUT | `/solicitudes/{id}` | Modifica los datos de una solicitud propia sin ofertas. El cuerpo omite `id`. |
| DELETE | `/solicitudes/{id}` | Elimina una solicitud propia sin ofertas. Devuelve HTTP 204. |
| GET | `/solicitudes/{id}/ofertas` | Devuelve las ofertas al productor y la propia al aplicador. |
| POST | `/solicitudes/{id}/ofertas` | Registra `precio` por hectárea como entero en CLP. |
| POST | `/solicitudes/{id}/aceptacion` | El productor acepta la oferta del `aplicador` indicado por RUT. |

La API admite fotos JPEG, PNG y WebP. `fotos` contiene hasta seis URL de datos Base64 separadas por saltos de línea. Los listados omiten esas fotos para reducir el tráfico.
