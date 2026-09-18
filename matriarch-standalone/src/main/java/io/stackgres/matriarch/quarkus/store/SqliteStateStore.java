package io.stackgres.matriarch.quarkus.store;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.spec.RunIntent;
import io.stackgres.matriarch.spi.StateStore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Durable {@link StateStore} on the shared {@link SqliteDatabase} (§3.2) for the standalone/bare-metal
 * matriarch: desired specs, idempotency keys, credentials and run intent survive a restart. Single-writer
 * by design (the matriarch is the sole writer), so no compare-and-swap. All access is serialized on the
 * shared database monitor ({@code synchronized (db)}), so the state store and the event log — which share
 * the one connection — never interleave transactions. The strongly-typed spec is stored as JSON text via
 * {@link ClusterSpecCodec}; the store governs serialization + storage only.
 */
public final class SqliteStateStore implements StateStore {

    private final SqliteDatabase db;
    private final Connection conn;
    private final SecretCipher cipher;

    public SqliteStateStore(SqliteDatabase db, SecretCipher cipher) {
        this.db = db;
        this.conn = db.connection();
        this.cipher = cipher;
        synchronized (db) {
            try (Statement st = conn.createStatement()) {
                st.execute("CREATE TABLE IF NOT EXISTS cluster_spec ("
                        + "id TEXT PRIMARY KEY, spec_json TEXT NOT NULL, "
                        + "desired_run TEXT NOT NULL DEFAULT 'RUNNING', "
                        + "provisioned INTEGER NOT NULL DEFAULT 0, updated_at TEXT NOT NULL)");
                st.execute("CREATE TABLE IF NOT EXISTS idempotency ("
                        + "key TEXT PRIMARY KEY, cluster_id TEXT NOT NULL)");
                st.execute("CREATE TABLE IF NOT EXISTS credential ("
                        + "cluster_id TEXT PRIMARY KEY, password TEXT NOT NULL)");
                // Migrate a store created before the 'provisioned' column existed (idempotent).
                ensureColumn("cluster_spec", "provisioned", "INTEGER NOT NULL DEFAULT 0");
            } catch (SQLException e) {
                throw new IllegalStateException("cannot initialize matriarch state store", e);
            }
        }
    }

    @Override
    public ClusterSpec getDesired(ClusterId id) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT spec_json FROM cluster_spec WHERE id=?")) {
                ps.setString(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? ClusterSpecCodec.fromJson(rs.getString(1)) : null;
                }
            } catch (SQLException e) {
                throw fail("getDesired", e);
            }
        }
    }

    @Override
    public List<ClusterSpec> listDesired() {
        synchronized (db) {
            List<ClusterSpec> out = new ArrayList<>();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT spec_json FROM cluster_spec")) {
                while (rs.next()) {
                    out.add(ClusterSpecCodec.fromJson(rs.getString(1)));
                }
                return out;
            } catch (SQLException e) {
                throw fail("listDesired", e);
            }
        }
    }

    @Override
    public void createDesired(ClusterSpec spec, String idempotencyKey) {
        synchronized (db) {
            try {
                conn.setAutoCommit(false);
                if (exists(spec.id())) {
                    throw new IllegalStateException("cluster already exists: " + spec.id().value());
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO cluster_spec(id, spec_json, desired_run, updated_at) VALUES(?,?,?,?)")) {
                    ps.setString(1, spec.id().value());
                    ps.setString(2, ClusterSpecCodec.toJson(spec));
                    ps.setString(3, RunIntent.RUNNING.name());
                    ps.setString(4, Instant.now().toString());
                    ps.executeUpdate();
                }
                if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT OR REPLACE INTO idempotency(key, cluster_id) VALUES(?,?)")) {
                        ps.setString(1, idempotencyKey);
                        ps.setString(2, spec.id().value());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                rollbackQuietly();
                throw fail("createDesired", e);
            } catch (RuntimeException e) {
                rollbackQuietly();
                throw e;
            } finally {
                resetAutoCommit();
            }
        }
    }

    @Override
    public void updateDesired(ClusterSpec spec) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE cluster_spec SET spec_json=?, updated_at=? WHERE id=?")) {
                ps.setString(1, ClusterSpecCodec.toJson(spec));
                ps.setString(2, Instant.now().toString());
                ps.setString(3, spec.id().value());
                if (ps.executeUpdate() == 0) {
                    // Upsert to mirror the in-memory put(); a fresh row defaults to RUNNING. An existing
                    // row keeps its desired_run — an update of the spec must not reset the run intent.
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO cluster_spec(id, spec_json, desired_run, updated_at) VALUES(?,?,?,?)")) {
                        ins.setString(1, spec.id().value());
                        ins.setString(2, ClusterSpecCodec.toJson(spec));
                        ins.setString(3, RunIntent.RUNNING.name());
                        ins.setString(4, Instant.now().toString());
                        ins.executeUpdate();
                    }
                }
            } catch (SQLException e) {
                throw fail("updateDesired", e);
            }
        }
    }

    @Override
    public void deleteDesired(ClusterId id) {
        synchronized (db) {
            try {
                exec("DELETE FROM cluster_spec WHERE id=?", id.value());
                exec("DELETE FROM credential WHERE cluster_id=?", id.value());
                exec("DELETE FROM idempotency WHERE cluster_id=?", id.value());
            } catch (SQLException e) {
                throw fail("deleteDesired", e);
            }
        }
    }

    @Override
    public ClusterId findByIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT cluster_id FROM idempotency WHERE key=?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new ClusterId(rs.getString(1)) : null;
                }
            } catch (SQLException e) {
                throw fail("findByIdempotencyKey", e);
            }
        }
    }

    @Override
    public boolean recordIdempotency(String key, ClusterId clusterId) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR IGNORE INTO idempotency(key, cluster_id) VALUES(?,?)")) {
                ps.setString(1, key);
                ps.setString(2, clusterId.value());
                return ps.executeUpdate() > 0;   // 1 = newly recorded, 0 = already present (a retry)
            } catch (SQLException e) {
                throw fail("recordIdempotency", e);
            }
        }
    }

    @Override
    public void putCredential(ClusterId id, String password) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO credential(cluster_id, password) VALUES(?,?)")) {
                ps.setString(1, id.value());
                ps.setString(2, cipher.encrypt(password));   // secret-classed: encrypted at rest (§3.7)
                ps.executeUpdate();
            } catch (SQLException e) {
                throw fail("putCredential", e);
            }
        }
    }

    @Override
    public String getCredential(ClusterId id) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT password FROM credential WHERE cluster_id=?")) {
                ps.setString(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? cipher.decrypt(rs.getString(1)) : null;
                }
            } catch (SQLException e) {
                throw fail("getCredential", e);
            }
        }
    }

    @Override
    public RunIntent getDesiredRun(ClusterId id) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT desired_run FROM cluster_spec WHERE id=?")) {
                ps.setString(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? RunIntent.valueOf(rs.getString(1)) : RunIntent.RUNNING;
                }
            } catch (SQLException e) {
                throw fail("getDesiredRun", e);
            }
        }
    }

    @Override
    public void setDesiredRun(ClusterId id, RunIntent intent) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE cluster_spec SET desired_run=? WHERE id=?")) {
                ps.setString(1, intent.name());
                ps.setString(2, id.value());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw fail("setDesiredRun", e);
            }
        }
    }

    @Override
    public boolean isProvisioned(ClusterId id) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT provisioned FROM cluster_spec WHERE id=?")) {
                ps.setString(1, id.value());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) != 0;
                }
            } catch (SQLException e) {
                throw fail("isProvisioned", e);
            }
        }
    }

    @Override
    public void markProvisioned(ClusterId id) {
        synchronized (db) {
            try (PreparedStatement ps = conn.prepareStatement("UPDATE cluster_spec SET provisioned=1 WHERE id=?")) {
                ps.setString(1, id.value());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw fail("markProvisioned", e);
            }
        }
    }

    // ---- helpers (called under synchronized (db)) ----

    /** Add {@code column} to {@code table} if a pre-existing store predates it; a no-op otherwise. */
    private void ensureColumn(String table, String column, String definition) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM pragma_table_info(?) WHERE name=?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;   // column already present
                }
            }
        }
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    private boolean exists(ClusterId id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM cluster_spec WHERE id=?")) {
            ps.setString(1, id.value());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void exec(String sql, String arg) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, arg);
            ps.executeUpdate();
        }
    }

    private void rollbackQuietly() {
        try {
            conn.rollback();
        } catch (SQLException ignore) {
            // best effort
        }
    }

    private void resetAutoCommit() {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException ignore) {
            // best effort
        }
    }

    private static IllegalStateException fail(String op, SQLException e) {
        return new IllegalStateException("state store " + op + " failed", e);
    }
}
