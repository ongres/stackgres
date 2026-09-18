package io.stackgres.matriarch.quarkus.event;

import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.quarkus.store.SqliteDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Durable {@link ClusterEventLog} on the shared {@link SqliteDatabase} — a SEPARATE table
 * ({@code cluster_event}) from the state store, but the SAME connection, so this best-effort history
 * adds no second connection/file and never contends for the SQLite write lock (all access serializes on
 * the shared database monitor). Bounded per cluster; dropped on cluster delete.
 * <strong>Best-effort</strong>: append/read failures are logged and swallowed — a broken event log must
 * never break a mutation (the core never reads these events).
 */
public final class SqliteClusterEventLog implements ClusterEventLog {

    private static final System.Logger LOG = System.getLogger(SqliteClusterEventLog.class.getName());

    private final SqliteDatabase db;
    private final Connection conn;

    public SqliteClusterEventLog(SqliteDatabase db) {
        this.db = db;
        this.conn = db.connection();
        synchronized (db) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS cluster_event ("
                        + "seq INTEGER PRIMARY KEY AUTOINCREMENT, cluster_id TEXT NOT NULL, "
                        + "event_json TEXT NOT NULL)");
                st.execute("CREATE INDEX IF NOT EXISTS cluster_event_by_cluster ON cluster_event(cluster_id, seq)");
            } catch (SQLException e) {
                throw new IllegalStateException("cannot initialize matriarch event log", e);
            }
        }
    }

    @Override
    public void append(ClusterEvent event) {
        synchronized (db) {
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO cluster_event(cluster_id, event_json) VALUES(?,?)")) {
                    ps.setString(1, event.clusterId().value());
                    ps.setString(2, ClusterEventCodec.toJson(event));
                    ps.executeUpdate();
                }
                // Evict oldest beyond the per-cluster bound.
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM cluster_event WHERE cluster_id=? AND seq NOT IN "
                        + "(SELECT seq FROM cluster_event WHERE cluster_id=? ORDER BY seq DESC LIMIT ?)")) {
                    ps.setString(1, event.clusterId().value());
                    ps.setString(2, event.clusterId().value());
                    ps.setInt(3, MAX_PER_CLUSTER);
                    ps.executeUpdate();
                }
            } catch (RuntimeException | SQLException e) {
                // Best-effort: never let a broken event log break the mutation that raised the event.
                LOG.log(System.Logger.Level.WARNING, "failed to record cluster event (dropping it)", e);
            }
        }
    }

    @Override
    public List<ClusterEvent> events(ClusterId id) {
        synchronized (db) {
            List<ClusterEvent> out = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT event_json FROM cluster_event WHERE cluster_id=? ORDER BY seq ASC")) {
                ps.setString(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        out.add(ClusterEventCodec.fromJson(rs.getString(1)));
                    }
                }
            } catch (RuntimeException | SQLException e) {
                LOG.log(System.Logger.Level.WARNING, "failed to read cluster events for {0}", id.value(), e);
            }
            return out;
        }
    }

    @Override
    public void deleteFor(ClusterId id) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cluster_event WHERE cluster_id=?")) {
                ps.setString(1, id.value());
                ps.executeUpdate();
            } catch (SQLException e) {
                LOG.log(System.Logger.Level.WARNING, "failed to drop cluster events for {0}", id.value(), e);
            }
        }
    }
}
