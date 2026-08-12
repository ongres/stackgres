package io.stackgres.slony.cloud;

import io.stackgres.cloud.Cloud;
import io.stackgres.cloud.CloudEnvironment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Azure Instance Metadata Service (IMDS) detection.
 * Uses the {@code Metadata: true} header with a date-versioned API.
 */
class AzureProbe implements CloudProbe {

    private static final System.Logger logger = System.getLogger("AzureProbe");
    private static final String ENDPOINT = "http://169.254.169.254";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

    @Override
    public CloudEnvironment detect(HttpClient client) {
        try {
            String region = getMetadata(client, "/metadata/instance/compute/location");
            String az = getMetadata(client, "/metadata/instance/compute/zone");
            String computeInstanceName = getMetadata(client, "/metadata/instance/compute/name");
            if (region != null)
                return new CloudEnvironment(Cloud.AZURE, region, az, computeInstanceName);
        } catch (Exception e) {
            logger.log(System.Logger.Level.DEBUG, "Azure detection failed: {0}", e.getMessage());
        }
        return null;
    }

    private static String getMetadata(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + path
                                + "?api-version=2021-02-01&format=text"))
                .timeout(REQUEST_TIMEOUT)
                .header("Metadata", "true")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200)
            return response.body().strip();
        return null;
    }

}