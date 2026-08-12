package io.stackgres.matriarch;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.event.Event;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.spec.CredentialSpec;
import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.Extension;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the authoritative-observer sync path {@link Matriarch#reconcileObserved}. */
class MatriarchReconcileObservedTest {

    private final List<Event> events = new ArrayList<>();
    private Matriarch matriarch;

    @BeforeEach
    void setUp() {
        events.clear();
        matriarch = new Matriarch(new InMemoryStateStore(), new InMemoryStatusCache(),
                new NoopExecutor(), events::add, new NoopVersionCatalog(), new NoopExtensionCatalog());
    }

    @Test
    void adoptsNewClusters() {
        matriarch.reconcileObserved(List.of(cluster("a", "alpha"), cluster("b", "beta")));

        assertEquals(Set.of("alpha", "beta"), observedNames());
        assertEquals(2, countEvents(ClusterEvent.ClusterObserved.class));
    }

    @Test
    void updatesKnownAndForgetsAbsent() {
        matriarch.reconcileObserved(List.of(cluster("a", "alpha"), cluster("b", "beta")));
        events.clear();

        // 'a' stays, 'b' disappears, 'c' is new — the observation is authoritative.
        matriarch.reconcileObserved(List.of(cluster("a", "alpha"), cluster("c", "gamma")));

        assertEquals(Set.of("alpha", "gamma"), observedNames());
        assertEquals(1, countEvents(ClusterEvent.ClusterObserved.class));   // only 'c'
        assertEquals(1, countEvents(ClusterEvent.ClusterDeleted.class));     // only 'b'
        assertNull(matriarch.getCluster(new ClusterId("b")));
        assertNotNull(matriarch.getCluster(new ClusterId("a")));
    }

    @Test
    void reflectsStatusChangeOnKnownCluster() {
        matriarch.reconcileObserved(List.of(cluster("a", "alpha", RunStatus.PENDING)));
        matriarch.reconcileObserved(List.of(cluster("a", "alpha", RunStatus.HEALTHY)));

        assertEquals(RunStatus.HEALTHY, matriarch.getCluster(new ClusterId("a")).status().runStatus());
        // The PENDING→HEALTHY transition emits exactly one ClusterHealthy event...
        assertEquals(1, countEvents(ClusterEvent.ClusterHealthy.class));
        // ...and it is neither a new adoption nor a deletion.
        assertEquals(0, countEvents(ClusterEvent.ClusterDeleted.class));
    }

    @Test
    void emitsFailedOnTransitionToFailed() {
        matriarch.reconcileObserved(List.of(cluster("a", "alpha", RunStatus.HEALTHY)));   // new → Recovered
        matriarch.reconcileObserved(List.of(cluster("a", "alpha", RunStatus.FAILED)));    // HEALTHY → FAILED

        assertEquals(1, countEvents(ClusterEvent.ClusterFailed.class));
    }

    @Test
    void reObservingSameStatusEmitsNoTransition() {
        matriarch.reconcileObserved(List.of(cluster("a", "alpha", RunStatus.HEALTHY)));   // new → Recovered
        events.clear();
        matriarch.reconcileObserved(List.of(cluster("a", "alpha", RunStatus.HEALTHY)));   // unchanged

        assertEquals(0, countEvents(ClusterEvent.ClusterHealthy.class));
        assertEquals(0, countEvents(ClusterEvent.ClusterFailed.class));
        assertEquals(0, countEvents(ClusterEvent.ClusterStarting.class));
    }

    @Test
    void emptyObservationForgetsAll() {
        matriarch.reconcileObserved(List.of(cluster("a", "alpha")));
        matriarch.reconcileObserved(List.of());

        assertTrue(matriarch.listClusters().isEmpty());
    }

    // ---- helpers ----

    private static Cluster cluster(String id, String name) {
        return cluster(id, name, RunStatus.HEALTHY);
    }

    private static Cluster cluster(String id, String name, RunStatus status) {
        ClusterId cid = new ClusterId(id);
        ClusterSpec spec = new ClusterSpec(cid, name, DatabaseEngine.POSTGRES, "18",
                List.of(), new CredentialSpec("postgres", false), TlsMode.SELF_SIGNED, null, Map.of());
        return new Cluster(spec, new ClusterStatus(cid, status, List.of()));
    }

    private Set<String> observedNames() {
        return matriarch.listClusters().stream().map(c -> c.spec().name()).collect(Collectors.toSet());
    }

    private long countEvents(Class<? extends Event> type) {
        return events.stream().filter(type::isInstance).count();
    }

    // ---- stub SPIs (unused by reconcileObserved) ----

    private static final class NoopExecutor implements Executor {
        @Override public void apply(ClusterSpec desired) { }
        @Override public void remove(ClusterSpec spec) { }
        @Override public void start(ClusterSpec spec) { }
        @Override public void stop(ClusterSpec spec) { }
        @Override public void restart(ClusterSpec spec) { }
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
