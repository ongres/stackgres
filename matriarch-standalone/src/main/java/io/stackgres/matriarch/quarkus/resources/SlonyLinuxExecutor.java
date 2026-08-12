package io.stackgres.matriarch.quarkus.resources;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.InstanceId;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.model.spec.InstanceSpec;
import io.stackgres.matriarch.model.spec.PostgresSpec;
import io.stackgres.matriarch.model.status.ClusterStatus;
import io.stackgres.matriarch.model.status.InstanceStatus;
import io.stackgres.matriarch.model.status.ReplicationStatus;
import io.stackgres.matriarch.model.status.RunStatus;
import io.stackgres.matriarch.quarkus.event.Observation;
import io.stackgres.matriarch.quarkus.event.ObservationBridge;
import io.stackgres.matriarch.spi.Executor;
import io.stackgres.matriarch.spi.StateStore;
import io.stackgres.proto.slon.SlonStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The BRIDGE executor (plan B): drives the <em>existing</em> slon/slony protocol
 * so the real, unmodified agents provision a standalone Postgres, while presenting
 * the new {@link Executor} SPI to the core.
 *
 * <p>Event-driven and non-blocking (§3.6): {@link #apply} kicks the slony
 * conversation and returns; the {@code on*} methods (called by the gRPC bridge
 * servers as agent messages arrive) advance a small per-instance state machine —
 * port handshake → CreateClusterCommand → (slon) InitDb → StartDb → HEALTHY —
 * firing {@link Observation} events at each step. The {@link ObservationBridge}
 * forwards those to the framework-free matriarch core.
 *
 * <p>Transport and wire-message construction live in the {@link Slons} (per-instance)
 * and {@link Slonys} (per-node) registries — mirroring the old matriarch's split — so
 * this class holds only the SPI and the provisioning state machine.
 */
@ApplicationScoped
public class SlonyLinuxExecutor implements Executor {

    private static final System.Logger LOG = System.getLogger(SlonyLinuxExecutor.class.getName());

    @Inject
    Event<Observation> observations;

    @Inject
    StateStore stateStore;

    @Inject
    Slons slons;

    @Inject
    Slonys slonys;

    private final Map<UUID, Provisioning> provisioning = new ConcurrentHashMap<>();  // instanceId -> ctx
    private final Map<UUID, UUID> portActions = new ConcurrentHashMap<>();            // actionId  -> instanceId
    private final Map<UUID, UUID> removing = new ConcurrentHashMap<>();               // instanceId -> clusterId
    private final Map<UUID, Long> dbSizeByInstance = new ConcurrentHashMap<>();       // instanceId -> last pushed dbSize
    private final Map<UUID, UUID> instanceToCluster = new ConcurrentHashMap<>();      // instanceId -> clusterId (survives HEALTHY)
    private final Map<UUID, Integer> portByInstance = new ConcurrentHashMap<>();      // instanceId -> bound port (survives HEALTHY)
    /**
     * In-flight lifecycle (start/stop/restart) operations, keyed by instance.
     */
    private final Map<UUID, Lifecycle> lifecycleOps = new ConcurrentHashMap<>();

    private enum LifecycleKind {START, STOP, RESTART}

    private record Lifecycle(UUID clusterId, LifecycleKind kind) {
    }

    // ---- Executor SPI ----

    @Override
    public void apply(ClusterSpec desired) {
        if (!slonys.isReady()) {
            // No usable substrate — the cluster rests at PENDING and reconcile() re-applies it when a
            // slony-linux agent (re)connects or its heartbeat recovers. NOT a failure.
            observations.fire(new Observation.Pending(desired.id(), slonys.isConnected()
                    ? "slony-linux agent is not responding — will provision when it recovers"
                    : "no slony-linux agent connected yet — will provision when one connects"));
            return;
        }
        InstanceSpec instance = desired.instances().get(0);
        UUID instanceId = UUID.fromString(instance.id().value());
        if (provisioning.containsKey(instanceId)) {
            return;   // already converging — idempotent
        }
        // Use the password the matriarch minted and stored (§3.7); it's the same value returned by
        // GetClusterCredentials. Fall back to a fresh one only if it's somehow missing.
        String password = stateStore.getCredential(desired.id());
        if (password == null) {
            password = UUID.randomUUID().toString().substring(0, 18);
            LOG.log(Level.WARNING, "no stored credential for {0} — provisioning with a fresh password", desired.id().value());
        }
        Provisioning prov = new Provisioning(desired, instance, password);
        provisioning.put(instanceId, prov);
        instanceToCluster.put(instanceId, UUID.fromString(desired.id().value()));   // survives HEALTHY for late diagnostics

        UUID actionId = UUID.randomUUID();
        portActions.put(actionId, instanceId);
        if (!slonys.reportUnusedPort(actionId, instance.requestedPort())) {
            // The agent stream died between the null-check and this send — roll back and treat as no
            // agent so the cluster stays PENDING and reconcile picks it up when one reconnects.
            provisioning.remove(instanceId);
            portActions.remove(actionId);
            observations.fire(new Observation.Pending(desired.id(),
                    "slony-linux agent disconnected — will provision when one reconnects"));
            return;
        }
        report(prov, RunStatus.INITIALIZING);
    }

    @Override
    public void remove(ClusterSpec spec) {
        if (!slonys.isReady()) {
            observations.fire(new Observation.Failed(spec.id(), "no slony-linux agent is connected to delete this cluster"));
            return;
        }
        UUID clusterId = UUID.fromString(spec.id().value());
        for (InstanceSpec instance : spec.instances()) {
            UUID instanceId = UUID.fromString(instance.id().value());
            removing.put(instanceId, clusterId);
            if (!slonys.deleteInstance(instanceId)) {
                removing.remove(instanceId);
                observations.fire(new Observation.Failed(spec.id(),
                        "slony-linux agent disconnected during delete"));
                return;
            }
        }
    }

    @Override
    public void start(ClusterSpec spec) {
        lifecycle(spec, LifecycleKind.START);
    }

    @Override
    public void stop(ClusterSpec spec) {
        lifecycle(spec, LifecycleKind.STOP);
    }

    @Override
    public void restart(ClusterSpec spec) {
        lifecycle(spec, LifecycleKind.RESTART);
    }

    /**
     * Drive a lifecycle verb over the per-instance slon stream (kept live past provisioning). START
     * sends StartDb; STOP and RESTART send StopDb first (RESTART sends StartDb once the slon reports
     * STOPPED). Terminal status (HEALTHY/STOPPED) arrives back via {@link #onSlonStatus}.
     */
    private void lifecycle(ClusterSpec spec, LifecycleKind kind) {
        UUID clusterId = UUID.fromString(spec.id().value());
        UUID instanceId = UUID.fromString(spec.instances().get(0).id().value());
        if (!slons.isConnected(instanceId)) {
            observations.fire(new Observation.Failed(spec.id(),
                    "cluster is not running on a connected agent — cannot " + kind.name().toLowerCase()));
            return;
        }
        lifecycleOps.put(instanceId, new Lifecycle(clusterId, kind));
        boolean sent = kind == LifecycleKind.START ? slons.startDb(instanceId) : slons.stopDb(instanceId);
        if (!sent) {
            lifecycleOps.remove(instanceId);
            observations.fire(new Observation.Failed(spec.id(),
                    "slon connection lost — cannot " + kind.name().toLowerCase()));
            return;
        }
        if (kind == LifecycleKind.START) {
            reportInstance(instanceId, clusterId, RunStatus.STARTING);   // stop/restart have no transitional enum
        }
    }

    @Override
    public Set<String> capabilities() {
        return Set.of("CLUSTER_LIFECYCLE");
    }

    // ---- agent events (called by the gRPC bridge servers) ----

    /**
     * A slon pushed a Diagnostics with the current db size; merge it into the instance's observed metrics.
     */
    public void onDiagnostics(UUID instanceId, long dbSize) {
        dbSizeByInstance.put(instanceId, dbSize);
        UUID clusterId = instanceToCluster.get(instanceId);
        if (clusterId != null) {
            observations.fire(new Observation.Metrics(new ClusterId(clusterId.toString()),
                    new InstanceId(instanceId.toString()), slonys.cpu(), slonys.memory(), dbSize));
        }
    }

    public void onUnusedPort(UUID actionId, int port) {
        UUID instanceId = portActions.remove(actionId);
        if (instanceId == null) {
            return;
        }
        Provisioning prov = provisioning.get(instanceId);
        if (prov == null) {
            return;
        }
        // An explicit port is strict: the agent reports the next free port from the desired one, so a
        // different port coming back means the requested one was already taken — fail rather than
        // silently binding elsewhere.
        Integer requested = prov.instance.requestedPort();
        if (requested != null && port != requested) {
            provisioning.remove(instanceId);
            observations.fire(new Observation.Failed(prov.spec.id(),
                    "requested port " + requested + " is already in use"));
            return;
        }
        prov.port = port;
        portByInstance.put(instanceId, port);   // survives HEALTHY for lifecycle status reports
        List<Extension> extensions = prov.spec.engineSpec() instanceof PostgresSpec pg ? pg.extensions() : List.of();
        boolean sent = slonys.createCluster(
                UUID.fromString(prov.spec.id().value()), instanceId,
                prov.spec.name(), prov.spec.version(), prov.spec.credential().username(), prov.password,
                prov.port, prov.instance.listenAddress(), extensions, prov.spec.tags());
        if (!sent) {
            provisioning.remove(instanceId);
            observations.fire(new Observation.Pending(prov.spec.id(),
                    "slony-linux agent disconnected — will provision when one reconnects"));
            return;
        }
        report(prov, RunStatus.INITIALIZING);
    }

    public void onInstanceCreated(UUID instanceId) {
        Provisioning prov = provisioning.get(instanceId);
        if (prov != null) {
            report(prov, RunStatus.STARTING);
        }
    }

    /**
     * Register an adopted instance (from a slony Registration) so its live slon status pushes are
     * recorded and lifecycle ops can reach it — the same maps {@link #apply} seeds during provisioning.
     */
    public void adoptInstance(UUID instanceId, UUID clusterId, int port) {
        instanceToCluster.put(instanceId, clusterId);
        portByInstance.put(instanceId, port);
    }

    public void onSlonStatus(UUID instanceId, io.stackgres.proto.slon.StatusUpdate update) {
        Provisioning prov = provisioning.get(instanceId);
        if (prov == null) {
            if (lifecycleOps.containsKey(instanceId)) {
                onLifecycleStatus(instanceId, update.getSlonStatus(), update.getStatus().getMessage());
                return;
            }
            // Idle/adopted instance (never re-drive it through initdb): just RECORD the observed status,
            // so an adopted cluster's UNKNOWN self-corrects to its true HEALTHY/STOPPED/FAILED.
            UUID clusterId = instanceToCluster.get(instanceId);
            if (clusterId != null) {
                RunStatus run = mapSlonStatus(update.getSlonStatus());
                if (run != RunStatus.UNKNOWN) {
                    reportInstance(instanceId, clusterId, run);
                }
            }
            return;
        }
        SlonStatus status = update.getSlonStatus();
        LOG.log(Level.INFO, "slon instance {0} reported {1}", instanceId, status);
        switch (status) {
            case STATUS_CREATED -> slons.initDb(instanceId);
            case STATUS_INITDB -> slons.startDb(instanceId);
            case STATUS_STARTED -> report(prov, RunStatus.STARTING);
            case STATUS_HEALTHY -> {
                provisioning.remove(instanceId);
                LOG.log(Level.INFO, "cluster {0} HEALTHY — host=127.0.0.1 port={1} user={2}", prov.spec.name(), prov.port, prov.spec.credential().username());
                report(prov, RunStatus.HEALTHY);
            }
            case STATUS_FAILED -> {
                provisioning.remove(instanceId);
                String msg = update.getStatus().getMessage();
                observations.fire(new Observation.Failed(prov.spec.id(),
                        "slon reported FAILED" + (msg.isEmpty() ? "" : ": " + msg)));
            }
            default -> { /* unspecified */ }
        }
    }

    /**
     * Advance an in-flight lifecycle op from a slon status (the instance has no Provisioning).
     */
    private void onLifecycleStatus(UUID instanceId, io.stackgres.proto.slon.SlonStatus status, String failMsg) {
        Lifecycle op = lifecycleOps.get(instanceId);
        if (op == null) {
            return;   // stale/recovered slon with no active lifecycle op — ignore
        }
        LOG.log(Level.INFO, "lifecycle {0} for instance {1}: slon reported {2}", op.kind(), instanceId, status);
        switch (status) {
            case STATUS_STOPPED -> {
                if (op.kind() == LifecycleKind.RESTART) {
                    if (!slons.startDb(instanceId)) {   // stopped half done — now start it back up
                        lifecycleOps.remove(instanceId);
                        observations.fire(new Observation.Failed(new ClusterId(op.clusterId().toString()),
                                "slon connection lost during restart"));
                    }
                } else {
                    lifecycleOps.remove(instanceId);
                    reportInstance(instanceId, op.clusterId(), RunStatus.STOPPED);
                }
            }
            case STATUS_STARTED -> reportInstance(instanceId, op.clusterId(), RunStatus.STARTING);   // transitional
            case STATUS_HEALTHY -> {
                lifecycleOps.remove(instanceId);
                reportInstance(instanceId, op.clusterId(), RunStatus.HEALTHY);
            }
            case STATUS_FAILED -> {
                lifecycleOps.remove(instanceId);
                observations.fire(new Observation.Failed(new ClusterId(op.clusterId().toString()),
                        "slon reported FAILED during " + op.kind().name().toLowerCase()
                        + (failMsg == null || failMsg.isEmpty() ? "" : ": " + failMsg)));
            }
            default -> { /* CREATED/INITDB not expected outside provisioning */ }
        }
    }

    public void onSlonDisconnected(UUID instanceId) {
        slons.remove(instanceId);
        Lifecycle op = lifecycleOps.remove(instanceId);
        if (op != null) {
            observations.fire(new Observation.Failed(new ClusterId(op.clusterId().toString()),
                    "slon disconnected during " + op.kind().name().toLowerCase()));
        }
        Provisioning prov = provisioning.remove(instanceId);
        if (prov != null) {
            observations.fire(new Observation.Failed(prov.spec.id(), "slon disconnected before reporting HEALTHY"));
        }
    }

    public void onInstanceDeleted(UUID instanceId) {
        instanceToCluster.remove(instanceId);
        dbSizeByInstance.remove(instanceId);
        portByInstance.remove(instanceId);
        slons.remove(instanceId);
        lifecycleOps.remove(instanceId);
        UUID clusterId = removing.remove(instanceId);
        if (clusterId != null) {
            observations.fire(new Observation.Removed(new ClusterId(clusterId.toString())));
        }
    }

    // ---- helpers ----

    private void report(Provisioning prov, RunStatus phase) {
        int port = prov.port > 0 ? prov.port : (prov.instance.requestedPort() != null ? prov.instance.requestedPort() : 0);
        long dbSize = dbSizeByInstance.getOrDefault(UUID.fromString(prov.instance.id().value()), 0L);
        InstanceStatus instanceStatus = new InstanceStatus(prov.instance.id(), phase, ReplicationStatus.STANDALONE, slonys.externalAddress(), port, slonys.cpu(), slonys.memory(), dbSize);
        observations.fire(new Observation.Status(new ClusterStatus(prov.spec.id(), phase, List.of(instanceStatus))));
    }

    /**
     * Emit an observed status for an instance with no active Provisioning (lifecycle path).
     */
    private void reportInstance(UUID instanceId, UUID clusterId, RunStatus phase) {
        InstanceStatus is = new InstanceStatus(new InstanceId(instanceId.toString()), phase, ReplicationStatus.STANDALONE,
                slonys.externalAddress(), portByInstance.getOrDefault(instanceId, 0),
                slonys.cpu(), slonys.memory(), dbSizeByInstance.getOrDefault(instanceId, 0L));
        observations.fire(new Observation.Status(
                new ClusterStatus(new ClusterId(clusterId.toString()), phase, List.of(is))));
    }

    /**
     * Map a reported slon status to the domain run status (for recording an idle/adopted cluster's state).
     */
    private static RunStatus mapSlonStatus(io.stackgres.proto.slon.SlonStatus s) {
        return switch (s) {
            case STATUS_HEALTHY -> RunStatus.HEALTHY;
            case STATUS_STOPPED -> RunStatus.STOPPED;
            case STATUS_FAILED -> RunStatus.FAILED;
            case STATUS_STARTED -> RunStatus.STARTING;
            case STATUS_CREATED, STATUS_INITDB -> RunStatus.INITIALIZING;
            default -> RunStatus.UNKNOWN;
        };
    }

    /**
     * Mutable per-instance provisioning context.
     */
    private static final class Provisioning {
        final ClusterSpec spec;
        final InstanceSpec instance;
        final String password;
        int port;

        Provisioning(ClusterSpec spec, InstanceSpec instance, String password) {
            this.spec = spec;
            this.instance = instance;
            this.password = password;
        }
    }

}