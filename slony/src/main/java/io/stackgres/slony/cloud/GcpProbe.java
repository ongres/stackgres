package io.stackgres.slony.cloud;

import io.stackgres.cloud.Cloud;
import io.stackgres.cloud.CloudEnvironment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * GCP Compute Engine metadata detection.
 * Uses the {@code Metadata-Flavor: Google} header.
 */
class GcpProbe implements CloudProbe {

    private static final System.Logger logger = System.getLogger("GcpProbe");
    private static final String ENDPOINT = "http://metadata.google.internal";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

    @Override
    public CloudEnvironment detect(HttpClient client) {
        try {
            String zonePath = getMetadata(client, "/computeMetadata/v1/instance/zone");
            if (zonePath == null)
                return null;
            // zonePath = "projects/NNN/zones/us-central1-a"
            String az = zonePath.substring(zonePath.lastIndexOf('/') + 1);
            String region = az.substring(0, az.lastIndexOf('-'));
            String computeInstanceName = getMetadata(client, "/computeMetadata/v1/instance/name");
            return new CloudEnvironment(Cloud.GCP, region, az, computeInstanceName);
        } catch (Exception e) {
            logger.log(System.Logger.Level.DEBUG, "GCP detection failed: {0}", e.getMessage());
        }
        return null;
    }

    private static String getMetadata(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Metadata-Flavor", "Google")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200)
            return response.body().strip();
        return null;
    }

}