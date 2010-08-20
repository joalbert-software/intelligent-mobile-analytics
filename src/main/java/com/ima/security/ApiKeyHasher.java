package com.ima.security;

import com.ima.domain.Checks;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** High-entropy bearer keys are stored as SHA-256 digests, never plaintext. */
public final class ApiKeyHasher {
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String digest(String key) {
        Checks.required(key, "key");
        if (key.length() < 32 || key.length() > 256) {
            throw new IllegalArgumentException("API key has invalid length");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(key.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(64);
            for (byte value : hash) {
                output.append(Character.forDigit((value >>> 4) & 15, 16));
                output.append(Character.forDigit(value & 15, 16));
            }
            return output.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public boolean matches(String key, String expectedDigest) {
        if (key == null || key.length() < 32 || key.length() > 256 || expectedDigest == null) {
            return false;
        }
        return MessageDigest.isEqual(digest(key).getBytes(StandardCharsets.US_ASCII),
                expectedDigest.getBytes(StandardCharsets.US_ASCII));
    }
}
