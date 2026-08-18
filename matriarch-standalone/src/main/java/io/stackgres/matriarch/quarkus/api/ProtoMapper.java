package io.stackgres.matriarch.quarkus.api;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterOperationProgress;
import io.stackgres.matriarch.model.spec.*;
import io.stackgres.matriarch.model.status.ClusterStatus;
import io.stackgres.matriarch.model.status.InstanceStatus;
import io.stackgres.matriarch.model.status.ReplicationStatus;
import io.stackgres.matriarch.model.status.RunStatus;
import io.stackgres.proto.api.v1.postgres.PostgresClusterCreateSpec;
import io.stackgres.proto.api.v1.postgres.PostgresClusterSpec;
import io.stackgres.proto.types.v1.Event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The single boundary where {@code stackgres.api.v1} protobuf meets the domain model
 * (decision (b)). The core never sees a generated class; the adapter maps here.
 * The slon/slony agent boundary lives separately in {@code slony.SlonyMapper}.
 */
public final class ProtoMapper {

    private ProtoMapper() {
    }

    // ---- inbound: request -> domain ----

    static ClusterCreate toCreate(io.stackgres.proto.api.v1.CreateClusterRequest req) {
        TlsMode tls = req.hasTls() && req.getTls().hasNone() ? TlsMode.NONE : TlsMode.SELF_SIGNED;
        Integer requestedPort = req.hasPort() ? req.getPort() : null;
        String password = req.getPassword().isBlank() ? null : req.getPassword();
        return new ClusterCreate(req.getName(), toDomain(req.getEngine()), req.getVersion(),
                req.getReplicas(), requestedPort, req.getListenAddress(), req.getUsername(), password, tls,
                toEngineSpec(req), req.getTagsMap());
    }

    private static PostgresSpec toEngineSpec(io.stackgres.proto.api.v1.CreateClusterRequest req) {
        if (!req.hasPostgres()) {
            return null;
        }
        PostgresClusterCreateSpec pg = req.getPostgres();
        List<Extension> exts = pg.getExtensionList().stream()
                .map(e -> new Extension(e.getName(),
                        e.hasVersion() ? e.getVersion() : null,
                        e.hasRevision() ? e.getRevision() : null))
                .toList();
        Map<String, String> settings = pg.hasConfig() ? pg.getConfig().getSettingMap() : java.util.Map.<String, String>of();
        return new PostgresSpec(exts, settings);
    }

    static DatabaseEngine toDomain(io.stackgres.proto.types.v1.DatabaseEngine engine) {
        return switch (engine) {
            case DATABASE_ENGINE_POSTGRES -> DatabaseEngine.POSTGRES;
            case DATABASE_ENGINE_IVORY -> DatabaseEngine.IVORY;
            default -> DatabaseEngine.UNSPECIFIED;
        };
    }

    // ---- outbound: domain -> proto ----

    public static io.stackgres.proto.api.v1.Cluster toProto(Cluster c) {
        ClusterSpec spec = c.spec();
        ClusterStatus status = c.status();
        Map<String, InstanceStatus> obsById = new HashMap<>();
        for (InstanceStatus is : status.instances()) {
            obsById.put(is.id().value(), is);
        }
        io.stackgres.proto.api.v1.Cluster.Builder b = io.stackgres.proto.api.v1.Cluster.newBuilder()
                .setId(id(spec.id().value()))
                .setEnvironmentId("local")
                .setName(spec.name())
                .setEngine(toProtoEngine(spec.engine()))
                .setVersion(spec.version())
                .setStatus(toProto(status.runStatus()))
                .setCredential(io.stackgres.proto.api.v1.ClusterCredentials.newBuilder()
                        .setUsername(spec.credential().username()).build());
        spec.tags().forEach(b::putTags);
        int idx = 0;
        for (InstanceSpec inst : spec.instances()) {
            InstanceStatus obs = obsById.get(inst.id().value());
            b.addInstance(io.stackgres.proto.api.v1.Instance.newBuilder()
                    .setId(id(inst.id().value()))
                    .setName(spec.name() + "-" + idx++)
                    .setVersion(spec.version())
                    .setStatus(toProto(obs != null ? obs.runStatus() : status.runStatus()))
                    .setReplicationStatus(toProtoRepl(obs != null ? obs.replication() : ReplicationStatus.UNKNOWN))
                    .setListenAddress(inst.listenAddress())
                    .setExternalAddress(obs != null ? obs.address() : "")
                    .setPort(obs != null && obs.port() > 0 ? obs.port()
                            : (inst.requestedPort() != null ? inst.requestedPort() : 0))
                    .setCpu(obs != null ? obs.cpu() : 0)
                    .setMemory(obs != null ? obs.memory() : 0)
                    .setStorageUsed(obs != null ? obs.storageUsed() : 0));
        }
        // Postgres-family capability (engine x executor), §5.1.
        b.addCapability(io.stackgres.proto.types.v1.Capability.CAPABILITY_EXTENSIONS);
        // Engine spec: resolved extensions.
        if (spec.engineSpec() instanceof PostgresSpec pg && !pg.extensions().isEmpty()) {
            PostgresClusterSpec.Builder pgb = io.stackgres.proto.api.v1.postgres.PostgresClusterSpec.newBuilder();
            for (Extension e : pg.extensions()) {
                io.stackgres.proto.api.v1.postgres.Extension.Builder eb = io.stackgres.proto.api.v1.postgres.Extension.newBuilder().setName(e.name());
                if (e.version() != null) eb.setVersion(e.version());
                if (e.revision() != null) eb.setRevision(e.revision());
                pgb.addExtension(eb);
            }
            b.setPostgres(pgb);
        }
        return b.build();
    }

    static io.stackgres.proto.api.v1.ClusterOperationProgress toProto(ClusterOperationProgress p) {
        io.stackgres.proto.api.v1.ClusterOperationProgress.Builder b = io.stackgres.proto.api.v1.ClusterOperationProgress.newBuilder()
                .setStatus(toProtoOpStatus(p.status()))
                .setCluster(toProto(p.cluster()));
        if (p.error() != null) {   // present only on FAILED
            b.setError(com.google.rpc.Status.newBuilder().setCode(13).setMessage(p.error()).build());  // 13 = INTERNAL
        }
        return b.build();
    }

    static io.stackgres.proto.types.v1.SourceInfo liveSourceInfo() {
        return io.stackgres.proto.types.v1.SourceInfo.newBuilder()
                .setSource(io.stackgres.proto.types.v1.SourceInfo.Source.LIVE)
                .setEnvironmentHealth(io.stackgres.proto.types.v1.SourceInfo.EnvironmentHealth.CONNECTED)
                .build();
    }

    private static io.stackgres.proto.types.v1.Id id(String value) {
        return io.stackgres.proto.types.v1.Id.newBuilder().setValue(value).build();
    }

    /**
     * Maps a domain {@link ClusterEvent} to the {@code types.v1.Event} the events API returns.
     */
    public static io.stackgres.proto.types.v1.Event toProtoEvent(ClusterEvent e) {
        Event.Builder b = io.stackgres.proto.types.v1.Event.newBuilder()
                .setScope("cluster")
                .setScopeId(id(e.clusterId().value()))
                .setTimestamp(toTimestamp(e.timestamp()));
        switch (e) {
            case ClusterEvent.ClusterAccepted a -> {
                b.setType("cluster.accepted");
                b.putData("name", a.name());
                b.putData("version", a.version());
                b.putData("standalone", String.valueOf(a.standalone()));
            }
            case ClusterEvent.ClusterHealthy h -> b.setType("cluster.healthy");
            case ClusterEvent.ClusterFailed f -> {
                b.setType("cluster.failed");
                if (f.reason() != null) b.putData("reason", f.reason());
            }
            case ClusterEvent.ClusterStarting s -> b.setType("cluster.starting");
            case ClusterEvent.ClusterStopping s -> b.setType("cluster.stopping");
            case ClusterEvent.ClusterRestarting r -> b.setType("cluster.restarting");
            case ClusterEvent.ClusterDeleting d -> b.setType("cluster.deleting");
            case ClusterEvent.ClusterDeleted d -> b.setType("cluster.deleted");
            case ClusterEvent.ClusterRecovered r -> {
                b.setType("cluster.recovered");
                b.putData("name", r.name());
            }
            case ClusterEvent.ClusterObserved o -> {
                b.setType("cluster.observed");
                b.putData("name", o.name());
            }
        }
        return b.build();
    }

    private static com.google.protobuf.Timestamp toTimestamp(java.time.Instant t) {
        return com.google.protobuf.Timestamp.newBuilder()
                .setSeconds(t.getEpochSecond())
                .setNanos(t.getNano())
                .build();
    }

    private static io.stackgres.proto.types.v1.DatabaseEngine toProtoEngine(DatabaseEngine e) {
        return switch (e) {
            case POSTGRES -> io.stackgres.proto.types.v1.DatabaseEngine.DATABASE_ENGINE_POSTGRES;
            case IVORY -> io.stackgres.proto.types.v1.DatabaseEngine.DATABASE_ENGINE_IVORY;
            default -> io.stackgres.proto.types.v1.DatabaseEngine.DATABASE_ENGINE_UNSPECIFIED;
        };
    }

    private static io.stackgres.proto.types.v1.ReplicationStatus toProtoRepl(ReplicationStatus r) {
        return switch (r) {
            case STANDALONE -> io.stackgres.proto.types.v1.ReplicationStatus.REPLICATION_STATUS_STANDALONE;
            case PRIMARY -> io.stackgres.proto.types.v1.ReplicationStatus.REPLICATION_STATUS_PRIMARY;
            case REPLICA -> io.stackgres.proto.types.v1.ReplicationStatus.REPLICATION_STATUS_REPLICA;
            default -> io.stackgres.proto.types.v1.ReplicationStatus.REPLICATION_STATUS_UNSPECIFIED;
        };
    }

    private static io.stackgres.proto.types.v1.ClusterStatus toProto(RunStatus phase) {
        return switch (phase) {
            case PENDING -> io.stackgres.proto.types.v1.ClusterStatus.CLUSTER_STATUS_PENDING;
            case INITIALIZING -> io.stackgres.proto.types.v1.ClusterStatus.CLUSTER_STATUS_INITIALIZING;
            case STARTING -> io.stackgres.proto.types.v1.ClusterStatus.CLUSTER_STATUS_STARTED;
            case HEALTHY -> io.stackgres.proto.types.v1.ClusterStatus.CLUSTER_STATUS_HEALTHY;
            case FAILED -> io.stackgres.proto.types.v1.ClusterStatus.CLUSTER_STATUS_FAILED;
            case STOPPED -> io.stackgres.proto.types.v1.ClusterStatus.CLUSTER_STATUS_STOPPED;
            case UNKNOWN -> io.stackgres.proto.types.v1.ClusterStatus.CLUSTER_STATUS_UNKNOWN;
        };
    }

    private static io.stackgres.proto.api.v1.OperationStatus toProtoOpStatus(ClusterOperationProgress.OperationStatus status) {
        return switch (status) {
            case ACCEPTED -> io.stackgres.proto.api.v1.OperationStatus.OPERATION_STATUS_ACCEPTED;
            case RUNNING -> io.stackgres.proto.api.v1.OperationStatus.OPERATION_STATUS_RUNNING;
            case SUCCEEDED -> io.stackgres.proto.api.v1.OperationStatus.OPERATION_STATUS_SUCCEEDED;
            case FAILED -> io.stackgres.proto.api.v1.OperationStatus.OPERATION_STATUS_FAILED;
        };
    }

}