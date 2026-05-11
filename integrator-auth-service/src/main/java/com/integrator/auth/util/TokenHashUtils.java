package com.integrator.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class TokenHashUtils {

    public static String hashedToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                                       .digest(token.getBytes(StandardCharsets.UTF_8)
                    );
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(
                        String.format("%02x", b)
                );
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
