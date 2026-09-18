package io.stackgres.matriarch.quarkus.store;

import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * One shared SQLite connection for the standalone matriarch's durable state — the state store and the
 * (separate-table) event log both run on it, in-band and synchronous. The workload is kubectl-like: a
 * handful of small writes per operation, dwarfed by the seconds an actual provisioning takes, so a
 * single connection has ~100× headroom and one writer lock is simpler and safer than multiple
 * connections (no {@code SQLITE_BUSY}, no double-fsync contention, one file to back up). Callers
 * serialize their transactions by synchronizing on this instance — the shared monitor — so state-store
 * and event-log transactions never interleave on the one connection.
 *
 * <p>WAL + {@code synchronous=NORMAL}: commits append to the WAL and defer fsync to checkpoint, so each
 * write is ~tens of µs. Works in GraalVM native-image: sqlite-jdbc's bundled Feature registers the JNI
 * native lib, and we open the connection via {@link SQLiteDataSource} directly rather than through
 * {@code DriverManager} — the latter needs JDBC {@code ServiceLoader} discovery that isn't wired up in a
 * native binary without a Quarkus JDBC extension.
 */
public final class SqliteDatabase implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(SqliteDatabase.class.getName());

    private final Connection conn;

    public SqliteDatabase(String path) {
        try {
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:" + path);
            this.conn = dataSource.getConnection();
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA journal_mode=WAL");
                st.execute("PRAGMA synchronous=NORMAL");
                st.execute("PRAGMA busy_timeout=5000");   // belt-and-suspenders; the shared lock already serializes
            }
            // Restrict the DB (and its WAL/SHM sidecars, created by the WAL PRAGMA above) to owner-only —
            // it holds secret-classed data (§3.7). SQLite propagates the DB file's mode to WAL/SHM it
            // recreates later, so this also covers subsequent sidecar recreation.
            FilePermissions.restrictToOwner(Path.of(path));
            FilePermissions.restrictToOwner(Path.of(path + "-wal"));
            FilePermissions.restrictToOwner(Path.of(path + "-shm"));
        } catch (SQLException e) {
            LOG.log(System.Logger.Level.ERROR, "cannot open matriarch database at {0}: {1}", path, e.toString());
            throw new IllegalStateException("cannot open matriarch database at " + path, e);
        }
    }

    /** The shared connection. Every use must be guarded by {@code synchronized (thisDatabase)}. */
    public Connection connection() {
        return conn;
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new IllegalStateException("failed to close matriarch database", e);
        }
    }
}
