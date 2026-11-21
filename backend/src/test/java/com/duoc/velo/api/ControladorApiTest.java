package com.duoc.velo.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:velo-http-pruebas;DB_CLOSE_DELAY=-1")
class ControladorApiTest {
    private static final String CLAVE = "clave-de-pruebas-1";
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+X7XcAAAAASUVORK5CYII=";
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @LocalServerPort
    private int puerto;
    @Autowired
    private JdbcTemplate sql;

    @Test
    void registroSesionYPerfilProtegenCredencialesYValidanLaFoto() throws Exception {
        assertThat(pedir("GET", "/salud", null, null, 200).get("estado").asText()).isEqualTo("disponible");
        pedir("GET", "/solicitudes", null, null, 401);

        var entrada = registro("12.345.678-K", "cuenta@ejemplo.cl", "PRODUCTOR");
        JsonNode usuario = pedir("POST", "/auth/register", entrada, null, 201);
        assertThat(usuario.get("rut").asText()).isEqualTo("12345678-K");
        assertThat(usuario.has("password") || usuario.has("clave")).isFalse();
        assertThat(sql.queryForObject("SELECT clave FROM usuarios WHERE rut = ?", String.class, "12345678-K"))
                .doesNotContain(CLAVE);

        pedir("POST", "/auth/register", entrada, null, 409);
        var sinRol = new HashMap<>(entrada);
        sinRol.put("rut", "12345679-1");
        sinRol.remove("rol");
        pedir("POST", "/auth/register", sinRol, null, 400);
        pedir("POST", "/auth/login", Map.of("identificador", "CUENTA@EJEMPLO.CL", "password", "erronea"), null, 401);

        String token = iniciar("CUENTA@EJEMPLO.CL");
        assertThat(iniciar("12.345.678-k")).isNotEqualTo(token);
        pedir("GET", "/solicitudes", null, "F".repeat(43), 401);
        pedir("POST", "/perfil/foto", Map.of("fotoPerfil", "bm8gZXMgdW5hIGZvdG8="), token, 400);
        assertThat(pedir("POST", "/perfil/foto", Map.of("fotoPerfil", PNG), token, 200)
                .get("fotoPerfil").asText()).isEqualTo(PNG);
        assertThat(pedir("POST", "/auth/login", Map.of("identificador", "12345678-K", "password", CLAVE),
                null, 200).get("usuario").get("fotoPerfil").asText()).isEqualTo(PNG);

        sql.update("UPDATE sesiones SET expira = 0 WHERE token_hash = ?", CredencialesSeguras.hashToken(token));
        pedir("GET", "/solicitudes", null, token, 401);
    }

    @Test
    void flujoCrudDesdeProductorConValidacionYControlDePropiedad() throws Exception {
        String productor = cuenta("11111111-1", "productor@ejemplo.cl", "PRODUCTOR");
        String otroProductor = cuenta("22222222-2", "otro@ejemplo.cl", "PRODUCTOR");
        String aplicador = cuenta("33333333-3", "aplicador@ejemplo.cl", "APLICADOR");
        String id = UUID.randomUUID().toString();
        String ruta = "/solicitudes/" + id;
        var datos = solicitud(id, 11.5, "Lote norte");
        datos.put("fotos", "data:image/png;base64," + PNG);

        pedir("POST", "/solicitudes", datos, aplicador, 403);
        var invalida = new HashMap<>(datos);
        invalida.put("hectareas", -1);
        pedir("POST", "/solicitudes", invalida, productor, 400);
        pedir("POST", "/solicitudes", datos, productor, 201);
        assertThat(pedir("POST", "/solicitudes", datos, productor, 201).get("id").asText()).isEqualTo(id);
        pedir("POST", "/solicitudes", datos, otroProductor, 409);
        assertThat(pedir("GET", "/solicitudes", null, productor, 200).get(0).get("solicitud")
                .get("fotos").asText()).isEmpty();
        assertThat(pedir("GET", ruta, null, aplicador, 200).get("fotos").asText())
                .isEqualTo("data:image/png;base64," + PNG);
        pedir("GET", ruta, null, otroProductor, 404);

        var edicion = solicitud(null, 14.0, "Lote sur");
        pedir("PUT", ruta, edicion, aplicador, 403);
        pedir("PUT", ruta, edicion, otroProductor, 404);
        assertThat(pedir("PUT", ruta, edicion, productor, 200).get("hectareas").asDouble()).isEqualTo(14.0);
        assertThat(pedir("GET", ruta, null, aplicador, 200).get("comentario").asText()).isEqualTo("Lote sur");
        pedir("DELETE", ruta, null, otroProductor, 404);
        pedir("DELETE", ruta, null, aplicador, 403);
        pedir("DELETE", ruta, null, productor, 204);
        pedir("GET", ruta, null, aplicador, 404);
        assertThat(pedir("GET", "/solicitudes", null, productor, 200).size()).isZero();
    }

    @Test
    void ofertasSonPrivadasYDosAceptacionesConcurrentesTienenUnGanador() throws Exception {
        String productor = cuenta("44444444-4", "dueno@ejemplo.cl", "PRODUCTOR");
        String aplicador = cuenta("55555555-5", "aplicador2@ejemplo.cl", "APLICADOR");
        String otroAplicador = cuenta("66666666-6", "aplicador3@ejemplo.cl", "APLICADOR");
        String id = UUID.randomUUID().toString();
        String ruta = "/solicitudes/" + id;
        pedir("POST", "/solicitudes", solicitud(id, 5.0, "Terreno disponible"), productor, 201);
        String ofertas = ruta + "/ofertas";
        String aceptacion = ruta + "/aceptacion";

        pedir("POST", ofertas, Map.of("precio", 13000), productor, 403);
        pedir("POST", ofertas, Map.of("precio", 0), aplicador, 400);
        pedir("POST", ofertas, Map.of("precio", 13000), aplicador, 201);
        pedir("POST", ofertas, Map.of("precio", 13000), aplicador, 201);
        pedir("POST", ofertas, Map.of("precio", 16000), aplicador, 409);
        pedir("POST", ofertas, Map.of("precio", 15000), otroAplicador, 201);
        assertThat(pedir("GET", ofertas, null, aplicador, 200).size()).isEqualTo(1);
        assertThat(pedir("GET", ofertas, null, productor, 200).size()).isEqualTo(2);
        assertThat(pedir("GET", "/solicitudes", null, productor, 200).get(0)
                .get("cantidadOfertas").asInt()).isEqualTo(2);
        pedir("PUT", ruta, solicitud(null, 15.0, "Modificado"), productor, 409);
        pedir("DELETE", ruta, null, productor, 409);
        pedir("POST", aceptacion, Map.of("aplicador", "55555555-5"), aplicador, 403);

        try (var hilos = Executors.newVirtualThreadPerTaskExecutor()) {
            var primera = hilos.submit(() -> enviar("POST", aceptacion, Map.of("aplicador", "55555555-5"), productor));
            var segunda = hilos.submit(() -> enviar("POST", aceptacion, Map.of("aplicador", "66666666-6"), productor));
            assertThat(new int[]{primera.get().statusCode(), segunda.get().statusCode()})
                    .containsExactlyInAnyOrder(200, 409);
        }

        String rutGanador = pedir("GET", ruta, null, productor, 200).get("aceptada").asText();
        pedir("POST", aceptacion, Map.of("aplicador", rutGanador), productor, 200);
        String ganador = rutGanador.equals("55555555-5") ? aplicador : otroAplicador;
        String perdedor = rutGanador.equals("55555555-5") ? otroAplicador : aplicador;
        pedir("GET", ruta, null, ganador, 200);
        pedir("GET", ruta, null, perdedor, 404);
    }

    private String cuenta(String rut, String email, String rol) throws Exception {
        pedir("POST", "/auth/register", registro(rut, email, rol), null, 201);
        return iniciar(email);
    }

    private String iniciar(String identificador) throws Exception {
        return pedir("POST", "/auth/login", Map.of("identificador", identificador, "password", CLAVE), null, 200)
                .get("token").asText();
    }

    private static Map<String, Object> registro(String rut, String email, String rol) {
        return Map.of("rut", rut, "nombre", "Usuario", "apellido", "De Prueba", "email", email,
                "telefono", "+56 9 1234 5678", "password", CLAVE, "rol", rol);
    }

    private static Map<String, Object> solicitud(String id, double hectareas, String comentario) {
        Map<String, Object> datos = new HashMap<>(Map.of("producto", "Líquido", "hectareas", hectareas,
                "agua", true, "turno", "Diurna", "obstaculos", "Antenas", "comentario", comentario, "fotos", ""));
        if (id != null) datos.put("id", id);
        return datos;
    }

    private JsonNode pedir(String metodo, String ruta, Object datos, String token, int estado) throws Exception {
        var respuesta = enviar(metodo, ruta, datos, token);
        assertThat(respuesta.statusCode()).as(metodo + " " + ruta + " " + respuesta.body()).isEqualTo(estado);
        if (estado == 204) return null;
        var cuerpo = json.readTree(respuesta.body());
        if (estado >= 400) assertThat(cuerpo.hasNonNull("error")).isTrue();
        return cuerpo;
    }

    private HttpResponse<String> enviar(String metodo, String ruta, Object datos, String token) throws Exception {
        var peticion = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + puerto + "/api/v1" + ruta))
                .header("Accept", "application/json");
        if (token != null) peticion.header("Authorization", "Bearer " + token);
        if (datos == null) {
            peticion.method(metodo, HttpRequest.BodyPublishers.noBody());
        } else {
            peticion.header("Content-Type", "application/json")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(datos)));
        }
        return cliente.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
    }
}
