package com.duoc.velo.api;

import org.springframework.http.HttpStatus;

final class Fallo extends RuntimeException {
    final HttpStatus estado;

    Fallo(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    static Fallo entrada(String mensaje) { return new Fallo(HttpStatus.BAD_REQUEST, mensaje); }
    static Fallo autenticacion() { return new Fallo(HttpStatus.UNAUTHORIZED, "Sesión inválida o credenciales incorrectas."); }
    static Fallo acceso() { return new Fallo(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción."); }
    static Fallo solicitudNoEncontrada() { return new Fallo(HttpStatus.NOT_FOUND, "No se encontró la solicitud."); }
    static Fallo conflicto(String mensaje) { return new Fallo(HttpStatus.CONFLICT, mensaje); }
}
