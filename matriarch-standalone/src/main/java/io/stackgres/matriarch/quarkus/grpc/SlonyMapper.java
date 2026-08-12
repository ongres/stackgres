package io.stackgres.matriarch.quarkus.grpc;

import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.InstanceId;
import io.stackgres.matriarch.model.spec.*;
import io.stackgres.matriarch.model.status.ClusterStatus;
import io.stackgres.matriarch.model.status.InstanceStatus;
import io.stackgres.matriarch.model.status.ReplicationStatus;
import io.stackgres.matriarch.model.status.RunStatus;
import io.stackgres.proto.slony.ClusterInstance;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The slon/slony agent boundary: maps an agent's startup {@code Registration} (its
 * existing containerd clusters) into domain {@link Cluster}s for adoption. Separate
 * from {@code api.ProtoMapper} (the client {@code api.v1} boundary).
 */
final class SlonyMapper {

    private SlonyMapper() {
    }

    /**
     * Rebuild domain clusters from a slony {@code Registration}'s instance list
     * (existing containerd clusters listed at agent startup). Instances are grouped
     * by clusterId; observed status starts UNKNOWN (the registration carries no run
     * state) and self-corrects from the slon's live status pushes.
     */
    static List<Cluster> recover(String externalAddress, double cpu, long memory,
                                 List<io.stackgres.proto.slony.ClusterInstance> reported) {
        Map<String, List<io.stackgres.proto.slony.ClusterInstance>> byCluster = new LinkedHashMap<>();
        for (ClusterInstance ci : reported) {
            byCluster.computeIfAbsent(ci.getClusterId().getValue().toStringUtf8(), k -> new ArrayList<>()).add(ci);
        }
        List<Cluster> clusters = new ArrayList<>();
        for (Map.Entry<String, List<ClusterInstance>> entry : byCluster.entrySet()) {
            ClusterId clusterId = new ClusterId(entry.getKey());
            ClusterInstance first = entry.getValue().get(0);
            List<InstanceSpec> specs = new ArrayList<>();
            List<InstanceStatus> statuses = new ArrayList<>();
            for (ClusterInstance ci : entry.getValue()) {
                InstanceId iid = new InstanceId(ci.getId().getValue().toStringUtf8());
                specs.add(new InstanceSpec(iid, InstanceRole.PRIMARY, ci.getPort(),
                        ci.getListenAddress(), new PostgresSpec(extensions(ci), Map.of())));
                statuses.add(new InstanceStatus(iid, RunStatus.UNKNOWN,
                        ci.getStandalone() ? ReplicationStatus.STANDALONE : ReplicationStatus.PRIMARY,
                        externalAddress, ci.getPort(),   // reachable address is the agent's, not the bind addr
                        cpu, memory, 0));                 // host cpu/memory; db size fills on the next diagnostics push
            }
            ClusterSpec spec = new ClusterSpec(clusterId, first.getClusterName(), toDomainEngine(first.getFlavor()),
                    first.getVersion(), specs, new CredentialSpec(first.getUsername(), false),
                    TlsMode.SELF_SIGNED, new PostgresSpec(extensions(first), Map.of()), first.getTagsMap());
            clusters.add(new Cluster(spec, new ClusterStatus(clusterId, RunStatus.UNKNOWN, statuses)));
        }
        return clusters;
    }

    private static List<Extension> extensions(io.stackgres.proto.slony.ClusterInstance ci) {
        return ci.getExtensionList().stream()
                .map(e -> new Extension(e.getName(),
                        e.getVersion().isBlank() ? null : e.getVersion(),
                        e.getRevision().isBlank() ? null : e.getRevision()))
                .toList();
    }

    private static DatabaseEngine toDomainEngine(common.Common.Flavor flavor) {
        return flavor == common.Common.Flavor.FLAVOR_IVORY_SQL ? DatabaseEngine.IVORY : DatabaseEngine.POSTGRES;
    }

}