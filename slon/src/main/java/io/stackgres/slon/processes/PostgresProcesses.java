package io.stackgres.slon.processes;

import io.stackgres.proto.slon.TlsConfig;
import io.stackgres.slon.SlonSystem;
import io.stackgres.slon.ssl.TlsCertificates;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.*;

public class PostgresProcesses extends Processes {

    private static final System.Logger logger = System.getLogger("PostgresProcesses");
    private static final Path PGDATA = Path.of("/postgres/data");
    private static final String CSVLOG_DIR = "/tmp/postgres/log";

    private static final String CSVLOG_OPTIONS = " -c log_destination=csvlog -c logging_collector=on -c log_directory=" + CSVLOG_DIR + " -c log_filename=postgresql-%H"
                                                 + " -c log_rotation_size=10MB -c log_rotation_age=0 -c log_truncate_on_rotation=on -c log_file_mode=0644";

    private final String username;
    private final String password;
    private final String listenAddress;
    private final String ivorySqlPort;
    private volatile boolean tlsEnabled;

    public PostgresProcesses() {
        username = System.getenv("POSTGRES_USER");
        password = System.getenv("POSTGRES_PASSWORD");
        listenAddress = System.getenv("POSTGRES_LISTEN_ADDRESS");
        ivorySqlPort = System.getenv("IVORYSQL_PORT");
    }

    @Override
    public void initDb() {
        // Idempotent by probing reality (§10.1): slon keeps no ledger, so the matriarch can safely
        // re-send a command whose outcome it is unsure about (e.g. after a dropped stream). PG_VERSION
        // present means the data dir is already initialized — a no-op, not an initdb error.
        if (Files.exists(PGDATA.resolve("PG_VERSION"))) {
            logger.log(System.Logger.Level.INFO, "PGDATA already initialized (PG_VERSION present) — skipping initdb");
            return;
        }
        // Non-empty PGDATA without PG_VERSION is an interrupted initdb; clear it so initdb can rerun
        // (initdb refuses a non-empty target). Only reached when the dir was never a complete data dir.
        if (isNonEmpty(PGDATA)) {
            logger.log(System.Logger.Level.WARNING, "PGDATA non-empty but uninitialized (interrupted initdb) — clearing and reinitializing");
            clearDirectory(PGDATA);
        }
        Path file;
        try {
            file = writePasswordFile();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write init password file", e);
        }
        ProcessBuilder processBuilder = new ProcessBuilder("initdb", "--username=" + username, "--pwfile=" + file.toAbsolutePath());
        processBuilder.environment().put("PGDATA", "/postgres/data");
        runCommand(processBuilder);
    }

    private static boolean isNonEmpty(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (var entries = Files.list(dir)) {
            return entries.findAny().isPresent();
        } catch (IOException e) {
            return false;
        }
    }

    /** Delete the directory's contents, keeping the (mounted) directory itself, so initdb can rerun. */
    private static void clearDirectory(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(p -> !p.equals(dir))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (IOException | UncheckedIOException e) {
            throw new RuntimeException("Failed to clear PGDATA " + dir, e);
        }
    }

    private Path writePasswordFile() throws IOException {
        Path file = Files.createTempFile("passwd", "");
        Files.writeString(file, password + "\n");
        Set<PosixFilePermission> perms = Set.of(OWNER_WRITE, OWNER_READ, GROUP_READ, OTHERS_READ);
        Files.setPosixFilePermissions(file, perms);
        return file;
    }

    @Override
    public void configureTls(TlsConfig tlsConfig) {
        if (tlsConfig.hasNoTls()) {
            logger.log(System.Logger.Level.INFO, "TLS explicitly disabled");
            tlsEnabled = false;
        } else if (tlsConfig.hasProvided()) {
            byte[] cert = tlsConfig.getProvided().getCertificate().toByteArray();
            byte[] key = tlsConfig.getProvided().getPrivateKey().toByteArray();
            TlsCertificates.writeProvidedCertificates(cert, key, PGDATA);
            tlsEnabled = true;
        } else if (tlsConfig.hasSelfSignedCerts()) {
            String clusterName = SlonSystem.getClusterName();
            TlsCertificates.generateSelfSigned(clusterName, PGDATA);
            tlsEnabled = true;
        }
    }

    public void startPostgres(String port) {
        // Idempotent: if Postgres is already accepting connections a re-sent StartDb is a no-op (pg_ctl
        // start would otherwise error with "another server might be running").
        if (healthcheck(port)) {
            logger.log(System.Logger.Level.INFO, "Postgres already accepting connections on port {0} — skipping start", port);
            return;
        }
        prepareCsvlogDir();
        String options = "-c listen_addresses='" + listenAddress + "' -p " + port;
        if (tlsEnabled)
            options += " -c ssl=on";
        options += CSVLOG_OPTIONS;
        if (ivorySqlPort != null && !ivorySqlPort.isBlank())
            options += " -c ivorysql.port=" + ivorySqlPort;
        runCommand(new ProcessBuilder("pg_ctl", "-D", "/postgres/data", "-l", "/tmp/server.log", "-o", options, "-w", "start"));
    }

    private void prepareCsvlogDir() {
        try {
            Files.createDirectories(Path.of(CSVLOG_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create csvlog dir " + CSVLOG_DIR, e);
        }
    }

    @Override
    public void stopPostgres() {
        // Idempotent: no postmaster.pid means nothing to stop; a re-sent StopDb is a no-op (pg_ctl stop
        // would otherwise error with "PID file does not exist").
        if (!Files.exists(PGDATA.resolve("postmaster.pid"))) {
            logger.log(System.Logger.Level.INFO, "Postgres not running (no postmaster.pid) — skipping stop");
            return;
        }
        runCommand(new ProcessBuilder("pg_ctl", "-D", "/postgres/data", "-m", "smart", "-w", "stop"));
    }

    @Override
    public boolean healthcheck(String port) {
        return runCommand(new ProcessBuilder("pg_isready", "-U", username, "-d", username, "-p", port, "-h", "/tmp"), false, false);
    }

    @Override
    public String pgControlData() {
        try {
            return runCommand(new ProcessBuilder("pg_controldata", "-D", "/postgres/data"), true);
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Failed to retrieve pg_controldata: {0}", e.getMessage());
            return null;
        }
    }

    @Override
    public long dbSize(String port) {
        try {
            String result = runCommand(new ProcessBuilder("psql", "-U", username, "-d", username, "-p", port, "-t", "-A", "-c",
                    "SELECT pg_database_size(current_database())"), true);
            if (result != null && !result.isBlank())
                return Long.parseLong(result.strip());
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Failed to retrieve database size: {0}", e.getMessage());
        }
        return 0;
    }

}