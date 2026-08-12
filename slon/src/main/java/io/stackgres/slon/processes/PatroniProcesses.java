package io.stackgres.slon.processes;

import io.stackgres.proto.slon.ReplicationStatus;
import io.stackgres.proto.slon.TlsConfig;
import io.stackgres.slon.Config;
import io.stackgres.slon.SlonSystem;
import io.stackgres.slon.ssl.TlsCertificates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PatroniProcesses extends Processes {

    private static final System.Logger logger = System.getLogger("PatroniProcesses");

    private static final Path TLS_DIR = Path.of("/postgres/home/tls");
    private static final Path TLS_CERT = TLS_DIR.resolve("server.crt");
    private static final Path TLS_KEY = TLS_DIR.resolve("server.key");

    private Process process;
    private volatile boolean tlsEnabled;

    @Override
    public void initDb() {
        // nothing to do
    }

    @Override
    public void configureTls(TlsConfig tlsConfig) {
        if (tlsConfig.hasNoTls()) {
            logger.log(System.Logger.Level.INFO, "TLS explicitly disabled");
            tlsEnabled = false;
            return;
        }
        try {
            Files.createDirectories(TLS_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create TLS directory " + TLS_DIR, e);
        }
        if (tlsConfig.hasProvided()) {
            byte[] cert = tlsConfig.getProvided().getCertificate().toByteArray();
            byte[] key = tlsConfig.getProvided().getPrivateKey().toByteArray();
            TlsCertificates.writeProvidedCertificates(cert, key, TLS_DIR);
            tlsEnabled = true;
        } else if (tlsConfig.hasSelfSignedCerts()) {
            String clusterName = SlonSystem.getClusterName();
            TlsCertificates.generateSelfSigned(clusterName, TLS_DIR);
            tlsEnabled = true;
        }
    }

    @Override
    public void startPostgres(String port) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/entrypoint.sh");
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(new File("/tmp/entrypoint-out.log")));
            if (tlsEnabled) {
                processBuilder.environment().put("PATRONI_SSL_ENABLED", "true");
                processBuilder.environment().put("PATRONI_SSL_CERT_FILE", TLS_CERT.toString());
                processBuilder.environment().put("PATRONI_SSL_KEY_FILE", TLS_KEY.toString());
            }
            process = processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to run command", e);
        }
    }

    @Override
    public void stopPostgres() {
        if (process != null) {
            try {
                process.destroy();
                process.waitFor();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            process = null;
        }
    }

    @Override
    public String pgControlData() {
        try {
            return runCommand(new ProcessBuilder("pg_controldata", "-D", "/postgres/home/data"), true);
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Failed to retrieve pg_controldata: {0}", e.getMessage());
            return null;
        }
    }

    @Override
    public long dbSize(String port) {
        try {
            String result = runCommand(new ProcessBuilder("psql", "-U", "postgres", "-d", "postgres", "-p", port, "-t", "-A", "-c",
                    "SELECT pg_database_size(current_database())"), true);
            if (result != null && !result.isBlank())
                return Long.parseLong(result.strip());
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Failed to retrieve database size: {0}", e.getMessage());
        }
        return 0;
    }

    @Override
    public boolean healthcheck(String port) {
        try {
            // TODO improve
            String output = runCommand(new ProcessBuilder("/patroni/patronictl", "-c", "/postgres/home/config.yml", "list", "-f", "tsv"), true);
            return output.contains("running");
        } catch (Exception e) {
            return false;
        }
    }

    public ReplicationStatus replicationStatus() {
        String memberName = Config.getValue("PATRONI_NAME", null);
        if (memberName == null)
            return ReplicationStatus.REPLICATION_STATUS_UNKNOWN;
        try {
            String output = runCommand(new ProcessBuilder("/patroni/patronictl", "-c", "/postgres/home/config.yml", "list", "-f", "tsv"), true);
            for (String line : output.split("\n")) {
                String[] cols = line.split("\t");
                if (cols.length < 4) continue;
                if (memberName.equals(cols[1].trim())) {
                    return parsePatroniRole(cols[3].trim());
                }
            }
            return ReplicationStatus.REPLICATION_STATUS_UNKNOWN;
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Failed to retrieve Patroni role: {0}", e.getMessage());
            return ReplicationStatus.REPLICATION_STATUS_UNKNOWN;
        }
    }

    private static ReplicationStatus parsePatroniRole(String role) {
        return switch (role) {
            case "Leader" -> ReplicationStatus.REPLICATION_STATUS_PRIMARY;
            case "Replica", "Sync Standby", "Standby Leader" -> ReplicationStatus.REPLICATION_STATUS_REPLICA;
            default -> ReplicationStatus.REPLICATION_STATUS_UNKNOWN;
        };
    }

}