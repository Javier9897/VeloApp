package com.duoc.velo.api;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.duoc.velo.api.Modelos.DatosSolicitud;

final class Validacion {
    private static final Pattern RUT = Pattern.compile("[0-9]{1,8}-[0-9K]");
    private static final Pattern CORREO = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final int LIMITE_FOTO_PERFIL = 1024 * 1024;
    private static final int LIMITE_FOTOS_TERRENO = 6 * 1024 * 1024;

    private Validacion() {}

    static String texto(String valor, int limite, String campo, boolean obligatorio) {
        String texto = valor == null ? "" : valor.strip();
        if ((obligatorio && texto.isBlank()) || texto.length() > limite) {
            throw Fallo.entrada("Revisa el campo " + campo + ".");
        }
        return texto;
    }

    static String rutNormalizado(String valor) {
        return valor.replace(".", "").replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }

    static String rut(String valor) {
        String normalizado = rutNormalizado(texto(valor, 20, "RUT", true));
        if (!RUT.matcher(normalizado).matches()) throw Fallo.entrada("Ingresa un RUT con dígito verificador.");
        return normalizado;
    }

    static String email(String valor) {
        String email = texto(valor, 254, "correo", true).toLowerCase(Locale.ROOT);
        if (!CORREO.matcher(email).matches()) throw Fallo.entrada("Ingresa un correo válido.");
        return email;
    }

    static String id(String valor) {
        try {
            UUID uuid = UUID.fromString(valor);
            if (uuid.toString().equals(valor)) return valor;
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // Se responde con el mismo error para todos los identificadores inválidos.
        }
        throw Fallo.entrada("Identificador de solicitud inválido.");
    }

    static DatosSolicitud solicitud(DatosSolicitud datos, String id) {
        if (!"Granulado".equals(datos.producto()) && !"Líquido".equals(datos.producto())) {
            throw Fallo.entrada("Selecciona un producto válido.");
        }
        if (!"Diurna".equals(datos.turno()) && !"Nocturna".equals(datos.turno())) {
            throw Fallo.entrada("Selecciona un turno válido.");
        }
        if (datos.hectareas() == null || !Double.isFinite(datos.hectareas())
                || datos.hectareas() <= 0 || datos.hectareas() > 1_000_000) {
            throw Fallo.entrada("Ingresa una superficie válida.");
        }
        if (datos.agua() == null) throw Fallo.entrada("Indica si hay agua en el terreno.");
        return new DatosSolicitud(id, datos.producto(), datos.hectareas(), datos.agua(), datos.turno(),
                texto(datos.obstaculos(), 300, "obstáculos", false),
                texto(datos.comentario(), 1000, "comentario", false), fotosTerreno(datos.fotos()));
    }

    static String fotoPerfil(String imagen) {
        if (imagen == null) return null;
        String foto = imagen.strip();
        tipoImagen(decodificarFoto(foto, LIMITE_FOTO_PERFIL));
        return foto;
    }

    private static String fotosTerreno(String datos) {
        if (datos == null || datos.isBlank()) return "";
        if (datos.length() > 8_389_000) throw Fallo.entrada("Las fotos del terreno exceden el tamaño permitido.");
        var fotos = new ArrayList<String>();
        int total = 0;
        for (String linea : datos.strip().split("\\R")) {
            if (fotos.size() == 6) throw Fallo.entrada("Se permiten hasta seis fotos por solicitud.");
            String foto = linea.strip();
            int separador = foto.indexOf(',');
            if (separador < 0) throw Fallo.entrada("Una foto del terreno es inválida.");
            byte[] bytes = decodificarFoto(foto.substring(separador + 1), LIMITE_FOTOS_TERRENO - total);
            String encabezado = "data:image/" + tipoImagen(bytes) + ";base64";
            if (!foto.startsWith(encabezado + ",")) throw Fallo.entrada("El formato de una foto es inválido.");
            total += bytes.length;
            fotos.add(foto);
        }
        return String.join("\n", fotos);
    }

    private static byte[] decodificarFoto(String foto, int limite) {
        if (foto.length() > ((limite + 2) / 3) * 4) throw Fallo.entrada("La imagen excede el tamaño permitido.");
        try {
            byte[] bytes = Base64.getDecoder().decode(foto);
            if (bytes.length > limite) throw Fallo.entrada("La imagen excede el tamaño permitido.");
            return bytes;
        } catch (IllegalArgumentException error) {
            throw Fallo.entrada("La imagen no contiene datos Base64 válidos.");
        }
    }

    private static String tipoImagen(byte[] bytes) {
        if (coinciden(bytes, 0, 0xff, 0xd8, 0xff)) return "jpeg";
        if (coinciden(bytes, 0, 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) return "png";
        if (coinciden(bytes, 0, 'R', 'I', 'F', 'F') && coinciden(bytes, 8, 'W', 'E', 'B', 'P')) return "webp";
        throw Fallo.entrada("Selecciona una imagen JPEG, PNG o WebP.");
    }

    private static boolean coinciden(byte[] bytes, int inicio, int... firma) {
        if (bytes.length < inicio + firma.length) return false;
        for (int i = 0; i < firma.length; i++) {
            if ((bytes[inicio + i] & 0xff) != firma[i]) return false;
        }
        return true;
    }
}
