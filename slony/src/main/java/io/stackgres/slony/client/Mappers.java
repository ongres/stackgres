package io.stackgres.slony.client;

import io.stackgres.postgres.Flavor;
import io.stackgres.postgres.PostgresCluster;
import io.stackgres.postgres.SlonyLinuxHAInstance;
import io.stackgres.postgres.SlonyLinuxInstance;
import io.stackgres.proto.slony.ClusterInstance;
import io.stackgres.proto.slony.Extension;
import io.stackgres.proto.slony.Registration;
import io.stackgres.proto.slony.VolumeMount;
import common.Common;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.protobuf.ByteString.copyFromUtf8;
import static io.stackgres.slony.SlonySystem.*;

public final class Mappers {

    public static Common.UUID mapUUID(UUID uuid) {
        return Common.UUID.newBuilder().setValue(copyFromUtf8(uuid.toString())).build();
    }

    public static UUID mapUUID(Common.UUID uuid) {
        if (uuid == null) return null;
        return UUID.fromString(uuid.getValue().toStringUtf8());
    }

    public static Stream<ClusterInstance> mapSlonyClusterInstance(PostgresCluster cluster) {
        return cluster.getInstances().stream()
                .map(SlonyLinuxInstance.class::cast)
                .map(instance -> {
                    ClusterInstance.Builder builder = ClusterInstance.newBuilder()
                            .setId(mapUUID(instance.getId()))
                            .setName(instance.getName())
                            // cluster information
                            .setClusterId(mapUUID(cluster.getId()))
                            .setClusterName(cluster.getName())
                            .setUsername(cluster.getUsername())
                            .setPassword(cluster.getPassword())
                            .setFlavor(mapFlavor(cluster.getFlavor()))
                            .addAllExtension(mapExtensions(cluster.getExtensions()))
                            .putAllTags(cluster.getTags())
                            .setStandalone(cluster.isStandalone())
                            // instance information
                            .setPort(instance.getPort())
                            .setVersion(instance.getVersion())
                            .setListenAddress(instance.getListenAddress())
                            .setConfigPath(instance.getConfigPath().toString())
                            .setDataDir(instance.getDataDir().toString())
                            .setLogDir(instance.getLogDir().toString())
                            .setWalDir(instance.getWalDir().toString())
                            .addAllVolumeMount(Mappers.mapVolumeMounts(instance.getVolumeMounts()));
                    if (instance instanceof SlonyLinuxHAInstance haInstance)
                        builder.setEtcdName(haInstance.getEtcdName())
                                .setEtcdClientUrl(haInstance.getEtcdClientUrl())
                                .setEtcdServerUrl(haInstance.getEtcdServerUrl());
                    if (instance.getImageName() != null)
                        builder.setImageName(instance.getImageName());
                    if (instance.getImageDigest() != null)
                        builder.setImageDigest(instance.getImageDigest());
                    if (instance.getIvorySqlPort() != null)
                        builder.setIvorySqlPort(instance.getIvorySqlPort());
                    return builder.build();
                });
    }

    public static common.Common.Flavor mapFlavor(Flavor flavor) {
        if (flavor == null) return common.Common.Flavor.FLAVOR_POSTGRES;
        return switch (flavor) {
            case POSTGRES -> common.Common.Flavor.FLAVOR_POSTGRES;
            case IVORY_SQL -> common.Common.Flavor.FLAVOR_IVORY_SQL;
        };
    }

    public static Flavor mapFlavor(common.Common.Flavor flavor) {
        return switch (flavor) {
            case FLAVOR_IVORY_SQL -> Flavor.IVORY_SQL;
            case FLAVOR_POSTGRES, UNRECOGNIZED -> Flavor.POSTGRES;
        };
    }

    public static List<Extension> mapExtensions(List<io.stackgres.postgres.Extension> extensions) {
        return extensions.stream().map(Mappers::mapExtension).toList();
    }

    public static Extension mapExtension(io.stackgres.postgres.Extension extension) {
        return Extension.newBuilder()
                .setName(extension.name())
                .setVersion(extension.version())
                .setRevision(extension.revision())
                .build();
    }

    public static List<VolumeMount> mapVolumeMounts(List<io.stackgres.postgres.VolumeMount> volumeMounts) {
        return volumeMounts.stream().map(Mappers::mapVolumeMount).toList();
    }

    private static VolumeMount mapVolumeMount(io.stackgres.postgres.VolumeMount volumeMount) {
        return VolumeMount.newBuilder()
                .setHostPath(volumeMount.hostPath())
                .setMountPath(volumeMount.mountPath())
                .build();
    }

    public static Registration buildRegistration(List<ClusterInstance> instances, UUID slonyId) {
        Registration.Builder builder = Registration.newBuilder()
                .setId(mapUUID(slonyId))
                .setHostname(getHostname())
                .setExternalAddress(getExternalAddress())
                .setOs(getOs())
                .setArch(getArch())
                .setVersion(getVersion())
                .setCpu(getNumberOfCpus())
                .setMemory(getMemoryBytes())
                .addAllInstance(instances)
                .putAllTags(getTags());
        if (getCloudEnvironment() != null) {
            builder.setCloud(getCloudEnvironment().cloud().id());
            if (getCloudEnvironment().region() != null)
                builder.setRegion(getCloudEnvironment().region());
            if (getCloudEnvironment().availabilityZone() != null)
                builder.setAvailabilityZone(getCloudEnvironment().availabilityZone());
            if (getCloudEnvironment().computeInstanceName() != null)
                builder.setComputeInstanceName(getCloudEnvironment().computeInstanceName());
        }
        return builder.build();
    }

    public static Map<String, String> extractMap(String mapString) {
        return Arrays.stream(mapString.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.split("="))
                .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
    }

    public static String mapToString(Map<String, String> map) {
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(","));
    }

}