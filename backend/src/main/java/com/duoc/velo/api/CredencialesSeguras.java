package com.duoc.velo.api;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

final class CredencialesSeguras {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int ITERACIONES = 600_000;

    private CredencialesSeguras() {}

    static String hashClave(String password) {
        byte[] sal = new byte[16];
        ALEATORIO.nextBytes(sal);
        return HexFormat.of().formatHex(sal) + ":" + HexFormat.of().formatHex(derivar(password, sal));
    }

    static boolean coincide(String password, String hash) {
        String[] partes = hash.split(":", 2);
        var hex = HexFormat.of();
        return MessageDigest.isEqual(derivar(password, hex.parseHex(partes[0])), hex.parseHex(partes[1]));
    }

    static String token() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("SHA-256 no está disponible", error);
        }
    }

    private static byte[] derivar(String password, byte[] sal) {
        var llave = new PBEKeySpec(password.toCharArray(), sal, ITERACIONES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(llave).getEncoded();
        } catch (GeneralSecurityException error) {
            throw new IllegalStateException("PBKDF2 no está disponible", error);
        } finally {
            llave.clearPassword();
        }
    }
}
