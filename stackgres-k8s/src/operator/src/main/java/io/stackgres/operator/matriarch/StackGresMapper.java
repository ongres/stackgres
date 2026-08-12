package io.stackgres.operator.matriarch;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.InstanceId;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.spec.CredentialSpec;
import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.EngineSpec;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.model.spec.InstanceRole;
import io.stackgres.matriarch.model.spec.InstanceSpec;
import io.stackgres.matriarch.model.spec.PostgresSpec;
import io.stackgres.matriarch.model.spec.TlsMode;
import io.stackgres.matriarch.model.status.ClusterStatus;
import io.stackgres.matriarch.model.status.InstanceStatus;
import io.stackgres.matriarch.model.status.ReplicationStatus;
import io.stackgres.matriarch.model.status.RunStatus;
import io.fabric8.kubernetes.api.model.Quantity;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterInstalledExtension;
import io.stackgres.common.crd.sgcluster.StackGresClusterPodStatus;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterStatus;
import io.stackgres.common.crd.sgprofile.StackGresInstanceProfile;

/**
 * Maps a StackGres {@code SGCluster} (+ its {@code SGInstanceProfile}) to the matriarch domain
 * {@link Cluster} the read-only api.v1 surface serves (v1 — CR + profile only; live pod
 * address/DB size arrive with {@code slon} in v1.1). Pure and null-safe.
 */
final class StackGresMapper {

  private StackGresMapper() {
  }

  static Cluster toCluster(StackGresCluster cr, StackGresInstanceProfile profile) {
    String uid = cr.getMetadata().getUid();
    String name = cr.getMetadata().getName();
    String namespace = cr.getMetadata().getNamespace();
    ClusterId clusterId = new ClusterId(uid);

    StackGresClusterSpec spec = cr.getSpec();
    StackGresClusterStatus status = cr.getStatus();

    String flavor = spec != null && spec.getPostgres() != null ? spec.getPostgres().getFlavor() : null;
    DatabaseEngine engine = "babelfish".equalsIgnoreCase(flavor) ? DatabaseEngine.IVORY : DatabaseEngine.POSTGRES;

    String version = status != null && status.getPostgresVersion() != null
        ? status.getPostgresVersion()
        : spec != null && spec.getPostgres() != null ? spec.getPostgres().getVersion() : null;
    if (version == null) {
      version = "";
    }

    Map<String, String> tags = new HashMap<>();
    if (cr.getMetadata().getLabels() != null) {
      tags.putAll(cr.getMetadata().getLabels());
    }
    if (namespace != null) {
      tags.put("namespace", namespace);
    }

    double cpu = profile != null && profile.getSpec() != null ? cpuCores(profile.getSpec().getCpu()) : 0;
    long memory = profile != null && profile.getSpec() != null ? memoryBytes(profile.getSpec().getMemory()) : 0;

    EngineSpec engineSpec = null;
    if (status != null && status.getExtensions() != null && !status.getExtensions().isEmpty()) {
      List<Extension> exts = new ArrayList<>();
      for (StackGresClusterInstalledExtension e : status.getExtensions()) {
        exts.add(new Extension(e.getName(), e.getVersion(), null));
      }
      engineSpec = new PostgresSpec(exts, Map.of());
    }

    RunStatus runStatus = clusterRunStatus(spec, status);

    List<InstanceSpec> instanceSpecs = new ArrayList<>();
    List<InstanceStatus> instanceStatuses = new ArrayList<>();
    List<StackGresClusterPodStatus> pods = status != null ? status.getPodStatuses() : null;
    if (pods != null && !pods.isEmpty()) {
      for (StackGresClusterPodStatus pod : pods) {
        addInstance(instanceSpecs, instanceStatuses, pod.getName(),
            Boolean.TRUE.equals(pod.getPrimary()), runStatus, cpu, memory);
      }
    } else {
      int desired = spec != null && spec.getInstances() != null ? spec.getInstances() : 0;
      for (int i = 0; i < desired; i++) {
        addInstance(instanceSpecs, instanceStatuses, name + "-" + i, i == 0, runStatus, cpu, memory);
      }
    }

    ClusterSpec clusterSpec = new ClusterSpec(clusterId, name, engine, version, instanceSpecs,
        new CredentialSpec("postgres", false), TlsMode.SELF_SIGNED, engineSpec, tags);
    return new Cluster(clusterSpec, new ClusterStatus(clusterId, runStatus, instanceStatuses));
  }

  /** Deterministic UUID for an instance — the CLI parses instance ids as UUIDs; no Pod fetch needed. */
  private static void addInstance(List<InstanceSpec> specs, List<InstanceStatus> statuses,
      String seedName, boolean primary, RunStatus runStatus, double cpu, long memory) {
    InstanceId id = new InstanceId(
        UUID.nameUUIDFromBytes(seedName.getBytes(StandardCharsets.UTF_8)).toString());
    InstanceRole role = primary ? InstanceRole.PRIMARY : InstanceRole.REPLICA;
    ReplicationStatus repl = primary ? ReplicationStatus.PRIMARY : ReplicationStatus.REPLICA;
    specs.add(new InstanceSpec(id, role, 5432, "0.0.0.0", null));
    statuses.add(new InstanceStatus(id, runStatus, repl, "", 5432, cpu, memory, 0));
  }

  private static RunStatus clusterRunStatus(StackGresClusterSpec spec, StackGresClusterStatus status) {
    if (status == null) {
      return RunStatus.UNKNOWN;
    }
    Integer ready = status.getInstances();
    int desired = spec != null && spec.getInstances() != null ? spec.getInstances() : 0;
    if (ready != null && desired > 0 && ready >= desired) {
      return RunStatus.HEALTHY;
    }
    if (ready != null && ready > 0) {
      return RunStatus.STARTING;
    }
    return RunStatus.PENDING;
  }

  private static double cpuCores(String q) {
    BigDecimal a = amount(q);
    return a != null ? a.doubleValue() : 0;
  }

  private static long memoryBytes(String q) {
    BigDecimal a = amount(q);
    return a != null ? a.longValue() : 0;
  }

  /** k8s quantity string ("500m", "512Mi", "2Gi") to its numeric amount; null/garbage -> null. */
  private static BigDecimal amount(String q) {
    if (q == null || q.isBlank()) {
      return null;
    }
    try {
      return Quantity.getAmountInBytes(new Quantity(q));
    } catch (RuntimeException e) {
      return null;
    }
  }
}
