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
    @SuppressWarnings("unchecked")
    public static String subject(String token) {
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
            if (!(data instanceof Map)) {
                return null;
            }
            Map<String, Object> claims = (Map<String, Object>) data;
            for (String key : new String[]{"email", "preferred_username", "sub"}) {
                Object value = claims.get(key);
                if (value != null && !value.toString().isBlank()) {
                    return value.toString();
                }
            }
            return null;
        } catch (RuntimeException e) {
            return null;   // opaque/garbled token — display-only, so fail soft
        }
    }
}
