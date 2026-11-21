package com.duoc.velo.api;

import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static com.duoc.velo.api.Modelos.*;

@RestController
@RequestMapping("/api/v1")
class ControladorApi {
    private final Cuentas cuentas;
    private final Solicitudes solicitudes;

    ControladorApi(Cuentas cuentas, Solicitudes solicitudes) {
        this.cuentas = cuentas;
        this.solicitudes = solicitudes;
    }

    @GetMapping("/salud")
    Map<String, String> salud() {
        solicitudes.comprobarDisponibilidad();
        return Map.of("estado", "disponible");
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    Usuario registrar(@RequestBody Registro datos) {
        return cuentas.registrar(datos);
    }

    @PostMapping("/auth/login")
    ResponseEntity<Sesion> iniciar(@RequestBody Credenciales datos) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(cuentas.iniciar(datos));
    }

    @PostMapping("/perfil/foto")
    Usuario actualizarFoto(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header,
                           @RequestBody FotoPerfil datos) {
        return cuentas.actualizarFoto(cuentas.autenticar(header), datos);
    }

    @GetMapping("/solicitudes")
    List<SolicitudConOfertas> listar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header) {
        return solicitudes.listar(cuentas.autenticar(header));
    }

    @PostMapping("/solicitudes")
    @ResponseStatus(HttpStatus.CREATED)
    Solicitud crear(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header,
                    @RequestBody DatosSolicitud datos) {
        return solicitudes.crear(cuentas.autenticar(header), datos);
    }

    @GetMapping("/solicitudes/{id}")
    Solicitud consultar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header,
                         @PathVariable String id) {
        return solicitudes.consultar(cuentas.autenticar(header), id);
    }

    @PutMapping("/solicitudes/{id}")
    Solicitud editar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header,
                      @PathVariable String id, @RequestBody DatosSolicitud datos) {
        return solicitudes.editar(cuentas.autenticar(header), id, datos);
    }

    @DeleteMapping("/solicitudes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header,
                 @PathVariable String id) {
        solicitudes.eliminar(cuentas.autenticar(header), id);
    }

    @GetMapping("/solicitudes/{id}/ofertas")
    List<Oferta> ofertas(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header,
                         @PathVariable String id) {
        return solicitudes.ofertas(cuentas.autenticar(header), id);
    }

    @PostMapping("/solicitudes/{id}/ofertas")
    @ResponseStatus(HttpStatus.CREATED)
    Oferta ofertar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header,
                   @PathVariable String id, @RequestBody NuevaOferta datos) {
        return solicitudes.ofertar(cuentas.autenticar(header), id, datos);
    }

    @PostMapping("/solicitudes/{id}/aceptacion")
    Solicitud aceptar(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String header,
                      @PathVariable String id, @RequestBody Aceptacion datos) {
        return solicitudes.aceptar(cuentas.autenticar(header), id, datos);
    }

    @ExceptionHandler(Fallo.class)
    ResponseEntity<Map<String, String>> errorDeDominio(Fallo fallo) {
        return ResponseEntity.status(fallo.estado).cacheControl(CacheControl.noStore())
                .body(Map.of("error", fallo.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> errorDeFormato() {
        return ResponseEntity.badRequest().body(Map.of("error", "Revisa el formato de los datos enviados."));
    }
}
