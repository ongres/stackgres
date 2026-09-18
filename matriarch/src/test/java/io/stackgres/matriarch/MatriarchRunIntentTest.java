package io.stackgres.matriarch;

import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.ClusterOperationProgress;
import io.stackgres.matriarch.model.IllegalRunIntentTransitionException;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.spec.CredentialSpec;
import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.model.spec.RunIntent;
import io.stackgres.matriarch.model.spec.TlsMode;
import io.stackgres.matriarch.model.status.ClusterStatus;
import io.stackgres.matriarch.model.status.RunStatus;
import io.stackgres.matriarch.spi.Executor;
import io.stackgres.matriarch.spi.ExtensionCatalog;
import io.stackgres.matriarch.spi.VersionCatalog;
import io.stackgres.matriarch.store.InMemoryStateStore;
import io.stackgres.matriarch.store.InMemoryStatusCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Durable desired run-intent (§3.6): stop/start persist intent, the reconcile loop drives observed
 * toward it (so an operation completes across a crash), and the code-side legality rules
 * ({@link RunIntent#canTransitionTo}) reject illegal moves.
 */
class MatriarchRunIntentTest {

    private InMemoryStateStore store;
    private InMemoryStatusCache statusCache;
    private RecordingExecutor executor;
    private Matriarch matriarch;

    @BeforeEach
    void setUp() {
        store = new InMemoryStateStore();
        statusCache = new InMemoryStatusCache();
        executor = new RecordingExecutor();
        matriarch = new Matriarch(store, statusCache, executor, e -> { },
                new NoopVersionCatalog(), new NoopExtensionCatalog());
    }

    @Test
    void createdClusterDefaultsToRunning() {
        store.createDesired(spec("a", "alpha"), "k");
        assertEquals(RunIntent.RUNNING, store.getDesiredRun(new ClusterId("a")));
    }

    @Test
    void stopPersistsStoppedIntent() {
        ClusterId id = seed("a", "alpha", RunStatus.HEALTHY);
        matriarch.stopCluster(id, "stop-1", NOOP_SINK);
        assertEquals(RunIntent.STOPPED, store.getDesiredRun(id));
        assertTrue(executor.stopped.contains(id));
    }

    @Test
    void startPersistsRunningIntent() {
        ClusterId id = seed("a", "alpha", RunStatus.STOPPED);
        store.setDesiredRun(id, RunIntent.STOPPED);
        matriarch.startCluster(id, "start-1", NOOP_SINK);
        assertEquals(RunIntent.RUNNING, store.getDesiredRun(id));
        assertTrue(executor.started.contains(id));
    }

    @Test
    void reconcileDrivesStoppedIntentToStop() {
        // Simulates a stop persisted before a crash: after restart the agent re-reports HEALTHY.
        ClusterId id = seed("a", "alpha", RunStatus.HEALTHY);
        store.setDesiredRun(id, RunIntent.STOPPED);
        matriarch.reconcile();
        assertTrue(executor.stopped.contains(id));
        assertTrue(executor.started.isEmpty());
    }

    @Test
    void reconcileDrivesRunningIntentToStart() {
        ClusterId id = seed("a", "alpha", RunStatus.STOPPED);   // intent defaults RUNNING
        matriarch.reconcile();
        assertTrue(executor.started.contains(id));
        assertTrue(executor.stopped.isEmpty());
    }

    @Test
    void notifyStatusConvergesWhenNoWatchOwnsCluster() {
        ClusterId id = seed("a", "alpha", RunStatus.STOPPED);
        store.setDesiredRun(id, RunIntent.STOPPED);
        // Agent later reports HEALTHY with no user op in flight → converge back to STOPPED.
        matriarch.notifyStatus(new ClusterStatus(id, RunStatus.HEALTHY, List.of()));
        assertTrue(executor.stopped.contains(id));
    }

    @Test
    void failedClusterIsNotAutoRestarted() {
        ClusterId id = seed("a", "alpha", RunStatus.FAILED);   // intent RUNNING, but observed FAILED
        matriarch.reconcile();
        assertTrue(executor.started.isEmpty());   // only a settled STOPPED is started
    }

    @Test
    void deletingIntentRejectsStart() {
        ClusterId id = seed("a", "alpha", RunStatus.STOPPED);
        store.setDesiredRun(id, RunIntent.DELETING);
        assertThrows(IllegalRunIntentTransitionException.class,
                () -> matriarch.startCluster(id, "start-1", NOOP_SINK));
        assertTrue(executor.started.isEmpty());
    }

    @Test
    void deletingIntentRejectsStop() {
        ClusterId id = seed("a", "alpha", RunStatus.HEALTHY);
        store.setDesiredRun(id, RunIntent.DELETING);
        assertThrows(IllegalRunIntentTransitionException.class,
                () -> matriarch.stopCluster(id, "stop-1", NOOP_SINK));
    }

    // ---- startup reconciliation: never-provisioned vs adopted (the durable-store gap) ----

    @Test
    void unprovisionedClusterIsProvisionedWhenAgentConnects() {
        // Post-restart shape: desired persisted, StatusCache empty (UNKNOWN), never provisioned.
        ClusterSpec spec = spec("a", "alpha");
        store.createDesired(spec, "");
        assertFalse(store.isProvisioned(spec.id()));

        matriarch.reconcile();   // an agent just connected

        assertTrue(executor.applied.contains(spec.id()));   // provisioned, not stranded
    }

    @Test
    void provisionedButDisconnectedClusterIsNotReapplied() {
        // Post-restart: desired persisted and already provisioned, but its host hasn't re-reported yet.
        ClusterSpec spec = spec("b", "beta");
        store.createDesired(spec, "");
        store.markProvisioned(spec.id());   // it was provisioned before the restart

        matriarch.reconcile();   // status still UNKNOWN (host down)

        assertTrue(executor.applied.isEmpty());   // never re-initialize an existing cluster
    }

    @Test
    void reachingHealthyLatchesProvisioned() {
        ClusterId id = seed("c", "gamma", RunStatus.PENDING);
        assertFalse(store.isProvisioned(id));
        matriarch.notifyStatus(new ClusterStatus(id, RunStatus.HEALTHY, List.of()));
        assertTrue(store.isProvisioned(id));
    }

    @Test
    void adoptKnownClusterIsObservationOnlyAndLatchesProvisioned() {
        // A cluster already in the durable store; an agent re-registers reporting it exists.
        ClusterSpec spec = spec("d", "delta");
        store.createDesired(spec, "");
        Cluster reported = new Cluster(spec("d", "delta-renamed-on-agent"),
                new ClusterStatus(spec.id(), RunStatus.HEALTHY, List.of()));

        matriarch.adopt(List.of(reported));

        assertTrue(store.isProvisioned(spec.id()));                       // latched
        assertEquals("delta", store.getDesired(spec.id()).name());       // desired spec untouched
        assertEquals(RunStatus.HEALTHY, statusCache.get(spec.id()).runStatus());   // observed refreshed
    }

    // ---- failure propagation (provisioning failure surfaced to status + watch) ----

    @Test
    void notifyFailedFlipsObservedStatusToFailed() {
        ClusterId id = seed("h", "eta", RunStatus.STARTING);
        matriarch.notifyFailed(id, "failed to create cluster instance: image not found");
        assertEquals(RunStatus.FAILED, statusCache.get(id).runStatus());
    }

    @Test
    void notifyFailedTerminatesWatchWithReason() {
        ClusterId id = seed("i", "theta", RunStatus.STARTING);
        CapturingSink sink = new CapturingSink();
        matriarch.stopCluster(id, "op-1", sink);   // registers a watch (STOP), drives executor
        matriarch.notifyFailed(id, "boom");
        assertTrue(sink.completed);
        assertEquals("boom", sink.lastError);
    }

    // ---- delete: provisioned goes through the executor; deferred keeps the DELETING intent ----

    @Test
    void deleteOfProvisionedClusterGoesThroughExecutorAndForgetsOnRemoval() {
        // A provisioned cluster delegates to the executor (not the never-provisioned local shortcut).
        // When removal completes (via the agent, or a local forget), notifyRemoved forgets it.
        ClusterId id = seed("k", "kappa", RunStatus.FAILED);
        store.markProvisioned(id);
        CapturingSink sink = new CapturingSink();

        matriarch.deleteCluster(id, "del-1", sink);
        assertTrue(executor.removed.contains(id));   // delegated to the executor

        matriarch.notifyRemoved(id);                 // agent confirmed removal
        assertNull(matriarch.getCluster(id));        // forgotten from the durable store
        assertTrue(sink.completed);
    }

    @Test
    void deleteOfNeverProvisionedClusterForgetsLocally() {
        ClusterId id = seed("m", "mu", RunStatus.PENDING);   // never provisioned
        CapturingSink sink = new CapturingSink();

        matriarch.deleteCluster(id, "del-1", sink);

        assertNull(matriarch.getCluster(id));        // forgotten immediately, no substrate to tear down
        assertTrue(executor.removed.isEmpty());      // executor not even asked
        assertTrue(sink.completed);
    }

    @Test
    void deferredDeleteHidesClusterButKeepsTombstone() {
        // Provisioned cluster; the executor can't reach an agent, so it defers via notifyDeletePending.
        ClusterId id = seed("n", "nu", RunStatus.FAILED);
        store.markProvisioned(id);
        CapturingSink sink = new CapturingSink();

        matriarch.deleteCluster(id, "del-1", sink);
        matriarch.notifyDeletePending(id, "no agent");

        // Gone from the user's view immediately...
        assertNull(matriarch.getCluster(id));
        assertTrue(matriarch.listClusters().isEmpty());
        // ...but the DELETING tombstone survives in the store: blocks adopt() resurrection + drives reconcile.
        assertNotNull(store.getDesired(id));
        assertEquals(RunIntent.DELETING, store.getDesiredRun(id));
        assertTrue(sink.completed);                  // stream completed (accepted), did not hang
    }

    @Test
    void reconcileActuatesDeferredDelete() {
        ClusterId id = seed("o", "omicron", RunStatus.FAILED);
        store.markProvisioned(id);
        store.setDesiredRun(id, RunIntent.DELETING);   // a delete was requested earlier and deferred

        matriarch.reconcile();                          // an agent reconnected

        assertTrue(executor.removed.contains(id));      // teardown actuated, not resurrected
    }

    // ---- helpers ----

    private ClusterId seed(String id, String name, RunStatus observed) {
        ClusterSpec s = spec(id, name);
        store.createDesired(s, "");
        statusCache.put(new ClusterStatus(s.id(), observed, List.of()));
        return s.id();
    }

    private static ClusterSpec spec(String id, String name) {
        return new ClusterSpec(new ClusterId(id), name, DatabaseEngine.POSTGRES, "18",
                List.of(), new CredentialSpec("postgres", false), TlsMode.SELF_SIGNED, null, Map.of());
    }

    private static final ProgressSink NOOP_SINK = new ProgressSink() {
        @Override public void onProgress(ClusterOperationProgress progress) { }
        @Override public void onComplete() { }
    };

    /** Records the terminal error and completion of a watch. */
    private static final class CapturingSink implements ProgressSink {
        volatile String lastError;
        volatile boolean completed;
        @Override public void onProgress(ClusterOperationProgress progress) {
            if (progress.error() != null) {
                lastError = progress.error();
            }
        }
        @Override public void onComplete() { completed = true; }
    }

    // ---- stub SPIs ----

    private static final class RecordingExecutor implements Executor {
        final Set<ClusterId> applied = new CopyOnWriteArraySet<>();
        final Set<ClusterId> removed = new CopyOnWriteArraySet<>();
        final Set<ClusterId> started = new CopyOnWriteArraySet<>();
        final Set<ClusterId> stopped = new CopyOnWriteArraySet<>();
        final Set<ClusterId> restarted = new CopyOnWriteArraySet<>();

        @Override public void apply(ClusterSpec desired) { applied.add(desired.id()); }
        @Override public void remove(ClusterSpec spec) { removed.add(spec.id()); }
        @Override public void start(ClusterSpec spec) { started.add(spec.id()); }
        @Override public void stop(ClusterSpec spec) { stopped.add(spec.id()); }
        @Override public void restart(ClusterSpec spec) { restarted.add(spec.id()); }
        @Override public Set<String> capabilities() { return Set.of(); }
    }

    private static final class NoopVersionCatalog implements VersionCatalog {
        @Override public String resolveVersion(DatabaseEngine engine, String versionDescription) { return versionDescription; }
        @Override public List<String> availableVersions(DatabaseEngine engine) { return List.of(); }
    }

    private static final class NoopExtensionCatalog implements ExtensionCatalog {
        @Override public List<Extension> resolveExtensions(DatabaseEngine engine, String version, List<Extension> requested) { return requested; }
        @Override public List<Extension> availableExtensions(DatabaseEngine engine, String version) { return List.of(); }
    }
}
