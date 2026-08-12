package io.stackgres.matriarch.quarkus.resources;

import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.proto.slony.*;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Per-node slony connection registry (mirrors the old matriarch's {@code Slonys}): owns the single
 * connected slony-linux agent stream, its Registration and liveness, and builds+sends the slony
 * {@link MatriarchMessage}s. The executor drives provisioning through the intent methods
 * ({@link #createCluster}/{@link #reportUnusedPort}/{@link #deleteInstance}); the
 * {@code ResourceBridgeService} reads the node accessors and pulls logs.
 *
 * <p>Silent-partition detection (parity with the old matriarch's 15s heartbeat liveness): the agent
 * is usable only while it keeps sending Heartbeat messages. A gap marks it inactive WITHOUT the stream
 * having to close (catching a silent partition), and a resumed heartbeat reactivates it.
 *
 * <p>This is the only class that constructs the slony {@code MatriarchMessage}s — the executor speaks
 * intent, not wire messages.
 */
@ApplicationScoped
public class Slonys {

    private static final System.Logger LOG = System.getLogger(Slonys.class.getName());

    /**
     * The base host port the slony-linux substrate scans from when no explicit port is requested
     * (host ports are shared and scarce here — unlike a service-local k8s substrate). This is the
     * substrate's allocation policy, not a desired-state value.
     */
    private static final int DEFAULT_PORT = 5432;

    private static final long HEARTBEAT_TIMEOUT_MS = 15_000;

    private volatile StreamObserver<MatriarchMessage> slonyOut;

    /**
     * Serializes writes to {@link #slonyOut} — a gRPC StreamObserver is not safe for concurrent onNext.
     */
    private final Object slonyLock = new Object();

    /**
     * The connected agent's reachable address (from its Registration) — the instances' external address.
     */
    private volatile String externalAddress = "";
    /**
     * Host cpu/memory from the slony Registration — stamped on every instance status (§ old behavior).
     */
    private volatile double cpu;
    private volatile long memory;
    /**
     * The connected agent's full Registration (source for the node/slon commands); null when none.
     */
    private volatile Registration registration;
    /**
     * Mutable node tags — seeded from the Registration, mutated by AddNodeTags/RemoveNodeTags.
     */
    private final Map<String, String> nodeTags = new ConcurrentHashMap<>();

    private volatile long lastHeartbeatMillis;
    private volatile boolean agentActive;
    private ScheduledExecutorService liveness;

    @PostConstruct
    void startLiveness() {
        liveness = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "slony-liveness");
            t.setDaemon(true);
            return t;
        });
        liveness.scheduleAtFixedRate(this::checkLiveness, 15, 5, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stopLiveness() {
        if (liveness != null) {
            liveness.shutdownNow();
        }
    }

    /**
     * Marks the agent inactive if it has gone silent past the timeout — the stream may still be open.
     */
    private void checkLiveness() {
        if (slonyOut != null && agentActive && System.currentTimeMillis() - lastHeartbeatMillis > HEARTBEAT_TIMEOUT_MS) {
            agentActive = false;
            LOG.log(Level.WARNING, "slony agent sent no heartbeat for over {0}s — marking inactive", HEARTBEAT_TIMEOUT_MS / 1000);
        }
    }

    // ---- stream lifecycle (called by the gRPC bridge servers) ----

    public void attach(StreamObserver<MatriarchMessage> out, Registration reg) {
        this.slonyOut = out;
        this.registration = reg;
        this.externalAddress = reg.getExternalAddress();
        this.cpu = reg.getCpu();
        this.memory = reg.getMemory();
        this.nodeTags.clear();
        this.nodeTags.putAll(reg.getTagsMap());
        this.lastHeartbeatMillis = System.currentTimeMillis();
        this.agentActive = true;
        LOG.log(Level.INFO, "slony agent registered (external address {0})", reg.getExternalAddress());
    }

    /**
     * The agent stream closed (disconnect / process killed). Drop the reference so subsequent apply/remove
     * see "no agent" and rest at PENDING rather than writing to a dead stream. Guarded so a stale close for
     * an already-replaced connection is a no-op.
     */
    public void detach(StreamObserver<MatriarchMessage> out) {
        if (slonyOut == out) {
            slonyOut = null;
            externalAddress = "";
            agentActive = false;
            registration = null;
            nodeTags.clear();
            LOG.log(Level.INFO, "slony agent disconnected");
        }
    }

    /**
     * A Heartbeat arrived. Refreshes liveness and, if the agent had gone inactive (silent past the
     * timeout), reactivates it. Returns true when this heartbeat RECOVERED the agent, so the caller can
     * reconcile any work left PENDING while it was unresponsive.
     */
    public boolean onHeartbeat() {
        if (slonyOut == null) {
            return false;   // no active stream — ignore
        }
        boolean recovered = !agentActive;
        lastHeartbeatMillis = System.currentTimeMillis();
        agentActive = true;
        return recovered;
    }

    // ---- node accessors (ResourceBridgeService) ----

    boolean isConnected() {
        return slonyOut != null;
    }

    boolean isReady() {
        return slonyOut != null && agentActive;
    }

    public boolean agentActive() {
        return agentActive;
    }

    public long lastHeartbeatMillis() {
        return lastHeartbeatMillis;
    }

    String externalAddress() {
        return externalAddress;
    }

    double cpu() {
        return cpu;
    }

    long memory() {
        return memory;
    }

    /**
     * The connected agent's Registration, or null when none is connected.
     */
    public Registration registration() {
        return registration;
    }

    /**
     * The node's mutable tags (seeded from the Registration).
     */
    public Map<String, String> nodeTags() {
        return nodeTags;
    }

    /**
     * Forget the connected agent (node delete/detach): after this, ListSlonys returns empty.
     */
    public void forgetRegistration() {
        registration = null;
        nodeTags.clear();
    }

    // ---- slony commands (intent methods) ----

    /**
     * The single point that writes to the agent stream. Serializes concurrent writes (the create
     * thread and the agent-callback thread both send) and treats a dead stream as a disconnect —
     * dropping the reference so the cluster falls back to PENDING. Returns false when there is no
     * usable agent stream.
     */
    private boolean send(MatriarchMessage message) {
        StreamObserver<MatriarchMessage> out = slonyOut;
        if (out == null || !agentActive) {
            return false;
        }
        synchronized (slonyLock) {
            try {
                out.onNext(message);
                return true;
            } catch (RuntimeException e) {
                detach(out);
                return false;
            }
        }
    }

    /**
     * Ask the agent for the next free host port at/above {@code desiredPort} (null → {@link #DEFAULT_PORT}).
     */
    boolean reportUnusedPort(UUID actionId, Integer desiredPort) {
        return send(MatriarchMessage.newBuilder()
                .setReportUnusedPortCommand(ReportUnusedPortCommand.newBuilder()
                        .setId(UuidCodec.toProto(actionId))
                        .setDesiredPort(desiredPort != null ? desiredPort : DEFAULT_PORT))
                .build());
    }

    boolean deleteInstance(UUID instanceId) {
        return send(MatriarchMessage.newBuilder()
                .setDeleteInstanceCommand(DeleteInstanceCommand.newBuilder()
                        .setId(UuidCodec.toProto(instanceId)))
                .build());
    }

    /**
     * Asks the connected slony agent to stream its logs back keyed by {@code logId} (bridge, plan B —
     * mirrors the old matriarch's on-demand {@code GetSlonyLogsCommand}). Returns {@code false} if no
     * agent is connected or the stream is dead.
     */
    public boolean requestLogs(UUID logId, boolean follow) {
        return send(MatriarchMessage.newBuilder()
                .setGetSlonyLogsCommand(GetSlonyLogsCommand.newBuilder()
                        .setId(UuidCodec.toProto(logId))
                        .setFollow(follow))
                .build());
    }

    /**
     * Tells the agent to stop a follow ({@code TailNodeLogs}) — mirrors the old {@code AbortSlonyLogsCommand}.
     */
    public boolean abortLogs(UUID logId) {
        return send(MatriarchMessage.newBuilder()
                .setAbortSlonyLogsCommand(AbortSlonyLogsCommand.newBuilder()
                        .setId(UuidCodec.toProto(logId)))
                .build());
    }

    /**
     * Provision a standalone cluster on the agent (the port has already been resolved). Builds the
     * slony-linux placement paths by convention from the instance id (§3.3) and sends the resolved
     * extensions so the agent installs them — and reports them back on re-registration, so they survive
     * a matriarch restart (§5.1). Returns false if the agent stream is gone.
     */
    boolean createCluster(UUID clusterId, UUID instanceId, String clusterName, String version,
                          String username, String password, int port, String listenAddress,
                          List<Extension> extensions, Map<String, String> tags) {
        String instance = instanceId.toString();
        String dataDir = "/var/lib/stackgres/clusters/" + instance;
        CreateInstance.Builder create = CreateInstance.newBuilder()
                .setId(UuidCodec.toProto(instanceId))
                .setName(clusterName + "-0")
                .setPort(port)
                .setListenAddress(listenAddress)
                .setConfigPath(dataDir)
                .setDataDir(dataDir)
                .setLogDir("/var/log/stackgres/" + instance)
                .setWalDir(dataDir + "/pg_wal");
        CreateClusterCommand.Builder command = CreateClusterCommand.newBuilder()
                .setId(UuidCodec.toProto(clusterId))
                .setName(clusterName)
                .setVersion(version)
                .setUsername(username)
                .setPassword(password)
                .setStandalone(true)
                .setFlavor(common.Common.Flavor.FLAVOR_POSTGRES)
                .putAllTags(tags)
                .addInstance(create);
        for (Extension e : extensions) {
            io.stackgres.proto.slony.Extension.Builder eb = io.stackgres.proto.slony.Extension.newBuilder().setName(e.name());
            if (e.version() != null) eb.setVersion(e.version());
            if (e.revision() != null) eb.setRevision(e.revision());
            command.addExtension(eb);
        }
        return send(MatriarchMessage.newBuilder().setCreateClusterCommand(command).build());
    }

}