package io.stackgres.matriarch;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.event.Event;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterAlreadyRunningException;
import io.stackgres.matriarch.model.ClusterAlreadyStoppedException;
import io.stackgres.matriarch.model.ClusterNameInUseException;
import io.stackgres.matriarch.model.ClusterNotFoundException;
import io.stackgres.matriarch.model.Credentials;
import io.stackgres.matriarch.model.spec.ClusterCreate;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.ClusterOperationProgress;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.status.ClusterStatus;
import io.stackgres.matriarch.model.status.InstanceStatus;
import io.stackgres.matriarch.model.spec.CredentialSpec;
import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.EngineSpec;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.model.InstanceId;
import io.stackgres.matriarch.model.spec.InstanceRole;
import io.stackgres.matriarch.model.spec.InstanceSpec;
import io.stackgres.matriarch.model.spec.PostgresSpec;
import io.stackgres.matriarch.model.status.RunStatus;
import io.stackgres.matriarch.spi.ExtensionCatalog;
import io.stackgres.matriarch.spi.Executor;
import io.stackgres.matriarch.spi.StateStore;
import io.stackgres.matriarch.spi.StatusCache;
import io.stackgres.matriarch.spi.VersionCatalog;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The local matriarch core: a <strong>desired-state reconciliation engine</strong>
 * (§3.6, P1). Plain Java, zero framework dependencies (P2). One per environment.
 *
 * <p>Three responsibilities, one per section below:
 * <ul>
 *   <li><b>Commands</b> — the client-facing API (the gRPC resource calls these):
 *       accepted-then-watch mutations that stream {@link ClusterOperationProgress}
 *       to a {@link ProgressSink}, plus reads.
 *   <li><b>Observation intake</b> — the executor's feedback (the adapter forwards
 *       it here): the substrate reports what it sees; these drive the watches and
 *       the {@link StatusCache}. <em>Not</em> the outbound {@link ClusterEvent}
 *       domain events — the direction is inbound.
 *   <li><b>Adoption</b> — pre-existing clusters an agent reports at startup.
 * </ul>
 *
 * <p>Desired specs live in the durable {@link StateStore} (the source of truth);
 * observed status in the lossy {@link StatusCache}. Outbound, the matriarch raises
 * typed domain {@link Event}s to a caller-supplied {@code Consumer}.
 */
public final class Matriarch {

    private static final System.Logger LOG = System.getLogger(Matriarch.class.getName());

    private final StateStore store;
    private final StatusCache statusCache;
    private final Executor executor;
    private final Consumer<Event> events;
    private final VersionCatalog versions;
    private final ExtensionCatalog extensions;

    /** Active accepted-then-watch operations, keyed by cluster. */
    private final Map<ClusterId, Watch> watches = new ConcurrentHashMap<>();

    public Matriarch(StateStore store, StatusCache statusCache, Executor executor, Consumer<Event> events, VersionCatalog versions, ExtensionCatalog extensions) {
        this.store = Objects.requireNonNull(store, "store");
        this.statusCache = Objects.requireNonNull(statusCache, "statusCache");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.events = Objects.requireNonNull(events, "events");
        this.versions = Objects.requireNonNull(versions, "versions");
        this.extensions = Objects.requireNonNull(extensions, "extensions");
    }

    // ======================================================================
    // Commands — client-facing; persist desired, ask the executor to converge.
    // ======================================================================

    public void createCluster(ClusterCreate intent, String idempotencyKey, ProgressSink progress) {
        Objects.requireNonNull(progress, "progress");

        ClusterId existing = store.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            Cluster prior = getCluster(existing);
            if (prior != null) {
                // Genuine retry under the same key — replay the prior outcome, don't double-create.
                progress.onProgress(ClusterOperationProgress.succeeded(prior));
                progress.onComplete();
                return;
            }
            // Stale idempotency record — the prior cluster was since deleted. Fall through and
            // create a fresh one (delete clears the key, but stay robust if one ever lingers).
        }

        // Cluster names are unique per environment (a name frees up once its cluster is deleted).
        // This is separate from idempotency: it rejects a DIFFERENT create that reuses a live name,
        // whereas the key above dedupes a retry of the SAME create. A blank name is resolved to a
        // fresh unique default in planDesiredSpec, so only explicit names need the check.
        if (intent.name() != null && !intent.name().isBlank() && nameInUse(intent.name())) {
            throw new ClusterNameInUseException(intent.name());
        }

        // No prior outcome for this key — only now plan a fresh spec (pure; fresh ids) and
        // persist it SYNCHRONOUSLY as the source of truth, before provisioning (§3.6).
        ClusterSpec spec = planDesiredSpec(intent);
        store.createDesired(spec, idempotencyKey);
        // Mint (or accept) the superuser password and hold it by reference (§3.7): the plaintext lives
        // in the store, never in the desired spec. The bridge executor reads it back to provision, and
        // GetClusterCredentials resolves it for the client.
        String password = intent.password() != null && !intent.password().isBlank() ? intent.password() : Defaults.generatePassword();
        store.putCredential(spec.id(), password);
        statusCache.put(ClusterStatus.pending(spec.id()));
        watches.put(spec.id(), new Watch(progress, WatchKind.CREATE));
        progress.onProgress(ClusterOperationProgress.accepted(snapshot(spec.id())));
        events.accept(new ClusterEvent.ClusterAccepted(Instant.now(), spec.id(), spec.name(), spec.version(), spec.standalone()));

        executor.apply(spec);   // non-blocking; progress arrives via notifyStatus()
    }

    public void deleteCluster(ClusterId id, String idempotencyKey, ProgressSink progress) {
        Objects.requireNonNull(progress, "progress");
        ClusterSpec desired = store.getDesired(id);
        if (desired == null) {
            progress.onComplete();   // already absent — idempotent no-op
            return;
        }

        // Run the teardown EXACTLY ONCE: a retry under the same key, or a delete already
        // in flight for this cluster, replays the in-progress state instead of re-running
        // (the teardown may later involve stop + cleanup side-effects, §5.1).
        boolean claimed = idempotencyKey == null || idempotencyKey.isBlank() || store.recordIdempotency(idempotencyKey, id);
        if (!claimed || watches.containsKey(id)) {
            progress.onProgress(ClusterOperationProgress.accepted(snapshot(id)));
            progress.onComplete();
            return;
        }

        watches.put(id, new Watch(progress, WatchKind.DELETE));
        progress.onProgress(ClusterOperationProgress.accepted(snapshot(id)));
        events.accept(new ClusterEvent.ClusterDeleting(Instant.now(), id));

        // A cluster still PENDING was never provisioned on any agent — there is nothing to tear
        // down, so complete the delete here without needing an executor/agent connected.
        ClusterStatus observed = statusCache.get(id);
        if (observed != null && observed.runStatus() == RunStatus.PENDING) {
            notifyRemoved(id);
            return;
        }

        executor.remove(desired);   // non-blocking; completion via notifyRemoved()
    }

    public void startCluster(ClusterId id, String idempotencyKey, ProgressSink progress) {
        Objects.requireNonNull(progress, "progress");
        ClusterSpec desired = store.getDesired(id);
        if (desired != null && isRunning(runStatusOf(id))) {
            // Reject a redundant start (matches the old matriarch's isRunning() check, and mirrors the
            // stop guard). Safe now that adopted clusters report their true status, not a fake HEALTHY.
            throw new ClusterAlreadyRunningException(desired.name());
        }
        lifecycle(id, idempotencyKey, progress, WatchKind.START);
    }

    public void stopCluster(ClusterId id, String idempotencyKey, ProgressSink progress) {
        Objects.requireNonNull(progress, "progress");
        ClusterSpec desired = store.getDesired(id);
        if (desired != null && runStatusOf(id) == RunStatus.STOPPED) {
            // Reject, matching the old matriarch's "already stopped" error, rather than silently
            // no-op (a redundant StopDb would fail at pg_ctl anyway). Mapped to FAILED_PRECONDITION.
            throw new ClusterAlreadyStoppedException(desired.name());
        }
        lifecycle(id, idempotencyKey, progress, WatchKind.STOP);
    }

    public void restartCluster(ClusterId id, String idempotencyKey, ProgressSink progress) {
        // Match the old matriarch (which checked cluster.isRunning()): only a running server gets the
        // full stop-then-start; a stopped/crashed/not-yet-running one just starts — sending StopDb to a
        // dead server fails at pg_ctl ("PID file does not exist").
        WatchKind kind = runStatusOf(id) == RunStatus.HEALTHY ? WatchKind.RESTART : WatchKind.START;
        lifecycle(id, idempotencyKey, progress, kind);
    }

    /** Best-known observed run status of a cluster (UNKNOWN if never observed). */
    private RunStatus runStatusOf(ClusterId id) {
        ClusterStatus s = statusCache.get(id);
        return s != null ? s.runStatus() : RunStatus.UNKNOWN;
    }

    /** Up, or on its way up — a start would be redundant (the old matriarch's {@code isRunning()}). */
    private static boolean isRunning(RunStatus s) {
        return s == RunStatus.HEALTHY || s == RunStatus.STARTING || s == RunStatus.INITIALIZING;
    }

    /** Shared accepted-then-watch skeleton for the imperative lifecycle verbs (start/stop/restart). */
    private void lifecycle(ClusterId id, String idempotencyKey, ProgressSink progress, WatchKind kind) {
        Objects.requireNonNull(progress, "progress");
        ClusterSpec desired = store.getDesired(id);
        if (desired == null) {
            throw new ClusterNotFoundException(id);   // no desired state to act on
        }
        // Run the verb once per key: a retry, or an operation already in flight, replays the accepted
        // state instead of re-driving the substrate.
        boolean claimed = idempotencyKey == null || idempotencyKey.isBlank()
                || store.recordIdempotency(idempotencyKey, id);
        if (!claimed || watches.containsKey(id)) {
            progress.onProgress(ClusterOperationProgress.accepted(snapshot(id)));
            progress.onComplete();
            return;
        }
        watches.put(id, new Watch(progress, kind));
        progress.onProgress(ClusterOperationProgress.accepted(snapshot(id)));
        Instant now = Instant.now();
        switch (kind) {
            case START -> { events.accept(new ClusterEvent.ClusterStarting(now, id)); executor.start(desired); }
            case STOP -> { events.accept(new ClusterEvent.ClusterStopping(now, id)); executor.stop(desired); }
            case RESTART -> { events.accept(new ClusterEvent.ClusterRestarting(now, id)); executor.restart(desired); }
            default -> throw new IllegalArgumentException("not a lifecycle verb: " + kind);
        }
    }

    public Cluster getCluster(ClusterId id) {
        ClusterSpec spec = store.getDesired(id);
        return spec == null ? null : new Cluster(spec, observedOrUnknown(id));
    }

    /** Like {@link #getCluster} but throws {@link ClusterNotFoundException} when the cluster is absent. */
    public Cluster requireCluster(ClusterId id) {
        Cluster cluster = getCluster(id);
        if (cluster == null) {
            throw new ClusterNotFoundException(id);
        }
        return cluster;
    }

    public List<Cluster> listClusters() {
        return store.listDesired().stream()
                .map(spec -> new Cluster(spec, observedOrUnknown(spec.id())))
                .toList();
    }

    /** The cluster's resolved superuser credential (§3.7, separately authorized), or {@code null}. */
    public Credentials getCredentials(ClusterId id) {
        ClusterSpec spec = store.getDesired(id);
        return spec == null ? null : new Credentials(spec.credential().username(), store.getCredential(id));
    }

    /** Seed a credential the matriarch didn't mint — e.g. one an agent reports at adoption. No-op on blank. */
    public void recordCredential(ClusterId id, String password) {
        if (password != null && !password.isBlank()) {
            store.putCredential(id, password);
        }
    }

    // ======================================================================
    // Observation intake — inbound executor feedback (NOT the outbound events).
    // ======================================================================

    public void notifyStatus(ClusterStatus status) {
        ClusterId id = status.id();
        ClusterStatus cached = statusCache.get(id);
        RunStatus previous = cached != null ? cached.runStatus() : RunStatus.UNKNOWN;
        statusCache.put(status);

        if (status.runStatus() != previous) {
            switch (status.runStatus()) {
                case HEALTHY -> events.accept(new ClusterEvent.ClusterHealthy(Instant.now(), id));
                case FAILED -> events.accept(new ClusterEvent.ClusterFailed(Instant.now(), id, "convergence failed"));
                default -> { /* intermediate phases are progress, not a milestone event */ }
            }
        }

        Watch watch = watches.get(id);
        if (watch != null && watch.kind() != WatchKind.DELETE) {
            // The target run state depends on the verb: a stop converges to STOPPED, everything else
            // (create/start/restart) to HEALTHY. Anything else is in-flight progress.
            RunStatus target = watch.kind() == WatchKind.STOP ? RunStatus.STOPPED : RunStatus.HEALTHY;
            if (status.runStatus() == target) {
                finish(id, ClusterOperationProgress.succeeded(snapshot(id)));
            } else if (status.runStatus() == RunStatus.FAILED) {
                finish(id, ClusterOperationProgress.failed(snapshot(id), "convergence failed"));
            } else {
                watch.progress().onProgress(ClusterOperationProgress.running(snapshot(id)));
            }
        }
    }

    /**
     * Merge observed resource metrics (host cpu/memory from registration, db size from a slon
     * Diagnostics push) into the cached instance status. Out-of-band from lifecycle status, so it
     * keeps runStatus/replication/address/port and touches no watches. No-op if the cluster/instance
     * isn't cached yet.
     */
    public void notifyMetrics(ClusterId clusterId, InstanceId instanceId, double cpu, long memory, long storageUsed) {
        ClusterStatus cached = statusCache.get(clusterId);
        if (cached == null) {
            return;
        }
        List<InstanceStatus> updated = cached.instances().stream()
                .map(is -> is.id().equals(instanceId)
                        ? new InstanceStatus(is.id(), is.runStatus(), is.replication(), is.address(),
                                is.port(), cpu, memory, storageUsed)
                        : is)
                .toList();
        statusCache.put(new ClusterStatus(clusterId, cached.runStatus(), updated));
    }

    public void notifyRemoved(ClusterId id) {
        ClusterSpec desired = store.getDesired(id);
        Cluster stopped = desired == null ? null : new Cluster(desired, new ClusterStatus(id, RunStatus.STOPPED, List.of()));
        store.deleteDesired(id);
        statusCache.delete(id);
        events.accept(new ClusterEvent.ClusterDeleted(Instant.now(), id));

        Watch watch = watches.get(id);
        if (watch != null && watch.kind() == WatchKind.DELETE) {
            if (stopped != null) {
                finish(id, ClusterOperationProgress.succeeded(stopped));
            } else {
                finishComplete(id);
            }
        }
    }

    public void notifyFailed(ClusterId id, String reason) {
        LOG.log(System.Logger.Level.WARNING, "operation on cluster {0} failed: {1}", id.value(), reason);
        events.accept(new ClusterEvent.ClusterFailed(Instant.now(), id, reason));
        if (watches.containsKey(id)) {
            finish(id, ClusterOperationProgress.failed(snapshot(id), reason));
        }
    }

    /**
     * The executor could not apply this spec yet (no substrate available — e.g. no slony-linux agent
     * connected). The cluster rests at PENDING: the create stream completes (client returns) and
     * {@link #reconcile()} will provision it once an executor becomes available.
     */
    public void notifyPending(ClusterId id, String reason) {
        LOG.log(System.Logger.Level.INFO, "cluster {0} pending: {1}", id.value(), reason);
        statusCache.put(ClusterStatus.pending(id));
        Watch watch = watches.get(id);
        if (watch != null && watch.kind() != WatchKind.DELETE) {
            finish(id, ClusterOperationProgress.accepted(snapshot(id)));
        }
    }

    // ======================================================================
    // Adoption — existing clusters an agent reports at startup (§3.2).
    // ======================================================================

    /**
     * Adopt clusters an agent already runs (its startup instance list). With a durable
     * StateStore this would be observation-only; the in-memory store loses desired on
     * restart, so the agent IS the only source of truth and unknown clusters are adopted.
     * Already-known clusters are left untouched.
     */
    public void adopt(List<Cluster> clusters) {
        for (Cluster cluster : clusters) {
            if (store.getDesired(cluster.id()) != null) {
                continue;   // already known by this id — same cluster, skip
            }
            resolveNameConflict(cluster);
            store.createDesired(cluster.spec(), "");
            statusCache.put(cluster.status());
            events.accept(new ClusterEvent.ClusterRecovered(Instant.now(), cluster.id(), cluster.spec().name()));
            LOG.log(System.Logger.Level.INFO, "adopted cluster {0} ({1}) from agent registration",
                    cluster.spec().name(), cluster.id().value());
        }
    }

    /**
     * An adopted cluster is real (running on the agent) and wins its name. If a DIFFERENT desired
     * cluster already holds that name — an unrealized/failed intent such as a PENDING create issued
     * while no agent was connected — it loses: fail and drop it here, BEFORE {@link #reconcile()}
     * would otherwise try to provision a duplicate onto an agent that already has the cluster (§5.1).
     * Two <em>live</em> clusters claiming one name is an anomaly we won't resolve by dropping a
     * running cluster — that only gets a loud warning.
     */
    private void resolveNameConflict(Cluster adopted) {
        ClusterId conflict = store.listDesired().stream()
                .filter(s -> s.name().equals(adopted.spec().name()) && !s.id().equals(adopted.id()))
                .map(ClusterSpec::id)
                .findFirst().orElse(null);
        if (conflict == null) {
            return;
        }
        ClusterStatus cs = statusCache.get(conflict);
        RunStatus run = cs != null ? cs.runStatus() : RunStatus.UNKNOWN;
        if (run == RunStatus.HEALTHY || run == RunStatus.INITIALIZING || run == RunStatus.STARTING) {
            LOG.log(System.Logger.Level.WARNING,
                    "name ''{0}'' claimed by both adopted {1} and live desired {2} — leaving both; investigate",
                    adopted.spec().name(), adopted.id().value(), conflict.value());
            return;
        }
        LOG.log(System.Logger.Level.WARNING, "failing pending cluster {0}: name ''{1}'' taken by adopted cluster", conflict.value(), adopted.spec().name());
        events.accept(new ClusterEvent.ClusterFailed(Instant.now(), conflict, "a cluster named '" + adopted.spec().name() + "' already exists on the agent"));
        statusCache.delete(conflict);
        store.deleteDesired(conflict);
    }

    /**
     * Declarative sync from an <em>authoritative</em> observer — the whole truth for this environment
     * in one call (e.g. the {@code k8s-stackgres} executor that watches the StackGres CRs). New
     * clusters are adopted, already-known ones have their spec + observed status refreshed, and any
     * previously-known cluster no longer present is forgotten. Contrast {@link #adopt}, which is
     * additive-only (an agent reports what it runs, never what it stopped running); a periodic
     * {@code reconcileObserved} keeps the core in lockstep with a source that also removes.
     *
     * <p>Read-only-safe: it only writes to the {@link StateStore}/{@link StatusCache}, never the
     * executor. When a live agent (v1.1 {@code slon}) later feeds metrics via {@code notifyMetrics},
     * the observer's status refresh must merge-preserve those fields rather than zero them.
     */
    public void reconcileObserved(List<Cluster> observed) {
        Set<ClusterId> seen = new HashSet<>();
        for (Cluster cluster : observed) {
            ClusterId id = cluster.id();
            seen.add(id);
            if (store.getDesired(id) == null) {
                resolveNameConflict(cluster);
                store.createDesired(cluster.spec(), "");
                statusCache.put(cluster.status());
                events.accept(new ClusterEvent.ClusterObserved(Instant.now(), id, cluster.spec().name()));
            } else {
                ClusterStatus previous = statusCache.get(id);   // read before overwriting
                store.updateDesired(cluster.spec());   // spec may have changed (version, instances, tags)
                statusCache.put(cluster.status());
                emitStatusTransition(id, previous, cluster.status());
            }
        }
        // listDesired() returns a copy, so removing while iterating is safe.
        for (ClusterSpec spec : store.listDesired()) {
            if (!seen.contains(spec.id())) {
                statusCache.delete(spec.id());
                store.deleteDesired(spec.id());
                events.accept(new ClusterEvent.ClusterDeleted(Instant.now(), spec.id()));
            }
        }
    }

    /**
     * Emit the lifecycle event matching a <em>change</em> in observed run status, so an authoritative
     * observer's refresh produces a real event timeline (nothing on an unchanged status).
     */
    private void emitStatusTransition(ClusterId id, ClusterStatus previous, ClusterStatus current) {
        RunStatus before = previous != null ? previous.runStatus() : null;
        RunStatus after = current.runStatus();
        if (after == before) {
            return;
        }
        Instant now = Instant.now();
        switch (after) {
            case HEALTHY -> events.accept(new ClusterEvent.ClusterHealthy(now, id));
            case FAILED -> events.accept(new ClusterEvent.ClusterFailed(now, id, null));
            case STARTING, INITIALIZING -> events.accept(new ClusterEvent.ClusterStarting(now, id));
            case STOPPED -> events.accept(new ClusterEvent.ClusterStopping(now, id));
            default -> { /* PENDING / UNKNOWN — no transition event */ }
        }
    }

    /**
     * Re-apply desired specs that have not converged — invoked when an executor becomes available
     * (e.g. a slony-linux agent registers). Only PENDING/UNKNOWN specs are applied; HEALTHY or
     * in-flight clusters are left alone (agent-reported clusters are marked HEALTHY by
     * {@link #adopt} first, so this never re-provisions a running one).
     */
    public void reconcile() {
        for (ClusterSpec spec : store.listDesired()) {
            ClusterStatus status = statusCache.get(spec.id());
            RunStatus run = status != null ? status.runStatus() : RunStatus.UNKNOWN;
            // Only PENDING specs need provisioning (a create issued while no agent was connected).
            // NOT UNKNOWN: an adopted cluster starts UNKNOWN (it already exists on the agent and just
            // hasn't reported its live status yet) — re-applying it would re-initialize its database.
            if (run == RunStatus.PENDING) {
                LOG.log(System.Logger.Level.INFO, "reconciling pending cluster {0}", spec.id().value());
                executor.apply(spec);
            }
        }
    }

    // ======================================================================
    // Internals
    // ======================================================================

    private void finish(ClusterId id, ClusterOperationProgress terminal) {
        Watch watch = watches.remove(id);
        if (watch != null) {
            watch.progress().onProgress(terminal);
            watch.progress().onComplete();
        }
    }

    private void finishComplete(ClusterId id) {
        Watch watch = watches.remove(id);
        if (watch != null) {
            watch.progress().onComplete();
        }
    }

    /** A fresh Cluster snapshot (desired spec + observed status) for the watch. */
    private Cluster snapshot(ClusterId id) {
        Cluster cluster = getCluster(id);
        if (cluster == null) {
            throw new IllegalStateException("no desired state for cluster " + id.value());
        }
        return cluster;
    }

    private ClusterStatus observedOrUnknown(ClusterId id) {
        ClusterStatus observed = statusCache.get(id);
        return observed != null ? observed : ClusterStatus.unknown(id);
    }

    private boolean nameInUse(String name) {
        return store.listDesired().stream().anyMatch(s -> name.equals(s.name()));
    }

    /** Turn a create intent into a concrete desired spec (§5.1). */
    private ClusterSpec planDesiredSpec(ClusterCreate intent) {
        if (intent.replicas() != 0) {
            throw new UnsupportedOperationException("only standalone (replicas=0) is supported so far");
        }
        ClusterId clusterId = new ClusterId(UUID.randomUUID().toString());
        InstanceId instanceId = new InstanceId(UUID.randomUUID().toString());
        // The matriarch fills sensible defaults for whatever the client left blank (§5.1) —
        // an empty name or version must never reach the executor (e.g. a blank version yields a
        // malformed slony image reference like "postgres--").
        String name = intent.name() == null || intent.name().isBlank()
                ? Defaults.nextName(store.listDesired().stream().map(ClusterSpec::name).collect(Collectors.toSet()))
                : intent.name();
        DatabaseEngine engine = intent.engine() == null || intent.engine() == DatabaseEngine.UNSPECIFIED
                ? DatabaseEngine.POSTGRES : intent.engine();
        // Resolve the version description (blank / "17" / "17.4") to an exact available version so
        // the desired spec is concrete (§5.1). Backed by the DOCIR catalog in the adapter.
        String version = versions.resolveVersion(engine, intent.version());
        String username = intent.username() == null || intent.username().isBlank()
                ? Defaults.USERNAME : intent.username();
        // Resolve extensions to exact (name, version, revision) for this engine+version (§5.1).
        PostgresSpec engineSpec = resolveEngineSpec(engine, version, intent.engineSpec());
        // An explicit requested port is strict (the executor fails if it is taken); null lets the
        // substrate assign one, and the actual bound port lands in the observed status (§5.1).
        String listenAddress = intent.listenAddress() == null || intent.listenAddress().isBlank()
                ? Defaults.LISTEN_ADDRESS : intent.listenAddress();
        InstanceSpec primary = new InstanceSpec(instanceId, InstanceRole.PRIMARY,
                intent.requestedPort(), listenAddress, engineSpec);
        // generate = the client didn't bring its own password (the value is held by reference either way).
        boolean generate = intent.password() == null || intent.password().isBlank();
        return new ClusterSpec(clusterId, name, engine, version,
                List.of(primary), new CredentialSpec(username, generate), intent.tls(),
                engineSpec,
                intent.tags() != null ? intent.tags() : Map.of());
    }

    private PostgresSpec resolveEngineSpec(DatabaseEngine engine, String version, EngineSpec requested) {
        List<Extension> requestedExtensions = requested instanceof PostgresSpec ps ? ps.extensions() : List.of();
        Map<String, String> settings = requested instanceof PostgresSpec ps ? ps.settings() : Map.of();
        return new PostgresSpec(extensions.resolveExtensions(engine, version, requestedExtensions), settings);
    }

    /** An in-flight accepted-then-watch operation and which verb it is watching. */
    private record Watch(ProgressSink progress, WatchKind kind) {
    }

    private enum WatchKind { CREATE, DELETE, START, STOP, RESTART }

}