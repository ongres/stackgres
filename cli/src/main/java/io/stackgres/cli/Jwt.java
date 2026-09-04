package io.stackgres.cli;

import org.yaml.snakeyaml.Yaml;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Minimal, offline read of a JWT's payload — enough to show "who am I" in {@code status} without a
 * server round-trip (the cloud doesn't serve the AccountService, but the bearer token is always at
 * hand). No signature verification: this is display-only. The payload is base64url JSON, and JSON is a
 * subset of YAML, so the snakeyaml already on the classpath parses it — no extra dependency.
 */
public final class Jwt {

    private Jwt() {
    }

    /** Best-effort display identity: email &gt; preferred_username &gt; sub. Null if not a decodable JWT. */
    public static String subject(String token) {
        Map<String, Object> claims = claims(token);
        if (claims == null) {
            return null;
        }
        for (String key : new String[]{"email", "preferred_username", "sub"}) {
            Object value = claims.get(key);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    /**
     * True only when the token carries an {@code exp} claim (NumericDate, seconds) already in the past.
     * Opaque/garbled tokens and tokens without {@code exp} are NOT reported expired — this is a soft,
     * display-only hint, so we never flag what we can't read.
     */
    public static boolean isExpired(String token) {
        Map<String, Object> claims = claims(token);
        if (claims == null) {
            return false;
        }
        Object exp = claims.get("exp");
        if (exp == null) {
            return false;
        }
        try {
            return Long.parseLong(exp.toString().trim()) <= java.time.Instant.now().getEpochSecond();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Decode a JWT's payload claims (base64url JSON, parsed as YAML). Null if not a decodable JWT. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> claims(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            Object data = new Yaml().load(new String(payload, StandardCharsets.UTF_8));
            return data instanceof Map ? (Map<String, Object>) data : null;
        } catch (RuntimeException e) {
            return null;   // opaque/garbled token — display-only, so fail soft
        }
    }
}
