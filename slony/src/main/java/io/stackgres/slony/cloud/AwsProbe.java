package io.stackgres.slony.cloud;

import io.stackgres.cloud.Cloud;
import io.stackgres.cloud.CloudEnvironment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * AWS EC2 metadata detection via IMDS.
 * Tries IMDSv2 (token-based) first, falls back to IMDSv1 (plain GET).
 */
class AwsProbe implements CloudProbe {

    private static final System.Logger logger = System.getLogger("AwsProbe");
    private static final String ENDPOINT = "http://169.254.169.254";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(2);

    @Override
    public CloudEnvironment detect(HttpClient client) {
        try {
            String token = getImdsv2Token(client);
            String region = getMetadata(client, token, "/latest/meta-data/placement/region");
            String az = getMetadata(client, token, "/latest/meta-data/placement/availability-zone");
            String computeInstanceName = getMetadata(client, token, "/latest/meta-data/instance-id");
            if (region != null)
                return new CloudEnvironment(Cloud.AWS, region, az, computeInstanceName);
        } catch (Exception e) {
            logger.log(System.Logger.Level.DEBUG, "AWS detection failed: {0}", e.getMessage());
        }
        return null;
    }

    private static String getImdsv2Token(HttpClient client) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENDPOINT + "/latest/api/token"))
                    .timeout(REQUEST_TIMEOUT)
                    .method("PUT", HttpRequest.BodyPublishers.noBody())
                    .header("X-aws-ec2-metadata-token-ttl-seconds", "21600")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200)
                return response.body().strip();
        } catch (Exception e) {
            // ignored
        }
        // fall back to IMDSv1
        return null;
    }

    private static String getMetadata(HttpClient client, String token, String path) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT + path))
                .timeout(REQUEST_TIMEOUT)
                .GET();
        if (token != null)
            builder.header("X-aws-ec2-metadata-token", token);
        HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200)
            return response.body().strip();
        return null;
    }

}