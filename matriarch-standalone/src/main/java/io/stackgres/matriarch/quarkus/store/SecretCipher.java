package io.stackgres.matriarch.quarkus.store;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption for secret-classed values (cluster credentials) stored at rest in the SQLite
 * file (§3.7), so the DB file alone — a copy, a backup, a snapshot — does not leak superuser passwords.
 * Stored form is {@code "v1:" + base64(nonce || ciphertext+tag)}; a value <em>without</em> the prefix is
 * treated as legacy plaintext on read, so an existing (pre-encryption) store keeps working. GCM is
 * authenticated, so a wrong key or tampering fails loudly rather than returning garbage.
 *
 * <p><b>Key management (honest caveat):</b> the key comes from {@code matriarch.secret.key} (base64,
 * ideally injected at runtime — env/secret manager) or, absent that, an auto-generated keyfile next to
 * the DB ({@code <db>.key}, {@code 0600}). The keyfile makes it zero-config and protects against
 * DB-file-only disclosure (a copied {@code .db} without the {@code .key}); it does NOT protect against a
 * reader of the whole directory. Supply the key out-of-band for protection against disk theft too.
 */
final class SecretCipher {

    private static final String PREFIX = "v1:";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    // Instance field, not a static: a build-time-initialized SecureRandom would be baked into a GraalVM
    // native image with a fixed seed (forbidden — predictable at runtime). Created when the cipher is
    // constructed at runtime.
    private final SecureRandom random = new SecureRandom();

    SecretCipher(byte[] keyBytes) {
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("secret key must be 32 bytes (AES-256), got " + keyBytes.length);
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    /** Resolve the key from config (base64) if present, else from a {@code 0600} keyfile (created if absent). */
    static SecretCipher load(String configKeyBase64, Path keyfile) {
        if (configKeyBase64 != null && !configKeyBase64.isBlank()) {
            return new SecretCipher(Base64.getDecoder().decode(configKeyBase64.trim()));
        }
        return new SecretCipher(readOrCreateKeyfile(keyfile));
    }

    private static byte[] readOrCreateKeyfile(Path keyfile) {
        try {
            if (Files.exists(keyfile)) {
                return Base64.getDecoder().decode(Files.readString(keyfile).trim());
            }
            byte[] generated = new byte[32];
            new SecureRandom().nextBytes(generated);   // local, runtime-created (never baked into a native image)
            Files.writeString(keyfile, Base64.getEncoder().encodeToString(generated));
            FilePermissions.restrictToOwner(keyfile);
            return generated;
        } catch (IOException e) {
            throw new IllegalStateException("cannot read/create secret keyfile " + keyfile, e);
        }
    }

    String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] out = new byte[nonce.length + ciphertext.length];
            System.arraycopy(nonce, 0, out, 0, nonce.length);
            System.arraycopy(ciphertext, 0, out, nonce.length, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to encrypt secret", e);
        }
    }

    String decrypt(String stored) {
        if (stored == null) {
            return null;
        }
        if (!stored.startsWith(PREFIX)) {
            return stored;   // legacy plaintext (pre-encryption store) — return as-is
        }
        try {
            byte[] blob = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] nonce = Arrays.copyOfRange(blob, 0, NONCE_BYTES);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            byte[] plaintext = cipher.doFinal(blob, NONCE_BYTES, blob.length - NONCE_BYTES);
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to decrypt secret (wrong key or corrupted value)", e);
        }
    }
}
