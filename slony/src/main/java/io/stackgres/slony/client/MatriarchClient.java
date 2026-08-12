package io.stackgres.slony.client;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.stackgres.postgres.*;
import io.stackgres.postgres.Extension;
import io.stackgres.postgres.VolumeMount;
import io.stackgres.proto.slony.*;
import io.stackgres.slony.Config;
import io.stackgres.slony.Main;
import io.stackgres.slony.Networks;
import io.stackgres.slony.cri.CreationEventPublisher;
import io.stackgres.slony.cri.Cri;
import io.stackgres.slony.cri.ExceptionMappers;
import io.stackgres.slony.cri.InstanceCreationException;
import io.grpc.*;
import io.grpc.stub.StreamObserver;
import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import static io.stackgres.slony.client.Mappers.buildRegistration;
import static io.stackgres.slony.client.Mappers.mapUUID;

public class MatriarchClient {

    private static final long LOOP_INTERVAL_SECONDS = 5;
    private static final System.Logger logger = System.getLogger("MatriarchClient");

    private final Cri cri;
    private final String matriarchUrl;
    private final boolean matriarchTls;
    private final UUID id;

    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private ManagedChannel channel;
    private SlonyServiceGrpc.SlonyServiceStub client;
    private StreamObserver<SlonyMessage> slonyStream;
    private final Lock slonyStreamLock = new ReentrantLock();
    private final Map<UUID, Tailer> logTailers = new ConcurrentHashMap<>();
    private final ExecutorService logExecutorService = Executors.newCachedThreadPool();
    private Thread mainThread;
    private boolean slonyStreamClosed;

    public MatriarchClient() {
        this.matriarchUrl = Config.getValue("STACKGRES_MATRIARCH_URL", "localhost:50051");
        this.matriarchTls = detectMatriarchTls();
        this.cri = new Cri();
        this.id = UUID.fromString(Config.getValue("STACKGRES_SLONY_ID", UUID.randomUUID().toString()));
    }

    private boolean detectMatriarchTls() {
        String configTls = Config.getValue("STACKGRES_MATRIARCH_TLS", null);
        if (configTls == null) {
            // assume TLS unless localhost
            return !matriarchUrl.startsWith("localhost:");
        }
        return Boolean.parseBoolean(configTls);
    }

    public void connectToMatriarch() {
        this.mainThread = Thread.currentThread();
        connect();

        while (connectionState != ConnectionState.SHUTDOWN) {
            try {
                Thread.sleep(LOOP_INTERVAL_SECONDS * 1000);

                switch (connectionState) {
                    case CONNECTED:
                        sendHeartbeat();
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
                registerSlony();
                logger.log(System.Logger.Level.INFO, "Successfully connected and registered at {0}", matriarchUrl);
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Failed to connect: {0}", e.getMessage());
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
        client = SlonyServiceGrpc.newStub(channel).withCallCredentials(credential);

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

    private void registerSlony() {
        List<io.stackgres.proto.slony.ClusterInstance> instances = cri.listExistingClusters().stream()
                .flatMap(Mappers::mapSlonyClusterInstance)
                .toList();
        StreamObserver<MatriarchMessage> matriarchStream = new MatriarchMessageObserver();
        SlonyMessage registration = SlonyMessage.newBuilder()
                .setRegistration(buildRegistration(instances, id))
                .build();
        slonyStream = client.transfer(matriarchStream);
        sendMessage(registration);
    }

    private void sendHeartbeat() {
        SlonyMessage heartbeat = SlonyMessage.newBuilder()
                .setHeartbeat(Heartbeat.newBuilder().build())
                .build();
        sendMessage(heartbeat);
    }

    private void handleConnectionError() {
        if (connectionState == ConnectionState.CONNECTED) {
            connectionState = ConnectionState.DISCONNECTED;
            logger.log(System.Logger.Level.WARNING, "Connection lost");
        }
    }

    private void sendAckUnusedPort(int port, UUID actionId) {
        sendMessage(SlonyMessage.newBuilder()
                .setUnusedPort(UnusedPort.newBuilder().setId(mapUUID(actionId)).setPort(port).build())
                .build());
    }

    private void sendErrorUnusedPort(Exception e, UUID actionId) {
        String errorMessage = ExceptionMappers.getRootCauseMessage(e);
        sendMessage(SlonyMessage.newBuilder()
                .setUnusedPort(UnusedPort.newBuilder()
                        .setId(mapUUID(actionId))
                        .setStatus(Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(errorMessage).build())
                        .build())
                .build());
    }

    private void sendAckInstanceCreated(UUID id, String imageName, String imageDigest) {
        sendMessage(SlonyMessage.newBuilder()
                .setClusterInstanceCreated(ClusterInstanceCreated.newBuilder()
                        .setId(mapUUID(id))
                        .setImageName(imageName)
                        .setImageDigest(imageDigest)
                        .build())
                .build());
    }

    private void sendErrorInstanceCreated(Exception e, UUID id) {
        String message = ExceptionMappers.getRootCauseMessage(e);
        sendMessage(SlonyMessage.newBuilder()
                .setClusterInstanceCreated(ClusterInstanceCreated.newBuilder()
                        .setId(mapUUID(id))
                        .setStatus(Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(message).build()))
                .build());
    }

    private void sendAckInstanceDeleted(UUID id) {
        sendMessage(SlonyMessage.newBuilder()
                .setClusterInstanceDeleted(ClusterInstanceDeleted.newBuilder()
                        .setId(mapUUID(id))
                        .build())
                .build());
    }

    private void sendErrorInstanceDeleted(Exception e, UUID id) {
        int code = Code.INTERNAL_VALUE;
        if (e instanceof NoSuchElementException) {
            code = Code.NOT_FOUND_VALUE;
        }

        String message = ExceptionMappers.getRootCauseMessage(e);
        sendMessage(SlonyMessage.newBuilder()
                .setClusterInstanceDeleted(ClusterInstanceDeleted.newBuilder()
                        .setId(mapUUID(id))
                        .setStatus(Status.newBuilder().setCode(code).setMessage(message).build()))
                .build());
    }

    private void sendSlonyLog(UUID logsId, String contents) {
        sendMessage(SlonyMessage.newBuilder()
                .setLog(Log.newBuilder()
                        .setId(mapUUID(logsId))
                        .setContents(contents)
                        .build())
                .build());
    }

    private void sendSlonyLogAck(UUID logsId) {
        sendMessage(SlonyMessage.newBuilder()
                .setLog(Log.newBuilder()
                        .setId(mapUUID(logsId))
                        .setStatus(Status.newBuilder().setCode(Code.OK_VALUE).build())
                        .build())
                .build());
    }

    private void sendSlonyLogError(UUID logsId, String error) {
        sendMessage(SlonyMessage.newBuilder()
                .setLog(Log.newBuilder()
                        .setId(mapUUID(logsId))
                        .setStatus(Status.newBuilder().setCode(Code.INTERNAL_VALUE).setMessage(error).build())
                        .build())
                .build());
    }

    private void sendMessage(SlonyMessage message) {
        if (connectionState != ConnectionState.CONNECTED || slonyStream == null) {
            logger.log(System.Logger.Level.WARNING, "Cannot send message (not connected)");
            return;
        }

        try {
            slonyStreamLock.lock();
            slonyStream.onNext(message);
        } finally {
            slonyStreamLock.unlock();
        }
    }

    private class MatriarchMessageObserver implements StreamObserver<MatriarchMessage> {

        @Override
        public void onNext(MatriarchMessage matriarchMessage) {
            if (matriarchMessage.hasReportUnusedPortCommand()) {
                handleReportUnusedPort(matriarchMessage.getReportUnusedPortCommand());
            } else if (matriarchMessage.hasCreateClusterCommand()) {
                handleCreateCluster(matriarchMessage.getCreateClusterCommand());
            } else if (matriarchMessage.hasDeleteInstanceCommand()) {
                handleDeleteInstance(matriarchMessage.getDeleteInstanceCommand());
            } else if (matriarchMessage.hasGetSlonyLogsCommand()) {
                UUID logsId = mapUUID(matriarchMessage.getGetSlonyLogsCommand().getId());
                handleGetSlonyLogsCommand(logsId, matriarchMessage.getGetSlonyLogsCommand().getFollow());
            } else if (matriarchMessage.hasAbortSlonyLogsCommand()) {
                UUID logsId = mapUUID(matriarchMessage.getAbortSlonyLogsCommand().getId());
                handleAbortSlonyLogsCommand(logsId);
            } else if (matriarchMessage.hasShutdownCommand()) {
                handleShutdownCommand();
            } else
                logger.log(System.Logger.Level.INFO, matriarchMessage.toString().replace('\n', ' '));
        }

        private void handleShutdownCommand() {
            logger.log(System.Logger.Level.INFO, "Received shutdown command from matriarch");
            completeSlonyStream();
            initiateShutdown();
        }

        private void handleReportUnusedPort(ReportUnusedPortCommand reportUnusedPortCommand) {
            logger.log(System.Logger.Level.INFO, "Received reportUnusedPortCommand({0})", reportUnusedPortCommand.toString().replace('\n', ' '));
            UUID actionId = mapUUID(reportUnusedPortCommand.getId());
            int desiredPort = reportUnusedPortCommand.getDesiredPort();
            try {
                sendAckUnusedPort(Networks.nextUnusedPort(desiredPort), actionId);
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Error while reporting unused port", e);
                logger.log(System.Logger.Level.ERROR, e);
                sendErrorUnusedPort(e, actionId);
            }
        }

        private void handleCreateCluster(CreateClusterCommand createClusterCommand) {
            logger.log(System.Logger.Level.INFO, "Received createClusterCommand({0})", createClusterCommand.toString().replace('\n', ' '));
            UUID clusterId = mapUUID(createClusterCommand.getId());
            try {
                PostgresCluster cluster = mapCluster(clusterId, createClusterCommand);
                BiConsumer<UUID, Cri.ImageInfo> onInstanceCreated = (id, info) -> sendAckInstanceCreated(id, info.imageName(), info.imageDigest());
                cri.createCluster(cluster, onInstanceCreated, creationEventPublisher);
            } catch (InstanceCreationException e) {
                logger.log(System.Logger.Level.ERROR, "Error while creating cluster instance {0}", e.getInstanceId());
                logger.log(System.Logger.Level.ERROR, e.getCause());
                sendErrorInstanceCreated(e, e.getInstanceId());
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Error while creating cluster", e);
                logger.log(System.Logger.Level.ERROR, e);
                UUID instanceId = mapUUID(createClusterCommand.getInstance(0).getId());
                sendErrorInstanceCreated(e, instanceId);
            }
        }

        private PostgresCluster mapCluster(UUID clusterId, CreateClusterCommand create) {
            List<SlonyLinuxInstance> instances = create.getInstanceList().stream()
                    .map(i -> {
                        UUID id = UUID.fromString(i.getId().getValue().toStringUtf8());
                        String instanceName = i.getName();
                        List<VolumeMount> volumeMounts = i.getVolumeMountList().stream().map(v -> new VolumeMount(v.getHostPath(), v.getMountPath())).toList();
                        Path configPath = Paths.get(i.getConfigPath());
                        Path dataDir = Paths.get(i.getDataDir());
                        Path logDir = Paths.get(i.getLogDir());
                        Path walDir = Paths.get(i.getWalDir());
                        SlonyLinuxInstance instance;
                        if (create.getStandalone()) {
                            instance = new SlonyLinuxInstance(id, instanceName, create.getVersion(), i.getPort(), i.getListenAddress(), io.stackgres.postgres.Status.CREATED,
                                    MatriarchClient.this.id, configPath, dataDir, logDir, walDir, volumeMounts);
                        } else {
                            instance = new SlonyLinuxHAInstance(id, instanceName, create.getVersion(), i.getPort(), i.getListenAddress(), io.stackgres.postgres.Status.CREATED,
                                    MatriarchClient.this.id, configPath, dataDir, logDir, walDir, volumeMounts, i.getEtcdName(), i.getEtcdServerUrl(), i.getEtcdClientUrl());
                        }
                        if (i.hasIvorySqlPort())
                            instance.setIvorySqlPort(i.getIvorySqlPort());
                        return instance;
                    })
                    .toList();
            List<Extension> extensions = create.getExtensionList().stream().map(e -> new Extension(e.getName(), e.getVersion(), e.getRevision())).toList();
            PostgresCluster cluster = new PostgresCluster(clusterId, create.getName(), create.getUsername(), create.getPassword(), extensions, create.getTagsMap(), create.getStandalone(), instances);
            cluster.setFlavor(Mappers.mapFlavor(create.getFlavor()));
            if (!cluster.isStandalone())
                cluster.getEtcdInitialCluster().putAll(create.getEtcdInitialClusterMap());
            return cluster;
        }

        private void handleDeleteInstance(DeleteInstanceCommand deleteInstanceCommand) {
            logger.log(System.Logger.Level.INFO, "Received deleteInstanceCommand({0})", deleteInstanceCommand.toString().replace('\n', ' '));
            UUID instanceId = UUID.fromString(deleteInstanceCommand.getId().getValue().toStringUtf8());
            try {
                SlonyLinuxInstance instance = cri.deleteInstance(instanceId);
                Networks.freeReservedPort(instance.getPort());
                if (instance.getIvorySqlPort() != null)
                    Networks.freeReservedPort(instance.getIvorySqlPort());
                if (instance instanceof SlonyLinuxHAInstance haInstance) {
                    if (haInstance.getEtcdClientPort() != null)
                        Networks.freeReservedPort(haInstance.getEtcdClientPort());
                    if (haInstance.getEtcdServerPort() != null)
                        Networks.freeReservedPort(haInstance.getEtcdServerPort());
                }
                sendAckInstanceDeleted(instanceId);
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Error while deleting instance", e);
                logger.log(System.Logger.Level.ERROR, e);
                sendErrorInstanceDeleted(e, instanceId);
            }
        }

        private void handleGetSlonyLogsCommand(UUID logsId, boolean follow) {
            Path logPath = Paths.get(Main.getSlonyLogPath());
            try {
                if (follow) {
                    Tailer tailer = Tailer.builder()
                            .setTailerListener(new TailerListenerAdapter() {
                                @Override
                                public void handle(String line) {
                                    sendSlonyLog(logsId, line);
                                }
                            })
                            .setFile(logPath.toString())
                            .setExecutorService(logExecutorService)
                            .get();
                    logTailers.put(logsId, tailer);
                } else {
                    String logs = Files.readString(logPath);
                    sendSlonyLog(logsId, logs);
                    sendSlonyLogAck(logsId);
                }
            } catch (IOException e) {
                logger.log(System.Logger.Level.ERROR, "Failed to read log file {0}: {1}", logPath, e.getMessage());
                sendSlonyLogError(logsId, "Failed to read log file " + logPath + ": " + e.getMessage());
            } catch (Exception e) {
                logger.log(System.Logger.Level.ERROR, "Error while handling slony logs", e);
                sendSlonyLogError(logsId, e.getMessage());
            }
        }

        private void handleAbortSlonyLogsCommand(UUID logsId) {
            Tailer tailer = logTailers.remove(logsId);
            if (tailer != null) {
                tailer.close();
                sendSlonyLogAck(logsId);
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
            logger.log(System.Logger.Level.INFO, "Matriarch connection completed");
            if (connectionState == ConnectionState.CONNECTED) {
                connectionState = ConnectionState.DISCONNECTED;
            }
        }

    }

    private void sendClusterEvent(String type, UUID clusterId) {
        sendMessage(SlonyMessage.newBuilder()
                .setEvent(io.stackgres.proto.slony.Event.newBuilder()
                        .setType(type)
                        .setClusterId(mapUUID(clusterId))
                        .build())
                .build());
    }

    private void sendInstanceEvent(String type, UUID instanceId, Map<String, String> data) {
        sendMessage(SlonyMessage.newBuilder()
                .setEvent(io.stackgres.proto.slony.Event.newBuilder()
                        .setType(type)
                        .setInstanceId(mapUUID(instanceId))
                        .putAllData(data)
                        .build())
                .build());
    }

    public void close() {
        connectionState = ConnectionState.SHUTDOWN;

        completeSlonyStream();

        if (channel != null && !channel.isShutdown()) {
            channel.shutdown();
            try {
                if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                    channel.shutdownNow();
                }
            } catch (InterruptedException e) {
                channel.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        cri.close();
    }

    public void initiateShutdown() {
        connectionState = ConnectionState.SHUTDOWN;
        if (mainThread != null)
            mainThread.interrupt();
    }

    private void completeSlonyStream() {
        if (slonyStreamClosed || slonyStream == null) return;
        try {
            slonyStreamLock.lock();
            if (slonyStreamClosed) return;
            slonyStream.onCompleted();
            slonyStreamClosed = true;
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Error completing stream: {0}", e.getMessage());
        } finally {
            slonyStreamLock.unlock();
        }
    }

    public enum ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, SHUTDOWN
    }

    private final CreationEventPublisher creationEventPublisher = new CreationEventPublisher() {

        @Override
        public void imagePulled(UUID instanceId, String imageRef, String imageDigest) {
            sendInstanceEvent("INSTANCE_IMAGE_PULLED", instanceId, Map.of("imageRef", imageRef, "imageDigest", imageDigest));
        }

        @Override
        public void imageCached(UUID instanceId, String imageRef, String imageDigest) {
            sendInstanceEvent("INSTANCE_IMAGE_CACHED", instanceId, Map.of("imageRef", imageRef, "imageDigest", imageDigest));
        }

        @Override
        public void etcdImagePulled(UUID instanceId, String imageRef) {
            sendInstanceEvent("ETCD_IMAGE_PULLED", instanceId, Map.of("imageRef", imageRef));
        }

        @Override
        public void etcdContainerStarted(UUID instanceId) {
            sendInstanceEvent("ETCD_CONTAINER_STARTED", instanceId, Map.of());
        }

        @Override
        public void etcdHealthy(UUID clusterId) {
            sendClusterEvent("ETCD_HEALTHY", clusterId);
        }

        @Override
        public void etcdStartupTimeout(UUID clusterId) {
            sendClusterEvent("ETCD_STARTUP_TIMEOUT", clusterId);
        }
    };

}