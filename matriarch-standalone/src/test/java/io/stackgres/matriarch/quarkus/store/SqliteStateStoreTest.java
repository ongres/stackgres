package io.stackgres.matriarch.quarkus.store;

import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.InstanceId;
import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.spec.CredentialSpec;
import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.model.spec.InstanceRole;
import io.stackgres.matriarch.model.spec.InstanceSpec;
import io.stackgres.matriarch.model.spec.PostgresSpec;
import io.stackgres.matriarch.model.spec.RunIntent;
import io.stackgres.matriarch.model.spec.TlsMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The durable SQLite {@link SqliteStateStore}: desired specs, credentials, idempotency keys and run
 * intent survive a store reopen (i.e. a matriarch restart), the JSON round-trip is lossless, and the
 * existing-SPI semantics (dup rejection, once-only idempotency, delete-clears-all) hold.
 */
class SqliteStateStoreTest {

    @TempDir
    Path tmp;

    // Fixed test key (32 bytes) — real deployments resolve it from config or a 0600 keyfile.
    private final SecretCipher cipher = new SecretCipher(new byte[32]);

    private String dbPath() {
        return tmp.resolve("matriarch.db").toString();
    }

    private static ClusterSpec sampleSpec(String id, String name) {
        PostgresSpec pg = new PostgresSpec(
                List.of(new Extension("pgvector", "0.7.0", "1")),
                Map.of("max_connections", "200"));
        InstanceSpec primary = new InstanceSpec(
                new InstanceId(id + "-0"), InstanceRole.PRIMARY, 5432, "0.0.0.0", pg);
        return new ClusterSpec(new ClusterId(id), name, DatabaseEngine.POSTGRES, "18",
                List.of(primary), new CredentialSpec("postgres", true), TlsMode.SELF_SIGNED, pg,
                Map.of("team", "db", "env", "prod"));
    }

    @Test
    void desiredSpecSurvivesReopen() {
        ClusterSpec spec = sampleSpec("a", "alpha");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s1 = new SqliteStateStore(db, cipher);
            s1.createDesired(spec, "key-1");
            s1.putCredential(spec.id(), "s3cret");
        }
        // Reopen the same file — simulates a matriarch restart.
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s2 = new SqliteStateStore(db, cipher);
            assertEquals(spec, s2.getDesired(spec.id()));          // full JSON round-trip, value-equal
            assertEquals(spec.id(), s2.findByIdempotencyKey("key-1"));
            assertEquals("s3cret", s2.getCredential(spec.id()));
            assertEquals(1, s2.listDesired().size());
        }
    }

    @Test
    void desiredRunSurvivesReopen() {
        ClusterSpec spec = sampleSpec("b", "beta");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s1 = new SqliteStateStore(db, cipher);
            s1.createDesired(spec, "");
            assertEquals(RunIntent.RUNNING, s1.getDesiredRun(spec.id()));   // default
            s1.setDesiredRun(spec.id(), RunIntent.STOPPED);                 // user stopped it
        }
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s2 = new SqliteStateStore(db, cipher);
            assertEquals(RunIntent.STOPPED, s2.getDesiredRun(spec.id()));   // intent persisted
        }
    }

    @Test
    void updateDesiredPreservesRunIntent() {
        ClusterSpec spec = sampleSpec("f", "phi");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s = new SqliteStateStore(db, cipher);
            s.createDesired(spec, "");
            s.setDesiredRun(spec.id(), RunIntent.STOPPED);
            s.updateDesired(sampleSpec("f", "phi-renamed"));               // spec changes...
            assertEquals(RunIntent.STOPPED, s.getDesiredRun(spec.id()));   // ...run intent must not reset
            assertEquals("phi-renamed", s.getDesired(spec.id()).name());
        }
    }

    @Test
    void provisionedFlagSurvivesReopenAndDefaultsFalse() {
        ClusterSpec spec = sampleSpec("g", "gamma");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s1 = new SqliteStateStore(db, cipher);
            s1.createDesired(spec, "");
            assertFalse(s1.isProvisioned(spec.id()));   // default for a fresh create
            s1.markProvisioned(spec.id());
        }
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s2 = new SqliteStateStore(db, cipher);
            assertTrue(s2.isProvisioned(spec.id()));     // latched fact persisted across restart
        }
    }

    @Test
    void createRejectsDuplicateId() {
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s = new SqliteStateStore(db, cipher);
            s.createDesired(sampleSpec("c", "gamma"), "");
            assertThrows(IllegalStateException.class,
                    () -> s.createDesired(sampleSpec("c", "gamma-again"), ""));
        }
    }

    @Test
    void recordIdempotencyIsOnce() {
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s = new SqliteStateStore(db, cipher);
            ClusterId id = new ClusterId("d");
            s.createDesired(sampleSpec("d", "delta"), "");
            assertTrue(s.recordIdempotency("op-1", id));
            assertFalse(s.recordIdempotency("op-1", id));   // retry under the same key
        }
    }

    @Test
    void deleteClearsEverything() {
        ClusterSpec spec = sampleSpec("e", "epsilon");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s = new SqliteStateStore(db, cipher);
            s.createDesired(spec, "key-e");
            s.putCredential(spec.id(), "pw");
            s.setDesiredRun(spec.id(), RunIntent.STOPPED);
            s.deleteDesired(spec.id());
            assertNull(s.getDesired(spec.id()));
            assertNull(s.getCredential(spec.id()));
            assertNull(s.findByIdempotencyKey("key-e"));
            assertEquals(RunIntent.RUNNING, s.getDesiredRun(spec.id()));   // back to the default
            assertTrue(s.listDesired().isEmpty());
        }
    }

    @Test
    void credentialIsEncryptedAtRest() throws Exception {
        ClusterSpec spec = sampleSpec("h", "eta");
        try (SqliteDatabase db = new SqliteDatabase(dbPath())) {
            SqliteStateStore s = new SqliteStateStore(db, cipher);
            s.createDesired(spec, "");
            s.putCredential(spec.id(), "s3cret-pw");
            assertEquals("s3cret-pw", s.getCredential(spec.id()));   // round-trips through the API
            // ...but the raw column holds ciphertext, not the plaintext password
            try (var st = db.connection().createStatement();
                 var rs = st.executeQuery("SELECT password FROM credential WHERE cluster_id='h'")) {
                assertTrue(rs.next());
                String raw = rs.getString(1);
                assertTrue(raw.startsWith("v1:"), "stored credential should be encrypted, was: " + raw);
                assertFalse(raw.contains("s3cret-pw"));
            }
        }
    }

    @Test
    void cipherRoundTripsAndReadsLegacyPlaintext() {
        String encrypted = cipher.encrypt("secret");
        assertTrue(encrypted.startsWith("v1:"));
        assertEquals("secret", cipher.decrypt(encrypted));
        // a pre-encryption value (no "v1:" prefix) is returned as-is, so an existing store keeps working
        assertEquals("legacy-plain", cipher.decrypt("legacy-plain"));
    }
}
