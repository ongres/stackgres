package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.quarkus.store.SqliteDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The durable {@link SqliteClusterEventLog}: history survives a reopen (restart), preserves chronological
 * order and per-event fields, is bounded per cluster, and drops on cluster delete.
 */
class SqliteClusterEventLogTest {

    @TempDir
    Path tmp;

    private String dbPath() {
        return tmp.resolve("events.db").toString();
    }

    private static final Instant T0 = Instant.parse("2026-09-17T10:00:00Z");

    @Test
    void historySurvivesReopenInOrderWithFields() {
        ClusterId id = new ClusterId("a");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteClusterEventLog log = new SqliteClusterEventLog(db);
            log.append(new ClusterEvent.ClusterAccepted(T0, id, "alpha", "18", true));
            log.append(new ClusterEvent.ClusterStarting(T0.plusSeconds(1), id));
            log.append(new ClusterEvent.ClusterFailed(T0.plusSeconds(2), id, "image not found"));
        }
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteClusterEventLog log = new SqliteClusterEventLog(db);
            List<ClusterEvent> events = log.events(id);
            assertEquals(3, events.size());
            var accepted = assertInstanceOf(ClusterEvent.ClusterAccepted.class, events.get(0));
            assertEquals("alpha", accepted.name());
            assertEquals("18", accepted.version());
            assertTrue(accepted.standalone());
            assertEquals(T0, accepted.timestamp());
            assertInstanceOf(ClusterEvent.ClusterStarting.class, events.get(1));
            var failed = assertInstanceOf(ClusterEvent.ClusterFailed.class, events.get(2));
            assertEquals("image not found", failed.reason());
        }
    }

    @Test
    void boundedPerCluster() {
        ClusterId id = new ClusterId("b");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteClusterEventLog log = new SqliteClusterEventLog(db);
            for (int i = 0; i < ClusterEventLog.MAX_PER_CLUSTER + 50; i++) {
                log.append(new ClusterEvent.ClusterStarting(T0.plusSeconds(i), id));
            }
            assertEquals(ClusterEventLog.MAX_PER_CLUSTER, log.events(id).size());
        }
    }

    @Test
    void deleteForDropsHistory() {
        ClusterId id = new ClusterId("c");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteClusterEventLog log = new SqliteClusterEventLog(db);
            log.append(new ClusterEvent.ClusterAccepted(T0, id, "gamma", "18", true));
            log.deleteFor(id);
            assertTrue(log.events(id).isEmpty());
        }
    }

    @Test
    void historyIsPerCluster() {
        ClusterId a = new ClusterId("d");
        ClusterId b = new ClusterId("e");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteClusterEventLog log = new SqliteClusterEventLog(db);
            log.append(new ClusterEvent.ClusterHealthy(T0, a));
            log.append(new ClusterEvent.ClusterHealthy(T0, b));
            log.append(new ClusterEvent.ClusterHealthy(T0.plusSeconds(1), a));
            assertEquals(2, log.events(a).size());
            assertEquals(1, log.events(b).size());
        }
    }
}
