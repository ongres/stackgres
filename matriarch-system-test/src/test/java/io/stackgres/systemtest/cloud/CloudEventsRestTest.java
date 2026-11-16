package io.stackgres.systemtest.cloud;

import io.stackgres.systemtest.BaseSystemTest;
import io.stackgres.systemtest.TestConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cloud-only: the aggregated Activity REST endpoint {@code GET /events} (JAX-RS on the cloud, consumed by the
 * web console — not the CLI). Over HTTP with the configured JWT it returns a JSON page of events. The base
 * URL is derived from the endpoint host ({@link TestConfig#HTTP_BASE}); override with
 * {@code -Dstackgres.systemtest.http-base=...} if the REST host differs from the gRPC one.
 */
class CloudEventsRestTest extends BaseSystemTest {

    @Test
    void eventsEndpointReturnsJson() throws Exception {
        requireCloud();
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(TestConfig.HTTP_BASE + "/events?limit=10"))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET();
        if (!TestConfig.TOKEN.isBlank()) {
            req.header("Authorization", "Bearer " + TestConfig.TOKEN);
        }
        HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).as("GET /events -> %d: %s", resp.statusCode(), resp.body()).isEqualTo(200);
        assertThat(resp.body().stripLeading()).as("JSON body").matches("(?s)[\\[{].*");   // array or object
    }
}
