package com.ima.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Length-prefixed components prevent delimiter collisions. */
public final class CacheKey {
    private CacheKey() {
    }

    public static String report(String applicationId, String type, String canonicalParameters) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            add(digest, applicationId);
            add(digest, type);
            add(digest, canonicalParameters);
            byte[] bytes = digest.digest();
            StringBuilder key = new StringBuilder("ima:report:v1:");
            for (byte value : bytes) {
                key.append(Character.forDigit((value >>> 4) & 15, 16));
                key.append(Character.forDigit(value & 15, 16));
            }
            return key.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException("SHA-256 unavailable", failure);
        }
    }

    private static void add(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
