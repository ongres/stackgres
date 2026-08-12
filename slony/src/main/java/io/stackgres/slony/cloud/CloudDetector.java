package io.stackgres.slony.cloud;

import io.stackgres.cloud.Cloud;
import io.stackgres.cloud.CloudEnvironment;
import io.stackgres.slony.Config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Probes cloud metadata services in parallel at startup.
 * On non-cloud machines all probes should time out harmlessly (~1s).
 */
public final class CloudDetector {

    private static final System.Logger logger = System.getLogger("CloudDetector");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(1);
    private static final int OVERALL_TIMEOUT_SECONDS = 3;

    private static final List<CloudProbe> PROBES = List.of(
            new AwsProbe(),
            new GcpProbe(),
            new AzureProbe()
    );

    public static CloudEnvironment detect() {
        String envCloud = Config.getValue("STACKGRES_CLOUD", null);
        if (envCloud != null) {
            try {
                Cloud cloud = Cloud.valueOf(envCloud.toUpperCase());
                String region = Config.getValue("STACKGRES_CLOUD_REGION", null);
                String availabilityZone = Config.getValue("STACKGRES_CLOUD_AZ", null);
                String computeInstanceName = Config.getValue("STACKGRES_CLOUD_INSTANCE_NAME", null);
                return new CloudEnvironment(cloud, region, availabilityZone, computeInstanceName);
            } catch (IllegalArgumentException e) {
                logger.log(System.Logger.Level.WARNING, "Unknown STACKGRES_CLOUD value: {0}", envCloud);
            }
        }

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build()) {
            List<CompletableFuture<CloudEnvironment>> futures = PROBES.stream()
                    .map(probe -> CompletableFuture.supplyAsync(() -> probe.detect(client)))
                    .toList();
            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(OVERALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // some or all probes timed out or failed
            }

            for (CompletableFuture<CloudEnvironment> future : futures) {
                CloudEnvironment result = getResult(future);
                if (result != null)
                    return result;
            }
            return null;
        }
    }

    private static CloudEnvironment getResult(CompletableFuture<CloudEnvironment> future) {
        try {
            return future.getNow(null);
        } catch (Exception e) {
            return null;
        }
    }

}