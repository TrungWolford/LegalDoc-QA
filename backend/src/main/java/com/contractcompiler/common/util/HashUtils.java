package com.contractcompiler.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Utility methods for hashing. Used for file checksums, fingerprints, and token hashes.
 * All methods are pure functions with no side effects.
 */
public final class HashUtils {

    private HashUtils() {
        // utility class
    }

    /**
     * Computes SHA-256 hex digest of the given byte array.
     */
    public static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Computes SHA-256 hex digest of the given string (UTF-8).
     */
    public static String sha256(String input) {
        return sha256(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Alias for {@link #sha256(String)} — used for token hashing.
     */
    public static String sha256Hex(String input) {
        return sha256(input);
    }

    /**
     * Computes SHA-256 hex digest of an input stream.
     * Reads the stream in chunks to support large files without loading into memory.
     */
    public static String sha256(InputStream inputStream) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
