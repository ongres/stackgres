package io.stackgres.slony.cloud;

import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validation shared by the cloud metadata probes so that a captive portal — or anything else on the
 * network that answers HTTP with a {@code 200} — cannot be mistaken for a cloud metadata service. A
 * bare {@code 200} is never trusted: the genuine services identify themselves with a provider-specific
 * response header, serve plain text (never HTML), and return short single-token values.
 *
 * <p>This stops captive portals and random responders; it does not defend against an active on-path
 * attacker who forges the headers (only signed instance-identity tokens would).
 */
final class MetadataResponses {

    private MetadataResponses() {
    }

    // A cloud region / availability-zone / instance identifier: one short token, no whitespace,
    // no angle brackets, no slashes — which also rejects an HTML body outright.
    private static final Pattern VALID_VALUE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,252}");

    /**
     * True when the response carries {@code <name>: <expected>} (case-insensitive).
     */
    static boolean headerMatches(HttpResponse<?> response, String name, String expected) {
        return response.headers().firstValue(name)
                .map(value -> value.equalsIgnoreCase(expected))
                .orElse(false);
    }

    /**
     * True when the {@code Server} response header equals {@code expected} (case-insensitive).
     */
    static boolean serverIs(HttpResponse<?> response, String expected) {
        return headerMatches(response, "Server", expected);
    }

    /** True unless the response declares an HTML content type. */
    static boolean isNotHtml(HttpResponse<?> response) {
        return !response.headers().firstValue("Content-Type")
                .map(value -> value.toLowerCase(Locale.ROOT).contains("text/html"))
                .orElse(false);
    }

    /**
     * The trimmed value when it is a valid single token, otherwise {@code null}.
     */
    static String validValueOrNull(String body) {
        if (body == null) {
            return null;
        }
        String value = body.strip();
        return VALID_VALUE.matcher(value).matches() ? value : null;
    }

}
