package io.stackgres.cli.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exchanges a one-time install token (OTT) for a full JWT via the cloud's unauthenticated
 * {@code POST https://{endpoint}/install/tokens/{ott}/exchange} endpoint — the same bootstrap the
 * install script uses. Always HTTPS (the endpoint's default port unless it carries one). Returns the JWT
 * string; throws a {@link RuntimeException} with a user-facing message on any failure (unreachable, 404
 * invalid/expired, or unparseable response).
 */
public final class TokenExchange {

    private static final Pattern JWT = Pattern.compile("\"jwt\"\\s*:\\s*\"([^\"]+)\"");

    private TokenExchange() {
    }

    public static String exchange(String endpoint, String oneTimeToken) {
        String ott = oneTimeToken == null ? "" : oneTimeToken.trim();
        URI uri = URI.create("https://" + endpoint + "/install/tokens/" + ott + "/exchange");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.noBody())
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<String> response;
        try {
            response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            throw new RuntimeException("could not reach " + endpoint + " to exchange the token: " + reason);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while exchanging the token");
        }
        if (response.statusCode() == 404) {
            throw new RuntimeException("one-time token is invalid or expired — get a fresh one from the web console");
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("token exchange failed: HTTP " + response.statusCode());
        }
        Matcher m = JWT.matcher(response.body());
        if (!m.find()) {
            throw new RuntimeException("could not parse the exchange response from " + endpoint);
        }
        return m.group(1);
    }
}
