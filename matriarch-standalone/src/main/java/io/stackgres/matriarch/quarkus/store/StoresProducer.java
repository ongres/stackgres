package io.stackgres.matriarch.quarkus.store;

import io.stackgres.matriarch.quarkus.event.ClusterEventLog;
import io.stackgres.matriarch.quarkus.event.InMemoryClusterEventLog;
import io.stackgres.matriarch.quarkus.event.SqliteClusterEventLog;
import io.stackgres.matriarch.spi.StateStore;
import io.stackgres.matriarch.spi.StatusCache;
import io.stackgres.matriarch.store.InMemoryStateStore;
import io.stackgres.matriarch.store.InMemoryStatusCache;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Produces the matriarch's persistence beans: the {@link StateStore} (desired specs + run intent — the
 * source of truth), the {@link StatusCache} (observed status — always in-memory, re-derivable), and the
 * {@link ClusterEventLog} (a separate, bounded, best-effort per-cluster event history). Selectable via
 * {@code matriarch.store}: {@code sqlite} puts durable state on ONE shared local SQLite file — state and
 * the event log share a single connection, written in-band (the workload is kubectl-like, so one
 * connection has ample headroom and one writer lock is simpler than many) — or {@code memory} for the
 * non-durable dev stores. A durable backend swaps in with no change to the core.
 */
@ApplicationScoped
public class StoresProducer {

    @ConfigProperty(name = "matriarch.store")
    String storeType;

    @ConfigProperty(name = "matriarch.store.sqlite.path")
    String sqlitePath;

    // Base64 AES-256 key for encrypting secret-classed values (credentials) at rest (§3.7). Absent →
    // an auto-generated 0600 keyfile next to the DB (<db>.key). Supply this (env/secret manager) to keep
    // the key off the DB's disk. Optional (not an empty default — that fails to resolve in native mode).
    @ConfigProperty(name = "matriarch.secret.key")
    java.util.Optional<String> secretKeyBase64;

    private SqliteDatabase database;
    private SecretCipher secretCipher;

    private boolean sqlite() {
        return "sqlite".equalsIgnoreCase(storeType);
    }

    /** The one shared SQLite connection, created lazily and reused by the state store and the event log. */
    private synchronized SqliteDatabase database() {
        if (database == null) {
            database = new SqliteDatabase(sqlitePath);
        }
        return database;
    }

    private synchronized SecretCipher secretCipher() {
        if (secretCipher == null) {
            secretCipher = SecretCipher.load(secretKeyBase64.orElse(null), java.nio.file.Path.of(sqlitePath + ".key"));
        }
        return secretCipher;
    }

    @Produces
    @ApplicationScoped
    public StateStore stateStore() {
        return sqlite() ? new SqliteStateStore(database(), secretCipher()) : new InMemoryStateStore();
    }

    @Produces
    @ApplicationScoped
    public StatusCache statusCache() {
        return new InMemoryStatusCache();
    }

    @Produces
    @ApplicationScoped
    public ClusterEventLog clusterEventLog() {
        return sqlite() ? new SqliteClusterEventLog(database()) : new InMemoryClusterEventLog();
    }

    @PreDestroy
    void close() {
        if (database != null) {
            database.close();
        }
    }
}
