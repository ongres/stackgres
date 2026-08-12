package io.stackgres.operator.matriarch;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.model.spec.InstanceSpec;
import io.stackgres.matriarch.model.spec.PostgresSpec;
import io.stackgres.matriarch.model.status.ClusterStatus;
import io.stackgres.matriarch.model.status.InstanceStatus;
import io.stackgres.matriarch.model.status.ReplicationStatus;
import io.stackgres.matriarch.model.status.RunStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Domain {@code ->} {@code stackgres.api.v1} mapping — shared by the local read service
 * ({@link StackGresApiReadService}) and the cloud uplink ({@link CloudUplinkClient}), so the mapping
 * lives in one place. Kept in sync with the cloud app's read-subset mapper as the api.v1 proto evolves
 * (see doc/architecture-redesign-stackgres.md §7).
 */
final class ClusterProtoMapper {

  private ClusterProtoMapper() {
  }

  static io.stackgres.proto.api.v1.Cluster toProto(Cluster c, String environmentId) {
    ClusterSpec spec = c.spec();
    ClusterStatus status = c.status();
    Map<String, InstanceStatus> obsById = new HashMap<>();
    for (InstanceStatus is : status.instances()) {
      obsById.put(is.id().value(), is);
    }
    io.stackgres.proto.api.v1.Cluster.Builder b = io.stackgres.proto.api.v1.Cluster.newBuilder()
        .setId(id(spec.id().value()))
        .setEnvironmentId(environmentId)
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
    b.addCapability(io.stackgres.proto.types.v1.Capability.CAPABILITY_EXTENSIONS);
    if (spec.engineSpec() instanceof PostgresSpec pg && !pg.extensions().isEmpty()) {
      io.stackgres.proto.api.v1.postgres.PostgresClusterSpec.Builder pgb =
          io.stackgres.proto.api.v1.postgres.PostgresClusterSpec.newBuilder();
      for (Extension e : pg.extensions()) {
        io.stackgres.proto.api.v1.postgres.Extension.Builder eb =
            io.stackgres.proto.api.v1.postgres.Extension.newBuilder().setName(e.name());
        if (e.version() != null) {
          eb.setVersion(e.version());
        }
        if (e.revision() != null) {
          eb.setRevision(e.revision());
        }
        pgb.addExtension(eb);
      }
      b.setPostgres(pgb);
    }
    return b.build();
  }

  static io.stackgres.proto.types.v1.SourceInfo liveSourceInfo() {
    return io.stackgres.proto.types.v1.SourceInfo.newBuilder()
        .setSource(io.stackgres.proto.types.v1.SourceInfo.Source.LIVE)
        .setEnvironmentHealth(io.stackgres.proto.types.v1.SourceInfo.EnvironmentHealth.CONNECTED)
        .build();
  }

  static io.stackgres.proto.types.v1.Event toProtoEvent(ClusterEvent e) {
    io.stackgres.proto.types.v1.Event.Builder b = io.stackgres.proto.types.v1.Event.newBuilder()
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
        if (f.reason() != null) {
          b.putData("reason", f.reason());
        }
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

  private static io.stackgres.proto.types.v1.Id id(String value) {
    return io.stackgres.proto.types.v1.Id.newBuilder().setValue(value).build();
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
}
