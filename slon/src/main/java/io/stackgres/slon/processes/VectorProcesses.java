package io.stackgres.slon.processes;

import io.stackgres.slon.SlonSystem;
import io.stackgres.slon.vector.VectorPipelineTemplates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Supervises the curated vector-agent process.
 *
 * <p>Lifecycle: {@link #start()} renders the runtime config from the bundled
 * template (standalone vs. Patroni picked from {@code PATRONI_NAME}), launches
 * {@code vector --config /tmp/postgres/vector/vector.yaml}, and spawns a monitor
 * thread that restarts the process with capped exponential backoff. After a
 * per-window failure threshold, the supervisor stops trying — Postgres keeps
 * running.
 *
 * <p>{@link #stop()} signals the monitor to stop restarting, then SIGTERMs
 * the running process, and waits for the disk-buffer flush.
 */
public final class VectorProcesses {

    private static final System.Logger logger = System.getLogger("VectorProcesses");

    private static final Path DATA_DIR = Path.of("/tmp/postgres/vector/data");
    private static final Path CONFIG_PATH = Path.of("/tmp/postgres/vector/vector.yaml");

    private static final Pattern OTLP_BLOCK = Pattern.compile("(?s)\\s*#\\s*BEGIN otlp_remote\\b.*?#\\s*END otlp_remote\\b[^\\n]*\\n?");

    private static final long STOP_TIMEOUT_SECONDS = 15;
    private static final long BACKOFF_INITIAL_MS = 1_000;
    private static final long BACKOFF_CAP_MS = 60_000;
    private static final long FAILURE_WINDOW_MS = 5 * 60_000;
    private static final int FAILURE_THRESHOLD = 10;

    private final UUID instanceUuid;
    private final boolean isPatroni;

    private volatile Process process;
    private volatile Thread supervisor;
    private volatile boolean stopping;

    public VectorProcesses(UUID instanceUuid) {
        this.instanceUuid = instanceUuid;
        this.isPatroni = System.getenv("PATRONI_NAME") != null;
    }

    public void start() {
        if (supervisor != null) {
            logger.log(System.Logger.Level.WARNING, "vector-agent already started");
            return;
        }
        try {
            prepareDataDir();
            renderConfig();
        } catch (IOException e) {
            logger.log(System.Logger.Level.ERROR, "Failed to prepare vector-agent: {0}", e.getMessage());
            return;
        }
        stopping = false;
        supervisor = new Thread(this::supervise, "vector-agent-supervisor");
        supervisor.setDaemon(true);
        supervisor.start();
    }

    public void stop() {
        stopping = true;
        Process p = process;
        if (p != null && p.isAlive()) {
            p.destroy();
            try {
                if (!p.waitFor(STOP_TIMEOUT_SECONDS, SECONDS)) {
                    logger.log(System.Logger.Level.WARNING, "vector-agent did not exit in {0}s; forcing", STOP_TIMEOUT_SECONDS);
                    p.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                p.destroyForcibly();
            }
        }
        Thread s = supervisor;
        if (s != null) {
            try {
                s.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void prepareDataDir() throws IOException {
        Files.createDirectories(DATA_DIR);
    }

    private void renderConfig() throws IOException {
        String yaml = isPatroni ? VectorPipelineTemplates.PATRONI : VectorPipelineTemplates.STANDALONE;
        String otlpEndpoint = SlonSystem.getOtlpEndpoint();
        if (otlpEndpoint == null || otlpEndpoint.isBlank()) {
            yaml = OTLP_BLOCK.matcher(yaml).replaceAll("\n");
            logger.log(System.Logger.Level.INFO, "OTLP_ENDPOINT unset; rendering vector.yaml without remote sink");
        } else {
            logger.log(System.Logger.Level.INFO, "OTLP_ENDPOINT set; rendering vector.yaml with remote sink -> {0}", otlpEndpoint);
        }
        Files.writeString(CONFIG_PATH, yaml);
    }

    private void supervise() {
        long backoff = BACKOFF_INITIAL_MS;
        long windowStart = System.currentTimeMillis();
        int failuresInWindow = 0;
        while (!stopping) {
            int exitCode;
            try {
                Process p = launch();
                process = p;
                exitCode = p.waitFor();
            } catch (IOException e) {
                logger.log(System.Logger.Level.ERROR, "Failed to launch vector-agent: {0}", e.getMessage());
                exitCode = -1;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (stopping) return;
            logger.log(System.Logger.Level.WARNING, "vector-agent exited with code {0}; restarting in {1}ms", exitCode, backoff);

            long now = System.currentTimeMillis();
            if (now - windowStart > FAILURE_WINDOW_MS) {
                windowStart = now;
                failuresInWindow = 0;
            }
            failuresInWindow++;
            if (failuresInWindow > FAILURE_THRESHOLD) {
                logger.log(System.Logger.Level.ERROR,
                        "vector-agent failed {0} times within {1}ms; giving up. Postgres continues; logs are not being shipped.",
                        failuresInWindow, FAILURE_WINDOW_MS);
                return;
            }
            try {
                Thread.sleep(backoff);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            backoff = Math.min(backoff * 2, BACKOFF_CAP_MS);
        }
    }

    private Process launch() throws IOException {
        ProcessBuilder pb = new ProcessBuilder("vector", "--config", CONFIG_PATH.toString());
        pb.environment().put("INSTANCE_UUID", instanceUuid.toString());
        String endpoint = SlonSystem.getOtlpEndpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            pb.environment().put("OTLP_ENDPOINT", endpoint);
        }
        String auth = SlonSystem.getOtlpAuthHeader();
        if (auth != null && !auth.isBlank()) {
            pb.environment().put("OTLP_AUTH_HEADER", auth);
        }
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(new File("/tmp/vector.log")));
        return pb.start();
    }

}