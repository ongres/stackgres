package io.stackgres.slon;

import com.google.protobuf.ByteString;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.stackgres.proto.slon.*;
import io.stackgres.slon.pgwire.PgWireTunnel;
import io.stackgres.slon.processes.PatroniProcesses;
import io.stackgres.slon.processes.PostgresProcesses;
import io.stackgres.slon.processes.Processes;
import io.stackgres.slon.processes.VectorProcesses;
import common.Common;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class MatriarchClient {

    private static final long LOOP_INTERVAL_SECONDS = 5;
    private static final long DIAGNOSTICS_INTERVAL_SECONDS = 300;
    private static final System.Logger logger = System.getLogger("MatriarchClient");

    private final Processes postgresProcesses;
    private final String matriarchUrl;
    private final boolean matriarchTls;
    private final String port;
    private final Map<UUID, PgWireTunnel> pgWireTunnels = new ConcurrentHashMap<>();
    private final Map<UUID, SubmissionPublisher<byte[]>> execPublishers = new ConcurrentHashMap<>();
    private final Map<UUID, Tailer> logTailers = new ConcurrentHashMap<>();
    private final Lock slonStreamLock = new ReentrantLock();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private volatile Instant lastDiagnosticsSentAt;
    private volatile Boolean lastPatroniHealthy = null;
    private volatile ReplicationStatus lastReplicationStatus = null;
    private volatile Thread mainThread;
    private volatile boolean stopping = false;
    private volatile boolean slonStreamClosed;
    private VectorProcesses vectorProcesses;
    private SlonServiceGrpc.SlonServiceStub client;
    private ManagedChannel channel;
    private StreamObserver<SlonMessage> slonStream;
    private UUID instanceId;
    private SlonStatus currentStatus;
    private SlonStatus statusToResend;

    public MatriarchClient(String port, SlonStatus initialStatus) {
        this.port = port;
        this.currentStatus = initialStatus;
        this.statusToResend = initialStatus;
        matriarchUrl = Config.getValue("STACKGRES_ENDPOINT_URL", "localhost:50051");
        matriarchTls = detectMatriarchTls();
        String patroniName = Config.getValue("PATRONI_NAME", null);
        postgresProcesses = (patroniName != null) ? new PatroniProcesses() : new PostgresProcesses();
    }

    private boolean detectMatriarchTls() {
        String configTls = Config.getValue("STACKGRES_ENDPOINT_TLS", null);
        if (configTls == null) {
            // assume TLS unless localhost
            return !matriarchUrl.startsWith("localhost:");
        }
        return Boolean.parseBoolean(configTls);
    }

    public void connectToMatriarch(UUID instanceId) {
        this.instanceId = instanceId;
        this.mainThread = Thread.currentThread();
        connect();

        while (connectionState != ConnectionState.SHUTDOWN) {
            try {
                Thread.sleep(LOOP_INTERVAL_SECONDS * 1000);

                switch (connectionState) {
                    case CONNECTED:
                        if (statusToResend != null)
                            updateStatus(statusToResend);
                        else
                            healthcheck();
                        break;

                    case DISCONNECTED:
                        connect();
                        break;

                    case CONNECTING:
                        logger.log(System.Logger.Level.DEBUG, "Connection in progress...");
                        break;

                    case SHUTDOWN:
                        break;
                }

            } catch (InterruptedException e) {
                logger.log(System.Logger.Level.INFO, "Interrupted: {0}", e.getMessage());
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void connect() {
        if (connectionState == ConnectionState.DISCONNECTED) {
            connectionState = ConnectionState.CONNECTING;
            try {
                logger.log(System.Logger.Level.INFO, "Connecting to {0}", matriarchUrl);
                initChannel();
                connectionState = ConnectionState.CONNECTED;
                lastDiagnosticsSentAt = null;
                lastPatroniHealthy = null;
                registerSlon();
                logger.log(System.Logger.Level.INFO, "Successfully connected and registered at {0}", matriarchUrl);
                updateStatus(currentStatus);
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to connect: {0}", e.getMessage());
                e.printStackTrace();
                logger.log(System.Logger.Level.INFO, "Will attempt to reconnect in {0} seconds", LOOP_INTERVAL_SECONDS);
                connectionState = ConnectionState.DISCONNECTED;
            }
        }
    }

    private void initChannel() throws InterruptedException {
        if (channel != null && !channel.isShutdown())
            channel.shutdownNow();

        ChannelCredentials channelCredentials = matriarchTls ? TlsChannelCredentials.create() : InsecureChannelCredentials.create();
        channel = Grpc.newChannelBuilder(matriarchUrl, channelCredentials).build();
        String token = Config.getValue("STACKGRES_TOKEN", null);
        JwtCredential credential = token != null ? new JwtCredential(token) : null;
        client = SlonServiceGrpc.newStub(channel).withCallCredentials(credential);

        awaitChannelReady();
    }

    private void awaitChannelReady() throws InterruptedException {
        ConnectivityState state = channel.getState(true);

        int countdown = 5;
        while (state != ConnectivityState.READY && countdown > 0) {
            Thread.sleep(1_000);
            state = channel.getState(true);
            countdown--;
        }
        if (state != ConnectivityState.READY)
            throw new IllegalStateException("Channel is not ready: " + state);
    }

    private void registerSlon() {
        StreamObserver<MatriarchMessage> matriarchStream = new MatriarchMessageObserver();
        SlonMessage registration = SlonMessage.newBuilder()
                .setRegistration(Registration.newBuilder()
                        .setId(mapUUID(instanceId))
                        .setClusterId(mapUUID(UUID.fromString(SlonSystem.getClusterId())))
                        .setClusterName(SlonSystem.getClusterName())
                        .setPort(Integer.parseInt(port))
                        .setInstanceName(SlonSystem.getInstanceName())
                        .setOs(SlonSystem.getOs())
                        .setArch(SlonSystem.getArch())
                        .setVersion(SlonSystem.getVersion())
                        .setCpu(SlonSystem.getNumberOfCpus())
                        .setMemory(SlonSystem.getMemoryBytes())
                        .setUsername(SlonSystem.getPgUsername())
                        .setPassword(SlonSystem.getPgPassword())
                        .setListenAddress(SlonSystem.getPgListenAddress())
                        .build())
                .build();
        slonStream = client.transfer(matriarchStream);
        slonStream.onNext(registration);
        logger.log(System.Logger.Level.INFO, "Successfully registered at {0}", client.getChannel().authority());
    }

    private void healthcheck() {
        boolean healthy = postgresProcesses.healthcheck(port);
        if (healthy) {
            if (currentStatus == SlonStatus.STATUS_STARTED || currentStatus == SlonStatus.STATUS_FAILED)
                updateStatus(SlonStatus.STATUS_HEALTHY);
            sendDiagnostics();
        } else {
            if (currentStatus == SlonStatus.STATUS_HEALTHY && !stopping)
                updateStatus(SlonStatus.STATUS_FAILED);
        }
        sendHeartbeat();

        if (postgresProcesses instanceof PatroniProcesses patroni) {
            if (lastPatroniHealthy == null || lastPatroniHealthy != healthy) {
                sendEvent(healthy ? "PATRONI_HEALTHY" : "PATRONI_UNHEALTHY");
                lastPatroniHealthy = healthy;
            }
            ReplicationStatus replicationStatus = patroni.replicationStatus();
            if (lastReplicationStatus == null || lastReplicationStatus != replicationStatus) {
                sendReplicationStatusUpdate(replicationStatus);
                lastReplicationStatus = replicationStatus;
            }
        }
    }

    private void sendReplicationStatusUpdate(ReplicationStatus replicationStatus) {
        SlonMessage message = SlonMessage.newBuilder()
                .setReplicationStatusUpdate(ReplicationStatusUpdate.newBuilder().setReplicationStatus(replicationStatus).build())
                .build();
        sendMessage(message);
    }

    private void sendHeartbeat() {
        SlonMessage heartbeat = SlonMessage.newBuilder().setHeartbeat(Heartbeat.newBuilder().build()).build();
        sendMessage(heartbeat);
    }

    public void updateStatus(SlonStatus slonStatus) {
        currentStatus = slonStatus;
        if (connectionState != ConnectionState.CONNECTED) {
            logger.log(System.Logger.Level.WARNING, "Cannot send status update (not connected), marking status for resend");
            statusToResend = slonStatus;
            return;
        }

        SlonMessage update = SlonMessage.newBuilder().setStatusUpdate(StatusUpdate.newBuilder().setSlonStatus(slonStatus).build()).build();
        sendMessage(update);
        statusToResend = null;
    }

    public void sendFailure(String message) {
        if (connectionState != ConnectionState.CONNECTED) {
            logger.log(System.Logger.Level.WARNING, "Cannot send failure (not connected)");
            statusToResend = SlonStatus.STATUS_FAILED;
            return;
        }

        SlonMessage slonMessage = SlonMessage.newBuilder().setStatusUpdate(StatusUpdate.newBuilder()
                .setStatus(Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(message))
                .setSlonStatus(SlonStatus.STATUS_FAILED)
                .build()).build();
        sendMessage(slonMessage);
        statusToResend = null;
    }

    private void handleConnectionError() {
        if (connectionState == ConnectionState.CONNECTED) {
            connectionState = ConnectionState.DISCONNECTED;
            logger.log(System.Logger.Level.WARNING, "Connection lost");
        }
    }

    private class MatriarchMessageObserver implements StreamObserver<MatriarchMessage> {

        @Override
        public void onNext(MatriarchMessage matriarchMessage) {
            // Log the message KIND, not the full protobuf .toString(): in the native image TextFormat's
            // debug-redaction check reflects on FieldOptions (getCtype), which isn't registered and throws
            // — that would abort onNext and CANCEL the stream. getKindCase() is reflection-free and safe.
            logger.log(System.Logger.Level.INFO, "Received matriarch message: {0}", matriarchMessage.getKindCase());
            try {
                if (matriarchMessage.hasInitDbCommand())
                    handleInitDbCommand(matriarchMessage.getInitDbCommand());
                if (matriarchMessage.hasStartDbCommand())
                    handleStartDbCommand();
                if (matriarchMessage.hasStopDbCommand())
                    handleStopDbCommand();
                if (matriarchMessage.hasShutdownCommand())
                    handleShutdownCommand();
                if (matriarchMessage.hasOpenPgWireTunnelCommand()) {
                    var openCmd = matriarchMessage.getOpenPgWireTunnelCommand();
                    UUID tunnelId = mapUUID(openCmd.getTunnelId());
                    int targetPort = openCmd.hasTargetPort() ? openCmd.getTargetPort() : Integer.parseInt(port);
                    handleOpenPgWireTunnel(tunnelId, targetPort);
                }
                if (matriarchMessage.hasPgWireTunnelData()) {
                    UUID tunnelId = mapUUID(matriarchMessage.getPgWireTunnelData().getTunnelId());
                    byte[] data = matriarchMessage.getPgWireTunnelData().getData().toByteArray();
                    handlePgWireTunnelData(tunnelId, data);
                }
                if (matriarchMessage.hasClosePgWireTunnelCommand()) {
                    UUID tunnelId = mapUUID(matriarchMessage.getClosePgWireTunnelCommand().getTunnelId());
                    handleClosePgWireTunnel(tunnelId);
                }
                if (matriarchMessage.hasExecCommand()) {
                    UUID execId = UUID.fromString(matriarchMessage.getExecCommand().getId().getValue().toStringUtf8());
                    List<String> commands = matriarchMessage.getExecCommand().getCommandList().stream().toList();
                    handleExecCommand(execId, commands);
                }
                if (matriarchMessage.hasExecMessage()) {
                    UUID execId = UUID.fromString(matriarchMessage.getExecMessage().getId().getValue().toStringUtf8());
                    byte[] bytes = matriarchMessage.getExecMessage().getBytes().toByteArray();
                    handleExecMessage(execId, bytes);
                }
                if (matriarchMessage.hasGetPostgresLogsCommand())
                    handleComponentLogs(mapUUID(matriarchMessage.getGetPostgresLogsCommand().getId()),
                            matriarchMessage.getGetPostgresLogsCommand().getFollow(), LogComponent.POSTGRES);
                if (matriarchMessage.hasGetPatroniLogsCommand())
                    handleComponentLogs(mapUUID(matriarchMessage.getGetPatroniLogsCommand().getId()),
                            matriarchMessage.getGetPatroniLogsCommand().getFollow(), LogComponent.PATRONI);
                if (matriarchMessage.hasGetSlonLogsCommand())
                    handleComponentLogs(mapUUID(matriarchMessage.getGetSlonLogsCommand().getId()),
                            matriarchMessage.getGetSlonLogsCommand().getFollow(), LogComponent.SLON);
                if (matriarchMessage.hasGetEtcdLogsCommand())
                    handleComponentLogs(mapUUID(matriarchMessage.getGetEtcdLogsCommand().getId()),
                            matriarchMessage.getGetEtcdLogsCommand().getFollow(), LogComponent.ETCD);
                if (matriarchMessage.hasAbortLogsCommand())
                    handleAbortLogsCommand(mapUUID(matriarchMessage.getAbortLogsCommand().getId()));
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Error while handling command", e);
                e.printStackTrace();
                sendFailure("Error while handling command: " + e.getMessage());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            logger.log(System.Logger.Level.ERROR, "Connection error received: " + throwable.getMessage(), throwable);
            throwable.printStackTrace();
            handleConnectionError();
        }

        @Override
        public void onCompleted() {
            logger.log(System.Logger.Level.INFO, "Matriarch connection finished");
            if (connectionState == ConnectionState.CONNECTED) {
                connectionState = ConnectionState.DISCONNECTED;
            }
        }
    }

    private void handleInitDbCommand(InitDbCommand command) {
        logger.log(System.Logger.Level.INFO, "Initializing DB...");
        postgresProcesses.initDb();
        postgresProcesses.configureTls(command.getTls());
        updateStatus(SlonStatus.STATUS_INITDB);
    }

    private void handleStartDbCommand() {
        logger.log(System.Logger.Level.INFO, "Starting Postgres server...");
        postgresProcesses.startPostgres(port);
        if (postgresProcesses instanceof PatroniProcesses)
            sendEvent("PATRONI_STARTED");
        startVectorAgentFirstTime();
        if (postgresProcesses.healthcheck(port))
            updateStatus(SlonStatus.STATUS_HEALTHY);
        else
            updateStatus(SlonStatus.STATUS_STARTED);
    }

    private void handleStopDbCommand() {
        logger.log(System.Logger.Level.INFO, "Stopping Postgres server...");
        stopping = true;
        try {
            postgresProcesses.stopPostgres();
            updateStatus(SlonStatus.STATUS_STOPPED);
        } finally {
            stopping = false;
        }
    }

    private void handleShutdownCommand() {
        logger.log(System.Logger.Level.INFO, "Received shutdown command from matriarch");
        completeSlonStream();
        initiateShutdown();
    }

    private void startVectorAgentFirstTime() {
        if (vectorProcesses != null) return;
        logger.log(System.Logger.Level.INFO, "Starting vector-agent...");
        vectorProcesses = new VectorProcesses(instanceId);
        vectorProcesses.start();
    }

    private void stopVectorAgent() {
        if (vectorProcesses == null) return;
        logger.log(System.Logger.Level.INFO, "Stopping vector-agent...");
        vectorProcesses.stop();
    }

    private void handleOpenPgWireTunnel(UUID tunnelId, int targetPort) {
        logger.log(System.Logger.Level.INFO, "Opening PgWire tunnel {0} to port {1}", tunnelId, targetPort);
        try {
            PgWireTunnel tunnel = new PgWireTunnel(tunnelId,
                    targetPort,
                    b -> sendPgWireTunnelData(tunnelId, b),
                    () -> closeTunnel(tunnelId, Code.CANCELLED_VALUE, null));
            pgWireTunnels.put(tunnelId, tunnel);
            executorService.submit(tunnel::readFromPostgres);
            sendPgWireTunnelOpened(tunnelId);
            logger.log(System.Logger.Level.INFO, "PgWire tunnel {0} opened", tunnelId);
        } catch (IOException e) {
            logger.log(System.Logger.Level.ERROR, "Failed to open PgWire tunnel {0}: {1}", tunnelId, e.getMessage());
            sendPgWireTunnelClosed(tunnelId, Code.INTERNAL_VALUE, e.getMessage());
        }
    }

    private void handlePgWireTunnelData(UUID tunnelId, byte[] data) {
        PgWireTunnel tunnel = pgWireTunnels.get(tunnelId);
        if (tunnel == null) {
            logger.log(System.Logger.Level.WARNING, "Received data for unknown tunnel {0}", tunnelId);
            return;
        }
        try {
            tunnel.writeToPostgres(data);
        } catch (IOException e) {
            logger.log(System.Logger.Level.ERROR, "Failed to write to tunnel {0}: {1}", tunnelId, e.getMessage());
            closeTunnel(tunnelId, Code.INTERNAL_VALUE, e.getMessage());
        }
    }

    private void handleClosePgWireTunnel(UUID tunnelId) {
        logger.log(System.Logger.Level.INFO, "Closing PgWire tunnel {0}", tunnelId);
        closeTunnel(tunnelId, Code.OK_VALUE, null);
    }

    private void closeTunnel(UUID tunnelId, int code, String message) {
        PgWireTunnel tunnel = pgWireTunnels.remove(tunnelId);
        if (tunnel != null) {
            tunnel.close();
            sendPgWireTunnelClosed(tunnelId, code, message);
            logger.log(System.Logger.Level.INFO, "PgWire tunnel {0} closed", tunnelId);
        }
    }

    private void sendPgWireTunnelOpened(UUID tunnelId) {
        SlonMessage message = SlonMessage.newBuilder()
                .setPgWireTunnelOpened(PgWireTunnelOpened.newBuilder().setTunnelId(mapUUID(tunnelId)).build())
                .build();
        slonStream.onNext(message);
    }

    private void sendPgWireTunnelData(UUID tunnelId, byte[] data) {
        SlonMessage message = SlonMessage.newBuilder()
                .setPgWireTunnelData(PgWireTunnelData.newBuilder()
                        .setTunnelId(mapUUID(tunnelId))
                        .setData(ByteString.copyFrom(data))
                        .build())
                .build();
        slonStream.onNext(message);
    }

    private void sendPgWireTunnelClosed(UUID tunnelId, int code, String errorMessage) {
        PgWireTunnelClosed.Builder closedBuilder = PgWireTunnelClosed.newBuilder().setTunnelId(mapUUID(tunnelId));
        if (code != Code.OK_VALUE || errorMessage != null) {
            Status.Builder statusBuilder = Status.newBuilder().setCode(code);
            if (errorMessage != null)
                statusBuilder.setMessage(errorMessage);
            closedBuilder.setStatus(statusBuilder.build());
        }
        SlonMessage message = SlonMessage.newBuilder().setPgWireTunnelClosed(closedBuilder.build()).build();
        slonStream.onNext(message);
    }

    private void sendDiagnostics() {
        Instant now = Instant.now();
        if (lastDiagnosticsSentAt != null && lastDiagnosticsSentAt.plusSeconds(DIAGNOSTICS_INTERVAL_SECONDS).isAfter(now))
            return;
        try {
            String data = postgresProcesses.pgControlData();
            if (data == null || data.isBlank())
                return;
            SlonMessage message = SlonMessage.newBuilder()
                    .setDiagnostics(Diagnostics.newBuilder()
                            .setPgControlData(data)
                            .setDbSize(postgresProcesses.dbSize(port))
                            .build())
                    .build();
            sendMessage(message);
            lastDiagnosticsSentAt = now;
            logger.log(System.Logger.Level.INFO, "Sent diagnostics");
        } catch (Exception e) {
            logger.log(System.Logger.Level.WARNING, "Failed to send diagnostics: {0}", e.getMessage());
        }
    }

    private void sendEvent(String type, Map<String, String> data) {
        sendMessage(SlonMessage.newBuilder()
                .setEvent(Event.newBuilder().setType(type).putAllData(data).build())
                .build());
    }

    private void sendEvent(String type) {
        sendMessage(SlonMessage.newBuilder()
                .setEvent(Event.newBuilder().setType(type).build())
                .build());
    }

    public void initiateShutdown() {
        connectionState = ConnectionState.SHUTDOWN;
        if (mainThread != null)
            mainThread.interrupt();
    }

    private void completeSlonStream() {
        if (slonStreamClosed || slonStream == null) return;
        try {
            slonStreamLock.lock();
            if (slonStreamClosed) return;
            slonStream.onCompleted();
            slonStreamClosed = true;
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Error completing stream: {0}", e.getMessage());
        } finally {
            slonStreamLock.unlock();
        }
    }

    private void handleExecCommand(UUID execId, List<String> commands) {
        logger.log(System.Logger.Level.INFO, "Received exec command: ID {0}, commands: {1}", execId, commands);
        executorService.submit(() -> {
            SubmissionPublisher<byte[]> publisher = new SubmissionPublisher<>();
            execPublishers.put(execId, publisher);
            Consumer<byte[]> consumer = bytes -> sendExecMessage(execId, bytes);
            int exit = postgresProcesses.exec(commands, consumer, publisher);
            sendExecExit(execId, exit);
            publisher.close();
            execPublishers.remove(execId);
        });
    }

    private void sendExecMessage(UUID execId, byte[] bytes) {
        Common.UUID uuid = Common.UUID.newBuilder().setValue(ByteString.copyFromUtf8(execId.toString())).build();
        SlonMessage message = SlonMessage.newBuilder().setExec(ExecMessage.newBuilder()
                .setId(uuid)
                .setBytes(ByteString.copyFrom(bytes))
                .build()).build();
        sendMessage(message);
        logger.log(System.Logger.Level.DEBUG, "Sent exec message for exec {0}, {1} bytes", execId, bytes.length);
    }

    private void sendExecExit(UUID execId, int exitCode) {
        Common.UUID uuid = Common.UUID.newBuilder().setValue(ByteString.copyFromUtf8(execId.toString())).build();
        SlonMessage message = SlonMessage.newBuilder().setExecExit(ExecExit.newBuilder()
                .setId(uuid)
                .setCode(exitCode)
                .build()).build();
        sendMessage(message);
        logger.log(System.Logger.Level.INFO, "Sent exec exit for exec {0}, code {1}", execId, exitCode);
    }

    private void handleExecMessage(UUID execId, byte[] bytes) {
        SubmissionPublisher<byte[]> publisher = execPublishers.get(execId);
        if (publisher == null) {
            logger.log(System.Logger.Level.WARNING, "Received exec message for unknown exec {0}", execId);
            return;
        }
        publisher.submit(bytes);
    }

    // ---- Component log tail (Slon-sourced). Restores the pre-logs-service Slon log path: the
    // matriarch asks this slon for one component's log by id; we stream matching lines back as
    // Log{contents} and finish a snapshot (or stop a follow via AbortLogsCommand) with Log{status}. ----

    private static final int SNAPSHOT_MAX_LINES = 1000;
    private static final int FOLLOW_PRIME_LINES = 100;

    private enum LogComponent {
        POSTGRES, PATRONI, SLON, ETCD
    }

    private void handleComponentLogs(UUID logsId, boolean follow, LogComponent component) {
        Path logPath;
        try {
            logPath = resolveLogFile(component);
        } catch (IOException e) {
            sendLogError(logsId, "Failed to locate " + component + " log: " + e.getMessage());
            return;
        }
        if (logPath == null) {
            sendLogError(logsId, component.name().toLowerCase() + " logs are not available for this instance");
            return;
        }
        try {
            if (follow) {
                // tail -f: emit a recent snapshot first (context), then stream new lines from the end.
                // NOTE: follows a single file — a csvlog rotation to a new filename is not followed yet.
                StringBuilder recent = new StringBuilder();
                for (String line : readLastLines(logPath, FOLLOW_PRIME_LINES)) {
                    recent.append(line).append('\n');
                }
                if (!recent.isEmpty()) {
                    sendLog(logsId, recent.toString());
                }
                Tailer tailer = Tailer.builder()
                        .setTailerListener(new TailerListenerAdapter() {
                            @Override
                            public void handle(String line) {
                                sendLog(logsId, line);   // raw line; the matriarch normalizes per component
                            }
                        })
                        .setFile(logPath.toString())
                        .setTailFromEnd(true)
                        .setExecutorService(executorService)
                        .get();
                logTailers.put(logsId, tailer);
            } else {
                StringBuilder snapshot = new StringBuilder();
                for (String line : readLastLines(logPath, SNAPSHOT_MAX_LINES)) {
                    snapshot.append(line).append('\n');
                }
                sendLog(logsId, snapshot.toString());
                sendLogAck(logsId);
            }
        } catch (IOException e) {
            sendLogError(logsId, "Failed to read log file " + logPath + ": " + e.getMessage());
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Error while handling component logs", e);
            sendLogError(logsId, e.getMessage());
        }
    }

    private void handleAbortLogsCommand(UUID logsId) {
        Tailer tailer = logTailers.remove(logsId);
        if (tailer != null) {
            tailer.close();
            sendLogAck(logsId);
        }
    }

    // Component -> file. Postgres is the structured csvlog (newest of the rotated set); the others are
    // single files. Patroni/etcd exist only in Patroni-mode clusters (null -> "not available").
    private Path resolveLogFile(LogComponent component) throws IOException {
        return switch (component) {
            case POSTGRES -> newestMatch(Paths.get("/tmp/postgres/log"), "postgresql-*.csv");
            case PATRONI -> existing(Paths.get("/tmp/patroni.log"));
            case SLON -> existing(Paths.get("/tmp/slon.log"));
            case ETCD -> existing(Paths.get("/tmp/etcd-logs/etcd.log"));
        };
    }

    private static Path existing(Path path) {
        return Files.exists(path) ? path : null;
    }

    private static Path newestMatch(Path dir, String glob) throws IOException {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        Path newest = null;
        FileTime newestTime = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path candidate : stream) {
                FileTime time = Files.getLastModifiedTime(candidate);
                if (newestTime == null || time.compareTo(newestTime) > 0) {
                    newestTime = time;
                    newest = candidate;
                }
            }
        }
        return newest;
    }

    private static List<String> readLastLines(Path path, int max) throws IOException {
        ArrayDeque<String> tail = new ArrayDeque<>(max);
        try (var lines = Files.lines(path)) {
            lines.forEach(line -> {
                if (tail.size() == max) {
                    tail.removeFirst();
                }
                tail.addLast(line);
            });
        }
        return new ArrayList<>(tail);
    }

    private void sendLog(UUID logsId, String contents) {
        sendMessage(SlonMessage.newBuilder()
                .setLog(Log.newBuilder().setId(mapUUID(logsId)).setContents(contents).build())
                .build());
    }

    private void sendLogAck(UUID logsId) {
        sendMessage(SlonMessage.newBuilder()
                .setLog(Log.newBuilder().setId(mapUUID(logsId))
                        .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build()).build())
                .build());
    }

    private void sendLogError(UUID logsId, String error) {
        sendMessage(SlonMessage.newBuilder()
                .setLog(Log.newBuilder().setId(mapUUID(logsId))
                        .setStatus(Status.newBuilder().setCode(Code.INTERNAL_VALUE)
                                .setMessage(error == null ? "" : error).build()).build())
                .build());
    }

    private void sendMessage(SlonMessage message) {
        try {
            slonStreamLock.lock();
            slonStream.onNext(message);
        } finally {
            slonStreamLock.unlock();
        }
    }

    public void close() {
        connectionState = ConnectionState.SHUTDOWN;

        stopVectorAgent();

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS))
                executorService.shutdownNow();
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        completeSlonStream();

        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(2, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        executorService.shutdown();
    }

    public enum ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, SHUTDOWN
    }

    private static Common.UUID mapUUID(UUID uuid) {
        return Common.UUID.newBuilder().setValue(ByteString.copyFromUtf8(uuid.toString())).build();
    }

    private static UUID mapUUID(Common.UUID uuid) {
        return UUID.fromString(uuid.getValue().toStringUtf8());
    }

}