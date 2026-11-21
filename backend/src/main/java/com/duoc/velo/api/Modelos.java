package com.duoc.velo.api;

final class Modelos {
    private Modelos() {}

    enum Rol { PRODUCTOR, APLICADOR }

    record Registro(String rut, String nombre, String apellido, String email, String telefono,
                    String password, Rol rol, String fotoPerfil) {}
    record Credenciales(String identificador, String password) {}
    record FotoPerfil(String fotoPerfil) {}
    record Usuario(String rut, String nombre, String apellido, String email, String telefono,
                   Rol rol, String fotoPerfil) {}
    record Cuenta(Usuario usuario, String clave) {}
    record Sesion(String token, Usuario usuario) {}

    record DatosSolicitud(String id, String producto, Double hectareas, Boolean agua, String turno,
                          String obstaculos, String comentario, String fotos) {}
    record Solicitud(String id, String productor, String producto, double hectareas, boolean agua,
                     String turno, String obstaculos, String comentario, String fotos,
                     long creada, String aceptada) {}
    record SolicitudConOfertas(Solicitud solicitud, int cantidadOfertas) {}
    record NuevaOferta(Long precio) {}
    record Oferta(String solicitud, String aplicador, String nombre, String telefono, long precio) {}
    record Aceptacion(String aplicador) {}
}
