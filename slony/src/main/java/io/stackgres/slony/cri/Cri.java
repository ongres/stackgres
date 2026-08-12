package io.stackgres.slony.cri;

import io.stackgres.postgres.*;
import io.stackgres.slony.Config;
import io.stackgres.slony.Networks;
import io.stackgres.slony.SlonySystem;
import io.stackgres.slony.client.Mappers;
import io.stackgres.slony.postgres.Directories;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.ChannelOption;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import runtime.v1.Api.*;
import runtime.v1.ImageServiceGrpc;
import runtime.v1.RuntimeServiceGrpc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import static io.stackgres.slony.cri.PodContainerConfigs.*;

public class Cri {

    private static final System.Logger logger = System.getLogger("CRI");

    public static final String LABEL_MANAGED_BY = "managed-by";
    public static final String LABEL_COMPONENT = "component";

    public static final String ANNOTATION_CLUSTER_ID = "stackgres.io/cluster-id";
    public static final String ANNOTATION_NAME = "stackgres.io/name";
    public static final String ANNOTATION_INSTANCE_NAME = "stackgres.io/instance-name";
    public static final String ANNOTATION_PG_VERSION = "stackgres.io/version";
    public static final String ANNOTATION_STANDALONE = "stackgres.io/standalone";
    public static final String ANNOTATION_PORT = "stackgres.io/port";
    public static final String ANNOTATION_USERNAME = "stackgres.io/username";
    public static final String ANNOTATION_PASSWORD = "stackgres.io/password";
    public static final String ANNOTATION_TAGS = "stackgres.io/tags";
    public static final String ANNOTATION_EXTENSIONS = "stackgres.io/extensions";
    public static final String ANNOTATION_LISTEN_ADDRESS = "stackgres.io/listen-address";
    public static final String ANNOTATION_PG_CONFIG_FILE = "stackgres.io/config-file";
    public static final String ANNOTATION_PG_DATA_PATH = "stackgres.io/pgdata-path";
    public static final String ANNOTATION_LOG_DIR = "stackgres.io/logs-dir";
    public static final String ANNOTATION_WAL_DIR = "stackgres.io/wal-dir";
    public static final String ANNOTATION_VOLUME_MOUNTS = "stackgres.io/volume-mounts";
    public static final String ANNOTATION_FLAVOR = "stackgres.io/flavor";
    public static final String ANNOTATION_IVORY_SQL_PORT = "stackgres.io/ivorysql/port";

    public static final String ETCD_IMAGE = "quay.io/coreos/etcd:v3.5.15";
    public static final String ANNOTATION_ETCD_INITIAL_CLUSTER = "stackgres.io/high-availability/etcd-initial-cluster";
    public static final String ANNOTATION_ETCD_NAME = "stackgres.io/high-availability/etcd-name";
    public static final String ANNOTATION_ETCD_CLIENT_URL = "stackgres.io/high-availability/etcd-client-url";
    public static final String ANNOTATION_ETCD_SERVER_URL = "stackgres.io/high-availability/etcd-server-url";

    private final Directories directories = new Directories();
    private final CriClientRequests clientRequests;
    private final EtcdRequests etcdRequests = new EtcdRequests();

    private final EpollEventLoopGroup eventLoop = new EpollEventLoopGroup();
    private final ManagedChannel channel;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public Cri() {
//        String criSocketPath = Config.getValue("STACKGRES_CRI_SOCKET_PATH", "/var/run/stackgres/containerd/containerd.sock");
        String criSocketPath = Config.getValue("STACKGRES_CRI_SOCKET_PATH", "/var/run/containerd/containerd.sock");
        clientRequests = new CriClientRequests(criSocketPath);
        channel = NettyChannelBuilder
                .forAddress(new DomainSocketAddress(criSocketPath))
                .eventLoopGroup(eventLoop)
                .channelType(EpollDomainSocketChannel.class)
                .usePlaintext()
                .withOption(ChannelOption.SO_KEEPALIVE, false)
                .withOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .build();
    }

    public List<PostgresCluster> listExistingClusters() {
        RuntimeServiceGrpc.RuntimeServiceStub runtimeService = runtimeService();
        return listClusterPods(runtimeService)
                .stream().map(pod -> mapPostgresCluster(pod, runtimeService))
                .toList();
    }

    private PostgresCluster mapPostgresCluster(PodSandbox pod, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        UUID instanceId = UUID.fromString(pod.getMetadata().getUid());
        UUID clusterId = UUID.fromString(pod.getAnnotationsOrDefault(ANNOTATION_CLUSTER_ID, null));
        String name = pod.getAnnotationsOrDefault(ANNOTATION_NAME, null);
        String version = pod.getAnnotationsOrDefault(ANNOTATION_PG_VERSION, null);
        boolean standalone = Boolean.parseBoolean(pod.getAnnotationsOrDefault(ANNOTATION_STANDALONE, "false"));
        String username = pod.getAnnotationsOrDefault(ANNOTATION_USERNAME, null);
        String password = pod.getAnnotationsOrDefault(ANNOTATION_PASSWORD, null);
        List<Extension> extensions = Extension.extractExtensions(pod.getAnnotationsOrDefault(ANNOTATION_EXTENSIONS, ""));
        Map<String, String> tags = Mappers.extractMap(pod.getAnnotationsOrDefault(ANNOTATION_TAGS, ""));
        Flavor flavor = Flavor.fromId(pod.getAnnotationsOrDefault(ANNOTATION_FLAVOR, null));
        String ivorySqlPortStr = pod.getAnnotationsOrDefault(ANNOTATION_IVORY_SQL_PORT, null);
        Integer ivorySqlPort = ivorySqlPortStr != null ? Integer.parseInt(ivorySqlPortStr) : null;

        int port = Integer.parseInt(pod.getAnnotationsOrDefault(ANNOTATION_PORT, "0"));
        String listenAddress = pod.getAnnotationsOrDefault(ANNOTATION_LISTEN_ADDRESS, null);
        Path configPath = Paths.get(pod.getAnnotationsOrDefault(ANNOTATION_PG_CONFIG_FILE, null));
        Path dataDir = Paths.get(pod.getAnnotationsOrDefault(ANNOTATION_PG_DATA_PATH, null));
        Path logDir = Paths.get(pod.getAnnotationsOrDefault(ANNOTATION_LOG_DIR, null));
        Path walDir = Paths.get(pod.getAnnotationsOrDefault(ANNOTATION_WAL_DIR, null));
        List<VolumeMount> volumeMounts = VolumeMount.parseMounts(pod.getAnnotationsOrDefault(ANNOTATION_VOLUME_MOUNTS, ""));

        String instanceName = pod.getAnnotationsOrDefault(ANNOTATION_INSTANCE_NAME, (name != null) ? (name + "-0") : null);
        SlonyLinuxInstance instance;
        if (standalone) {
            instance = new SlonyLinuxInstance(instanceId, instanceName, version, port, listenAddress, Status.UNKNOWN, null, configPath, dataDir, logDir, walDir, volumeMounts);
        } else {
            String etcdName = pod.getAnnotationsOrDefault(ANNOTATION_ETCD_NAME, null);
            String etcdClientUrl = pod.getAnnotationsOrDefault(ANNOTATION_ETCD_CLIENT_URL, null);
            String etcdServerUrl = pod.getAnnotationsOrDefault(ANNOTATION_ETCD_SERVER_URL, null);
            instance = new SlonyLinuxHAInstance(instanceId, instanceName, version, port, listenAddress, Status.UNKNOWN, null, configPath, dataDir, logDir, walDir, volumeMounts, etcdName, etcdServerUrl, etcdClientUrl);
        }

        ImageInfo imageInfo = getContainerImageInfo(pod.getId(), runtimeService);
        if (imageInfo != null) {
            instance.setImageName(imageInfo.imageName());
            instance.setImageDigest(imageInfo.imageDigest());
        }
        instance.setIvorySqlPort(ivorySqlPort);

        PostgresCluster cluster = new PostgresCluster(clusterId, name, username, password, extensions, tags, standalone, List.of(instance));
        cluster.setFlavor(flavor);

        if (!standalone) {
            Map<String, String> etcd = Mappers.extractMap(pod.getAnnotationsOrDefault(ANNOTATION_ETCD_INITIAL_CLUSTER, ""));
            cluster.getEtcdInitialCluster().putAll(etcd);
        }

        return cluster;
    }

    private ImageInfo getContainerImageInfo(String podId, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        List<Container> containers = clientRequests.listPodContainers(podId, runtimeService);
        return containers.stream()
                .filter(c -> c.getState() == ContainerState.CONTAINER_RUNNING)
                .findFirst()
                .map(c -> new ImageInfo(c.getImage().getImage(), c.getImageRef()))
                .orElse(null);
    }

    public void createCluster(PostgresCluster cluster, BiConsumer<UUID, ImageInfo> onInstanceCreated, CreationEventPublisher eventPublisher) {
        try {
            if (cluster.isStandalone()) {
                SlonyLinuxInstance instance = slonyStandaloneInstance(cluster);
                String resolvedRef = createAndStartInstance(cluster, instance, eventPublisher);
                onInstanceCreated.accept(instance.getId(), new ImageInfo(instance.imageRef(cluster.getExtensions(), cluster.getFlavor()), resolvedRef));
            } else {
                String containerId = null;
                ClusterInstance lastInstance = null;
                for (ClusterInstance instance : cluster.getInstances()) {
                    containerId = startEtcd(cluster, (SlonyLinuxHAInstance) instance, eventPublisher);
                    lastInstance = instance;
                }

                waitForEtcdHealth(containerId, (SlonyLinuxHAInstance) lastInstance, cluster.getId(), eventPublisher);

                for (ClusterInstance instance : cluster.getInstances()) {
                    try {
                        String resolvedRef = createAndStartHAInstance(cluster, (SlonyLinuxHAInstance) instance, eventPublisher);
                        onInstanceCreated.accept(instance.getId(), new ImageInfo(instance.imageRef(cluster.getExtensions(), cluster.getFlavor()), resolvedRef));
                    } catch (Exception e) {
                        throw new InstanceCreationException(instance.getId(), e);
                    }
                }
            }
        } catch (InstanceCreationException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getCause() instanceof CriClientException
                && ((CriClientException) e.getCause()).getRoot() instanceof StatusRuntimeException statusRuntime
                && statusRuntime.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
                String message = statusRuntime.getMessage() != null ? statusRuntime.getMessage() : "Image not found";
                throw new IllegalArgumentException(message, e);
            } else {
                throw e;
            }
        }
    }

    private String createAndStartInstance(PostgresCluster cluster, SlonyLinuxInstance instance, CreationEventPublisher eventPublisher) {
        RuntimeServiceGrpc.RuntimeServiceStub runtimeService = runtimeService();
        String resolvedRef = downloadImage(instance.getId(), instance.imageRef(cluster.getExtensions(), cluster.getFlavor()), eventPublisher);

        directories.createDirectories(instance);
        PodSandboxConfig podSandboxConfig = postgresPodSandboxConfig(cluster, instance);
        String podId = clientRequests.createPod(podSandboxConfig, runtimeService);
        String containerId = createContainer(cluster, instance, podId, podSandboxConfig, runtimeService);

        clientRequests.startContainer(runtimeService, containerId);
        return resolvedRef;
    }

    private String downloadImage(UUID instanceId, String image, CreationEventPublisher eventPublisher) {
        ImageServiceGrpc.ImageServiceStub imageService = ImageServiceGrpc.newStub(channel);

        if (clientRequests.imageExists(image, imageService)) {
            logger.log(System.Logger.Level.INFO, "Image {0} already exists", image);
            String digest = clientRequests.getImageRef(image, imageService);
            eventPublisher.imageCached(instanceId, image, digest);
            return digest;
        } else {
            logger.log(System.Logger.Level.INFO, "Pulling image {0} ...", image);
            String digest = clientRequests.pullImage(image, imageService);
            logger.log(System.Logger.Level.INFO, "Pulled image {0} ({1})", image, digest);
            eventPublisher.imagePulled(instanceId, image, digest);
            return digest;
        }
    }

    private String createContainer(PostgresCluster cluster, SlonyLinuxInstance instance, String podId, PodSandboxConfig podSandboxConfig, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        String logFileName = directories.logFileName(cluster);
        ContainerConfig containerConfig = postgresContainerConfig(cluster, instance, logFileName);
        return clientRequests.createContainer(podId, containerConfig, podSandboxConfig, runtimeService);
    }

    private String startEtcd(PostgresCluster cluster, SlonyLinuxHAInstance instance, CreationEventPublisher eventPublisher) {
        RuntimeServiceGrpc.RuntimeServiceStub runtimeService = runtimeService();
        downloadImage(instance.getId(), ETCD_IMAGE, eventPublisher);
        eventPublisher.etcdImagePulled(instance.getId(), ETCD_IMAGE);

        PodSandboxConfig podSandboxConfig = etcdPodSandboxConfig(instance);
        String podId = clientRequests.createPod(podSandboxConfig, runtimeService);

        String logFileName = directories.etcdLogFileName();
        Path etcdDataPath = directories.etcdDataPath(cluster);
        ContainerConfig containerConfig = etcdContainerConfig(cluster, instance, logFileName, etcdDataPath);
        String containerId = clientRequests.createContainer(podId, containerConfig, podSandboxConfig, runtimeService);
        clientRequests.startContainer(runtimeService, containerId);
        eventPublisher.etcdContainerStarted(instance.getId());
        return containerId;
    }

    private void waitForEtcdHealth(String containerId, SlonyLinuxHAInstance instance, UUID clusterId, CreationEventPublisher eventPublisher) {
        RuntimeServiceGrpc.RuntimeServiceStub runtimeService = runtimeService();
        int countdown = 10;
        boolean etcdHealthy = false;
        while (--countdown > 0 && !etcdHealthy) {
            sleep1Second();
            etcdHealthy = isEtcdHealthy(containerId, instance, runtimeService, false);
        }

        if (!isEtcdHealthy(containerId, instance, runtimeService, true)) {
            eventPublisher.etcdStartupTimeout(clusterId);
            throw new IllegalStateException("Could not successfully start etcd. Please check the stackgres logs.");
        }
        eventPublisher.etcdHealthy(clusterId);
    }

    private boolean isEtcdHealthy(String containerId, SlonyLinuxHAInstance instance, RuntimeServiceGrpc.RuntimeServiceStub runtimeService, boolean logOnFailure) {
        try {
            ExecSyncResponse response = clientRequests.execSyncInContainer(containerId, List.of("etcdctl", "endpoint", "health", "-w", "json", "--endpoints=" + instance.getEtcdClientUrl()), runtimeService);

            boolean healthy = etcdRequests.isHealthy(response.getStdout().toStringUtf8());
            if (!healthy && logOnFailure)
                logger.log(System.Logger.Level.ERROR, "etcd did not start successfully, exit code: {0}, {1}, {2}", response.getExitCode(), response.getStdout(), response.getStderr());
            return healthy;
        } catch (Exception e) {
            if (logOnFailure)
                logger.log(System.Logger.Level.ERROR, "etcd did not start successfully, reason: {0}", e.getMessage());
            return false;
        }
    }

    private static void sleep1Second() {
        try {
            Thread.sleep(1_000);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private String createAndStartHAInstance(PostgresCluster cluster, SlonyLinuxHAInstance instance, CreationEventPublisher eventPublisher) {
        RuntimeServiceGrpc.RuntimeServiceStub runtimeService = runtimeService();
        String resolvedRef = downloadImage(instance.getId(), instance.imageRef(cluster.getExtensions(), cluster.getFlavor()), eventPublisher);

        int patroniApiPort = Networks.nextUnusedPort(8008);
        PodSandboxConfig podSandboxConfig = postgresPodSandboxConfig(cluster, instance);
        String podId = clientRequests.createPod(podSandboxConfig, runtimeService);
        String ipAddress = SlonySystem.getExternalAddress();
        String logFileName = directories.logFileName(cluster);
        ContainerConfig containerConfig = haPostgresContainerConfig(cluster, instance, ipAddress, patroniApiPort, logFileName);
        String containerId = clientRequests.createContainer(podId, containerConfig, podSandboxConfig, runtimeService);

        clientRequests.startContainer(runtimeService, containerId);
        return resolvedRef;
    }

    public void close() {
        try {
            channel.shutdownNow().awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        eventLoop.shutdownGracefully(50, 50, TimeUnit.MILLISECONDS);
    }

    private RuntimeServiceGrpc.RuntimeServiceStub runtimeService() {
        return RuntimeServiceGrpc.newStub(channel);
    }

    private List<PodSandbox> listClusterPods(RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        return clientRequests.listPods(Map.of(LABEL_MANAGED_BY, "stackgres", LABEL_COMPONENT, "postgres"), runtimeService);
    }

    private void stop(PodSandbox pod, List<Container> containers, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        containers.stream()
                .filter(c -> c.getPodSandboxId().equals(pod.getId()))
                .forEach(c -> {
                    clientRequests.stopContainer(c.getId(), runtimeService);
                    clientRequests.removeContainer(c.getId(), runtimeService);
                });
    }

    public SlonyLinuxInstance deleteInstance(UUID instanceId) {
        RuntimeServiceGrpc.RuntimeServiceStub runtimeService = runtimeService();
        PodSandbox pod = getPod(instanceId, runtimeService);
        PostgresCluster cluster = mapPostgresCluster(pod, runtimeService);

        SlonyLinuxInstance instance = slonyStandaloneInstance(cluster);
        List<Container> containers = clientRequests.listPodContainers(pod.getId(), runtimeService);
        stop(pod, containers, runtimeService);
        clientRequests.removePod(pod.getId(), runtimeService);
        directories.deleteLogsDir(instance);

        if (!cluster.isStandalone()) {
            Optional<PodSandbox> etcdPod = getEtcdPod(runtimeService, instance.getId());
            etcdPod.ifPresent(p -> {
                List<Container> etcdContainers = clientRequests.listPodContainers(p.getId(), runtimeService);
                stop(p, etcdContainers, runtimeService);
                clientRequests.removePod(p.getId(), runtimeService);
            });
        }
        return instance;
    }

    private Optional<PodSandbox> getEtcdPod(RuntimeServiceGrpc.RuntimeServiceStub runtimeService, UUID instanceId) {
        String name = "etcd-" + instanceId.toString();
        return clientRequests.listPods(Map.of(LABEL_MANAGED_BY, "stackgres", LABEL_COMPONENT, "etcd"), runtimeService).stream()
                .filter(p -> name.equals(p.getMetadata().getName()))
                .findAny();
    }

    private PodSandbox getPod(UUID instanceId, RuntimeServiceGrpc.RuntimeServiceStub runtimeService) {
        return listClusterPods(runtimeService).stream()
                .filter(p -> p.getMetadata().getUid().equals(instanceId.toString()))
                .findAny().orElseThrow(() -> new NoSuchElementException("Cluster instance " + instanceId + " not found") {
                });
    }

    private static SlonyLinuxInstance slonyStandaloneInstance(PostgresCluster cluster) {
        if (cluster.getInstances().size() != 1)
            throw new IllegalArgumentException("Expected standalone Slony cluster with exactly one instance");
        ClusterInstance instance = cluster.getInstances().iterator().next();
        if (!(instance instanceof SlonyLinuxInstance))
            throw new IllegalArgumentException("Only SlonyLinux instances are supported");
        return (SlonyLinuxInstance) instance;
    }

    public record ImageInfo(String imageName, String imageDigest) {
    }

}