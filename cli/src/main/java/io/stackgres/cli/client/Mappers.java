package io.stackgres.cli.client;

import com.google.protobuf.Timestamp;
import io.stackgres.cli.postgres.*;
import io.stackgres.cloud.Cloud;
import io.stackgres.cloud.CloudEnvironment;
import io.stackgres.postgres.Flavor;
import io.stackgres.postgres.GenericClusterInstance;
import io.stackgres.postgres.PostgresCluster;
import io.stackgres.postgres.Status;
import io.stackgres.proto.api.v1.CreateClusterRequest;
import io.stackgres.proto.cli.Event;
import common.Common;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.google.protobuf.ByteString.copyFromUtf8;

public final class Mappers {

    public static Common.UUID mapUUID(UUID uuid) {
        return Common.UUID.newBuilder().setValue(copyFromUtf8(uuid.toString())).build();
    }

    public static UUID mapUUID(Common.UUID uuid) {
        if (uuid == null) return null;
        return UUID.fromString(uuid.getValue().toStringUtf8());
    }

    /**
     * Maps an api.v1 {@code types.v1.Event} to the cli.proto {@link Event} the events command renders.
     */
    public static Event mapEventV1(io.stackgres.proto.types.v1.Event e) {
        Timestamp ts = e.getTimestamp();
        long millis = ts.getSeconds() * 1000L + ts.getNanos() / 1_000_000L;
        Event.Builder b = Event.newBuilder()
                .setTimestamp(millis)
                .setScope(e.getScope().toUpperCase())   // command compares against "CLUSTER"
                .setType(e.getType())
                .putAllData(e.getDataMap());
        if (e.hasScopeId())
            b.setScopeId(Common.UUID.newBuilder().setValue(copyFromUtf8(e.getScopeId().getValue())));
        return b.build();
    }

    public static List<Slony> mapSlonys(List<io.stackgres.proto.cli.Slony> slonys) {
        return slonys.stream()
                .map(s -> {
                    Instant lastHeartbeat = Instant.ofEpochSecond(s.getLastHeartbeat().getSeconds(), s.getLastHeartbeat().getNanos());
                    return new Slony(mapUUID(s.getId()), s.getHostname(), s.getOs(), s.getArch(), s.getVersion(), s.getCpu(), s.getMemory(),
                            mapCloudEnvironment(s), mapSlonyStatus(s.getStatus()), lastHeartbeat, s.getTagsMap(), "");
                })
                .toList();
    }

    private static SlonyStatus mapSlonyStatus(io.stackgres.proto.cli.SlonyStatus status) {
        return switch (status) {
            case SLONY_STATUS_ACTIVE -> SlonyStatus.ACTIVE;
            case SLONY_STATUS_INACTIVE -> SlonyStatus.INACTIVE;
            case SLONY_STATUS_DISCONNECTED -> SlonyStatus.DISCONNECTED;
            case UNRECOGNIZED -> SlonyStatus.UNKNOWN;
        };
    }

    private static CloudEnvironment mapCloudEnvironment(io.stackgres.proto.cli.Slony s) {
        if (!s.hasCloud())
            return null;
        try {
            Cloud cloud = Cloud.valueOf(s.getCloud().toUpperCase());
            String region = s.hasRegion() ? s.getRegion() : null;
            String availabilityZone = s.hasAvailabilityZone() ? s.getAvailabilityZone() : null;
            String computeInstanceName = s.hasComputeInstanceName() ? s.getComputeInstanceName() : null;
            return new CloudEnvironment(cloud, region, availabilityZone, computeInstanceName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // api.v1 Node (from ListNodes) -> the CLI's Slony domain model. Same shape, new source.
    public static List<Slony> mapNodes(List<io.stackgres.proto.api.v1.Node> nodes) {
        return nodes.stream()
                .map(n -> {
                    Instant lastHeartbeat = n.hasLastHeartbeat()
                            ? Instant.ofEpochSecond(n.getLastHeartbeat().getSeconds(), n.getLastHeartbeat().getNanos())
                            : null;
                    return new Slony(java.util.UUID.fromString(n.getId().getValue()), n.getHostname(), n.getOs(),
                            n.getArch(), n.getVersion(), n.getCpu(), n.getMemory(),
                            mapNodeCloudEnvironment(n), mapNodeStatus(n.getStatus()), lastHeartbeat, n.getTagsMap(), n.getEnvironmentId());
                })
                .toList();
    }

    private static CloudEnvironment mapNodeCloudEnvironment(io.stackgres.proto.api.v1.Node n) {
        if (n.getCloud().isBlank())
            return null;
        try {
            Cloud cloud = Cloud.valueOf(n.getCloud().toUpperCase());
            String region = n.getRegion().isBlank() ? null : n.getRegion();
            String availabilityZone = n.getAvailabilityZone().isBlank() ? null : n.getAvailabilityZone();
            String computeInstanceName = n.getComputeInstanceName().isBlank() ? null : n.getComputeInstanceName();
            return new CloudEnvironment(cloud, region, availabilityZone, computeInstanceName);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static SlonyStatus mapNodeStatus(io.stackgres.proto.api.v1.NodeStatus status) {
        return switch (status) {
            case NODE_STATUS_ACTIVE -> SlonyStatus.ACTIVE;
            case NODE_STATUS_INACTIVE -> SlonyStatus.INACTIVE;
            case NODE_STATUS_DISCONNECTED -> SlonyStatus.DISCONNECTED;
            default -> SlonyStatus.UNKNOWN;
        };
    }

    public static List<Slon> mapSlons(List<io.stackgres.proto.cli.Slon> slons) {
        return slons.stream()
                .map(s -> new Slon(mapUUID(s.getId()), s.getPort(), s.getName(), s.getOs(), s.getArch(), s.getVersion(), s.getCpu(), s.getMemory()))
                .toList();
    }

    public static ClusterDiagnostics mapClusterDiagnostics(io.stackgres.proto.cli.ClusterDiagnostics diagnostics) {
        List<InstanceDiagnostics> instances = diagnostics.getInstanceList().stream()
                .map(i -> {
                    UUID instanceId = mapUUID(i.getId());
                    String pgControlData = i.hasPgControlData() ? i.getPgControlData() : null;
                    Instant receivedAt = i.hasReceivedAt() ? Instant.ofEpochMilli(i.getReceivedAt()) : null;
                    String imageName = i.hasImageName() ? i.getImageName() : null;
                    String imageDigest = i.hasImageDigest() ? i.getImageDigest() : null;
                    return new InstanceDiagnostics(instanceId, i.getName(), pgControlData, receivedAt, imageName, imageDigest);
                })
                .toList();
        return new ClusterDiagnostics(instances);
    }

    public static com.google.protobuf.Timestamp mapTimestamp(Instant instant) {
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public static CreateClusterRequest createClusterRequest(PostgresCluster cluster) {
        io.stackgres.postgres.ClusterInstance instance = cluster.getInstances().isEmpty() ? null
                : cluster.getInstances().iterator().next();
        String version = instance == null ? null : instance.getVersion();
        int replicas = cluster.isStandalone() ? 0 : Math.max(0, cluster.getInstances().size() - 1);
        CreateClusterRequest.Builder b =
                CreateClusterRequest.newBuilder()
                        .setEnvironmentId("local")
                        .setIdempotencyKey(UUID.randomUUID().toString())
                        .setEngine(mapEngine(cluster.getFlavor()))
                        .setReplicas(replicas)
                        .putAllTags(cluster.getTags());
        if (cluster.getName() != null)
            b.setName(cluster.getName());
        if (version != null)
            b.setVersion(version);
        if (cluster.getUsername() != null && !cluster.getUsername().isBlank())
            b.setUsername(cluster.getUsername());
        if (cluster.isNoTls())
            b.setTls(io.stackgres.proto.api.v1.TlsConfig.newBuilder()
                    .setNone(io.stackgres.proto.api.v1.NoTls.newBuilder()).build());
        if (!cluster.getExtensions().isEmpty()) {
            io.stackgres.proto.api.v1.postgres.PostgresClusterCreateSpec.Builder pg =
                    io.stackgres.proto.api.v1.postgres.PostgresClusterCreateSpec.newBuilder();
            for (io.stackgres.postgres.Extension e : cluster.getExtensions()) {
                io.stackgres.proto.api.v1.postgres.CreateExtension.Builder ce =
                        io.stackgres.proto.api.v1.postgres.CreateExtension.newBuilder().setName(e.name());
                if (e.version() != null)
                    ce.setVersion(e.version());
                if (e.revision() != null)
                    ce.setRevision(e.revision());
                pg.addExtension(ce);
            }
            b.setPostgres(pg);
        }
        if (instance != null && instance.getPort() != null)
            b.setPort(instance.getPort());
        if (instance != null && instance.getListenAddress() != null && !instance.getListenAddress().isBlank())
            b.setListenAddress(instance.getListenAddress());
        if (cluster.getPassword() != null && !cluster.getPassword().isBlank())
            b.setPassword(cluster.getPassword());
        // NOTE: placement not yet forwarded.
        return b.build();
    }

    public static PostgresCluster mapCluster(io.stackgres.proto.api.v1.Cluster c) {
        List<io.stackgres.postgres.ClusterInstance> instances = c.getInstanceList().stream()
                .map(i -> {
                    UUID instanceId = i.getId().getValue().isEmpty() ? null : UUID.fromString(i.getId().getValue());
                    Integer port = i.getPort() != 0 ? i.getPort() : null;
                    Status status = ClusterStatusMapper.mapStatus(i.getStatus());
                    GenericClusterInstance inst = new GenericClusterInstance(
                            instanceId, i.getName(), i.getVersion(), port, i.getListenAddress(), status);
                    inst.setReplicationStatus(ReplicationStatusMapper.mapStatus(i.getReplicationStatus()));
                    inst.setExternalAddress(i.getExternalAddress());
                    return (io.stackgres.postgres.ClusterInstance) inst;
                })
                .toList();
        UUID id = c.getId().getValue().isEmpty() ? null : UUID.fromString(c.getId().getValue());
        String username = c.hasCredential() ? c.getCredential().getUsername() : null;
        boolean standalone = c.getInstanceCount() <= 1;
        List<io.stackgres.postgres.Extension> extensions = c.hasPostgres()
                ? c.getPostgres().getExtensionList().stream()
                  .map(e -> new io.stackgres.postgres.Extension(e.getName(),
                          e.getVersion().isBlank() ? null : e.getVersion(),
                          e.getRevision().isBlank() ? null : e.getRevision()))
                  .toList()
                : List.of();
        PostgresCluster cluster = new PostgresCluster(id, c.getName(), username, null,
                extensions, c.getTagsMap(), standalone, instances);
        cluster.setFlavor(mapFlavor(c.getEngine()));
        // Roll instance metrics up to the cluster (matches the old CliClusterService): cpu/memory summed
        // across instances, db size the max. Host cpu/memory come from the slony registration, db size
        // from the slon diagnostics push.
        cluster.setCpu(c.getInstanceList().stream().mapToDouble(io.stackgres.proto.api.v1.Instance::getCpu).sum());
        cluster.setMemory(c.getInstanceList().stream().mapToLong(io.stackgres.proto.api.v1.Instance::getMemory).sum());
        cluster.setDbSize(c.getInstanceList().stream().mapToLong(io.stackgres.proto.api.v1.Instance::getStorageUsed).max().orElse(0));
        return cluster;
    }

    static io.stackgres.proto.types.v1.DatabaseEngine mapEngine(Flavor flavor) {
        return flavor == Flavor.IVORY_SQL
                ? io.stackgres.proto.types.v1.DatabaseEngine.DATABASE_ENGINE_IVORY
                : io.stackgres.proto.types.v1.DatabaseEngine.DATABASE_ENGINE_POSTGRES;
    }

    private static Flavor mapFlavor(io.stackgres.proto.types.v1.DatabaseEngine engine) {
        return engine == io.stackgres.proto.types.v1.DatabaseEngine.DATABASE_ENGINE_IVORY ? Flavor.IVORY_SQL : Flavor.POSTGRES;
    }

}