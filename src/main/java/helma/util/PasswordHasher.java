/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 */

package helma.util;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * Modern password hashing for Helma. New hashes use PBKDF2-HMAC-SHA256 with a
 * per-password random salt and a tunable iteration count, requiring no external
 * dependencies (the algorithm ships with the JDK).
 *
 * <p>{@link #hash} produces a self-describing string that carries everything
 * {@link #verify} needs:</p>
 *
 * <pre>$pbkdf2-sha256$&lt;iterations&gt;$&lt;base64-salt&gt;$&lt;base64-hash&gt;</pre>
 *
 * <p>The format uses {@code $} as its field separator and never contains
 * {@code :}, so hashes are safe to store in Helma's colon-separated
 * {@code passwd} files.</p>
 *
 * <p>{@link #verify} additionally accepts two legacy formats so existing
 * credentials keep working: bare 32-character MD5 hex digests and Unix
 * {@code crypt(3)} hashes. Callers can use {@link #needsRehash} to detect
 * legacy or weaker hashes and transparently upgrade them on next login.</p>
 */
public final class PasswordHasher {

    private static final String PREFIX = "$pbkdf2-sha256$";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    // OWASP-recommended iteration count for PBKDF2-HMAC-SHA256.
    private static final int DEFAULT_ITERATIONS = 210000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /**
     * Hash a plaintext password into a self-describing PBKDF2 string suitable
     * for storage.
     *
     * @param password the plaintext password
     * @return the encoded hash, or null if password is null
     */
    public static String hash(String password) {
        if (password == null) {
            return null;
        }
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] digest = pbkdf2(password, salt, DEFAULT_ITERATIONS, HASH_BITS);

        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIX + DEFAULT_ITERATIONS + "$"
                + b64.encodeToString(salt) + "$"
                + b64.encodeToString(digest);
    }

    /**
     * Verify a plaintext password against a stored hash. Recognizes the modern
     * PBKDF2 format as well as legacy MD5-hex and Unix crypt hashes.
     *
     * @param password the plaintext password to check
     * @param stored the stored hash to check against
     * @return true if the password matches
     */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }

        if (stored.startsWith(PREFIX)) {
            try {
                String[] parts = stored.split("\\$");
                // parts[0] is empty (leading $), [1]=pbkdf2-sha256, [2]=iterations, [3]=salt, [4]=hash
                int iterations = Integer.parseInt(parts[2]);
                Base64.Decoder b64 = Base64.getDecoder();
                byte[] salt = b64.decode(parts[3]);
                byte[] expected = b64.decode(parts[4]);
                byte[] actual = pbkdf2(password, salt, iterations, expected.length * 8);
                return MessageDigest.isEqual(expected, actual);
            } catch (Exception malformed) {
                return false;
            }
        }

        // Legacy: bare 32-char MD5 hex digest
        if (isMd5Hex(stored)) {
            byte[] expected = stored.toLowerCase().getBytes();
            byte[] actual = DigestUtils.md5Hex(password).getBytes();
            return MessageDigest.isEqual(expected, actual);
        }

        // Legacy: Unix crypt(3). The salt is the first two characters of the hash.
        try {
            String cryptpw = Crypt.crypt(stored, password);
            return MessageDigest.isEqual(stored.getBytes(), cryptpw.getBytes());
        } catch (Exception x) {
            return false;
        }
    }

    /**
     * Whether a stored hash should be re-hashed (because it is in a legacy
     * format or uses a weaker-than-current iteration count). Useful for
     * transparently upgrading credentials on successful login.
     *
     * @param stored the stored hash
     * @return true if the hash should be replaced with a fresh {@link #hash}
     */
    public static boolean needsRehash(String stored) {
        if (stored == null || !stored.startsWith(PREFIX)) {
            return true;
        }
        try {
            int iterations = Integer.parseInt(stored.split("\\$")[2]);
            return iterations < DEFAULT_ITERATIONS;
        } catch (Exception malformed) {
            return true;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations, int bits) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (Exception x) {
            throw new RuntimeException("Could not derive PBKDF2 hash", x);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean isMd5Hex(String s) {
        if (s.length() != 32) {
            return false;
        }
        for (int i = 0; i < 32; i++) {
            char c = s.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }
}
