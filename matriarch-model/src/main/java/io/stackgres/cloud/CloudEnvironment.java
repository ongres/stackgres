package io.stackgres.cloud;

public record CloudEnvironment(Cloud cloud, String region, String availabilityZone, String computeInstanceName) {
}
