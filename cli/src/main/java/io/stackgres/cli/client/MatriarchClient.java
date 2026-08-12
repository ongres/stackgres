package io.stackgres.cli.client;

import com.google.protobuf.ByteString;
import io.stackgres.cli.Config;
import io.stackgres.cli.Strings;
import io.stackgres.cli.commands.ProgressMessages;
import io.stackgres.cli.postgres.ClusterDiagnostics;
import io.stackgres.cli.postgres.Slon;
import io.stackgres.cli.postgres.Slony;
import io.stackgres.postgres.ClusterInstance;
import io.stackgres.postgres.PostgresCluster;
import io.stackgres.proto.api.v1.*;
import io.stackgres.proto.api.v1.CreateClusterRequest;
import io.stackgres.proto.api.v1.DeleteClusterRequest;
import io.stackgres.proto.api.v1.ListClustersRequest;
import io.stackgres.proto.api.v1.RestartClusterRequest;
import io.stackgres.proto.api.v1.StackGresApiGrpc.StackGresApiBlockingStub;
import io.stackgres.proto.api.v1.StartClusterRequest;
import io.stackgres.proto.api.v1.StopClusterRequest;
import io.stackgres.proto.cli.*;
import io.stackgres.proto.cli.GetClusterDiagnosticsRequest;
import io.stackgres.proto.cli.GetClusterDiagnosticsResponse;
import io.stackgres.proto.types.v1.Id;
import io.grpc.*;
import io.grpc.stub.BlockingClientCall;
import io.grpc.stub.StreamObserver;

import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static io.stackgres.cli.Strings.commentAnsi;
import static io.stackgres.cli.client.Mappers.mapTimestamp;
import static io.stackgres.cli.client.Mappers.mapUUID;
import static java.util.concurrent.TimeUnit.SECONDS;

public class MatriarchClient {

    private final ClusterServiceGrpc.ClusterServiceBlockingV2Stub clusterClient;
    private final ClusterServiceGrpc.ClusterServiceStub asyncClient;
    private final AccountServiceGrpc.AccountServiceBlockingV2Stub accountClient;
    private final ResourceServiceGrpc.ResourceServiceBlockingV2Stub resourceClient;
    private final ResourceServiceGrpc.ResourceServiceStub asyncResourceClient;
    // stackgres.api.v1 — cluster CRUD talks this; the rest still use the cli.proto stubs above.
    private final StackGresApiBlockingStub stackGresClient;

    private final String matriarchUrl;
    private boolean debug;
    private ProgressMessages debugMessages;

    public MatriarchClient() {
        this.matriarchUrl = Config.getValue("STACKGRES_MATRIARCH_URL", "localhost:50051");
        boolean matriarchTls = detectMatriarchTls(matriarchUrl);
        ChannelCredentials channelCredentials = matriarchTls ? TlsChannelCredentials.create() : InsecureChannelCredentials.create();
        ManagedChannel channel = Grpc.newChannelBuilder(matriarchUrl, channelCredentials).build();
        String token = Config.getValue("STACKGRES_TOKEN", null);
        JwtCredential credential = token != null ? new JwtCredential(token) : null;
        clusterClient = ClusterServiceGrpc.newBlockingV2Stub(channel).withCallCredentials(credential);
        asyncClient = ClusterServiceGrpc.newStub(channel).withCallCredentials(credential);
        accountClient = AccountServiceGrpc.newBlockingV2Stub(channel).withCallCredentials(credential);
        resourceClient = ResourceServiceGrpc.newBlockingV2Stub(channel).withCallCredentials(credential);
        asyncResourceClient = ResourceServiceGrpc.newStub(channel).withCallCredentials(credential);
        stackGresClient = StackGresApiGrpc.newBlockingStub(channel).withCallCredentials(credential);
    }

    private boolean detectMatriarchTls(String matriarchUrl) {
        String configTls = Config.getValue("STACKGRES_MATRIARCH_TLS", null);
        if (configTls == null) {
            // assume TLS unless localhost
            return !matriarchUrl.startsWith("localhost:");
        }
        return Boolean.parseBoolean(configTls);
    }

    private RuntimeException statusError(StatusRuntimeException e) {
        String detail = e.getStatus().getDescription() != null ? e.getStatus().getDescription() : e.getMessage();
        if (e.getStatus().getCode() == io.grpc.Status.Code.UNAVAILABLE) {
            return new RuntimeException("cannot reach the matriarch at " + matriarchUrl + " — is it running? Set STACKGRES_MATRIARCH_URL (default localhost:50051; the api.v1 matriarch serves 9000). [" + detail + "]");
        }
        return new RuntimeException(detail);
    }

    public void createCluster(PostgresCluster cluster, Map<String, String> nodeSelector, Consumer<ClusterCreationUpdate> statusConsumer) {
        CreateClusterRequest request = Mappers.createClusterRequest(cluster);
        logDebug("Creating cluster: " + request);

        boolean announced = false;
        boolean succeeded = false;
        boolean pending = false;
        String lastId = "";
        String lastName = cluster.getName() == null ? "" : cluster.getName();
        try {
            Iterator<ClusterOperationProgress> stream = stackGresClient.createCluster(request);
            while (stream.hasNext()) {
                ClusterOperationProgress p = stream.next();
                logDebug("Received progress: " + p);
                String id = p.getCluster().getId().getValue();
                lastId = id;
                lastName = p.getCluster().getName();
                switch (p.getStatus()) {
                    case OPERATION_STATUS_SUCCEEDED -> {
                        succeeded = true;
                        statusConsumer.accept(new ClusterCreationUpdate(id, p.getCluster().getName(), "", ClusterCreationUpdate.Status.HEALTHY));
                    }
                    case OPERATION_STATUS_FAILED -> throw new RuntimeException(p.getError().getMessage());
                    default -> {
                        pending = p.getCluster().getStatus()
                                  == io.stackgres.proto.types.v1.ClusterStatus.CLUSTER_STATUS_PENDING;
                        if (!announced) {
                            announced = true;
                            // Secret-by-reference (§3.7): a generated password isn't in the stream — fetch
                            // it out-of-band via GetClusterCredentials. If the user supplied -P, skip it
                            // (the command only prints the password when the user didn't provide one).
                            String password = "";
                            if (cluster.getPassword() == null) {
                                try {
                                    password = getClusterCredentials(id);
                                } catch (RuntimeException ex) {
                                    logDebug("could not fetch credentials: " + ex.getMessage());
                                }
                            }
                            statusConsumer.accept(new ClusterCreationUpdate(id, p.getCluster().getName(), password, ClusterCreationUpdate.Status.CREATED));
                        }
                    }
                }
            }
            // No agent connected → the create rests at PENDING; tell the user it will provision later.
            if (!succeeded && pending) {
                statusConsumer.accept(new ClusterCreationUpdate(lastId, lastName, "", ClusterCreationUpdate.Status.PENDING));
            }
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    /**
     * Resolve a cluster's superuser password out-of-band (§3.7 separate call).
     */
    public String getClusterCredentials(String clusterId) {
        GetClusterCredentialsRequest request = GetClusterCredentialsRequest.newBuilder()
                .setEnvironmentId("local")
                .setClusterId(Id.newBuilder().setValue(clusterId))
                .build();
        try {
            return stackGresClient.getClusterCredentials(request).getPassword();
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public void deleteCluster(String clusterName, Consumer<String> deletionConsumer) {
        PostgresCluster cluster = getCluster(clusterName);
        deleteById(cluster.getId().toString(), clusterName, deletionConsumer);
    }

    public void deleteAllClusters(Consumer<String> deletionConsumer) {
        for (PostgresCluster cluster : listClusters(Map.of()))
            deleteById(cluster.getId().toString(), cluster.getName(), deletionConsumer);
    }

    public void deleteClusters(Map<String, String> tags, Consumer<String> deletionConsumer) {
        for (PostgresCluster cluster : listClusters(tags))
            deleteById(cluster.getId().toString(), cluster.getName(), deletionConsumer);
    }

    // api.v1 delete is by-id; the CLI resolves the name/tags → id(s) client-side above.
    private void deleteById(String id, String name, Consumer<String> deletionConsumer) {
        DeleteClusterRequest request = DeleteClusterRequest.newBuilder()
                        .setSelector(io.stackgres.proto.api.v1.ClusterSelector.newBuilder()
                                .setEnvironmentId("local")
                                .setId(Id.newBuilder().setValue(id)))
                        .setIdempotencyKey(id)
                        .build();
        try {
            Iterator<ClusterOperationProgress> stream = stackGresClient.deleteCluster(request);
            while (stream.hasNext()) {
                ClusterOperationProgress p = stream.next();
                logDebug("Received progress: " + p);
                switch (p.getStatus()) {
                    case OPERATION_STATUS_SUCCEEDED -> deletionConsumer.accept(name);
                    case OPERATION_STATUS_FAILED -> throw new RuntimeException(p.getError().getMessage());
                    default -> {
                    }
                }
            }
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public void startCluster(String clusterName) {
        lifecycle(LifecycleVerb.START, getCluster(clusterName).getId().toString());
    }

    public void startAllClusters() {
        for (PostgresCluster c : listClusters(Map.of())) if (!c.isRunning()) lifecycle(LifecycleVerb.START, c.getId().toString());
    }

    public void startClusters(Map<String, String> tags) {
        for (PostgresCluster c : listClusters(tags)) if (!c.isRunning()) lifecycle(LifecycleVerb.START, c.getId().toString());
    }

    public void stopCluster(String clusterName) {
        lifecycle(LifecycleVerb.STOP, getCluster(clusterName).getId().toString());
    }

    public void stopAllClusters() {
        for (PostgresCluster c : listClusters(Map.of())) if (c.isRunning()) lifecycle(LifecycleVerb.STOP, c.getId().toString());
    }

    public void stopClusters(Map<String, String> tags) {
        for (PostgresCluster c : listClusters(tags)) if (c.isRunning()) lifecycle(LifecycleVerb.STOP, c.getId().toString());
    }

    public void restartCluster(String clusterName) {
        lifecycle(LifecycleVerb.RESTART, getCluster(clusterName).getId().toString());
    }

    public void restartAllClusters() {
        for (PostgresCluster c : listClusters(Map.of())) lifecycle(LifecycleVerb.RESTART, c.getId().toString());
    }

    public void restartClusters(Map<String, String> tags) {
        for (PostgresCluster c : listClusters(tags)) lifecycle(LifecycleVerb.RESTART, c.getId().toString());
    }

    private enum LifecycleVerb {START, STOP, RESTART}

    // api.v1 lifecycle is by-id (name/tags resolved client-side); streams progress to a terminal frame.
    private void lifecycle(LifecycleVerb verb, String id) {
        var selector = io.stackgres.proto.api.v1.ClusterSelector.newBuilder()
                .setEnvironmentId("local")
                .setId(Id.newBuilder().setValue(id))
                .build();
        String key = UUID.randomUUID().toString();
        try {
            Iterator<ClusterOperationProgress> stream = switch (verb) {
                case START -> stackGresClient.startCluster(StartClusterRequest.newBuilder().setSelector(selector).setIdempotencyKey(key).build());
                case STOP -> stackGresClient.stopCluster(StopClusterRequest.newBuilder().setSelector(selector).setIdempotencyKey(key).build());
                case RESTART -> stackGresClient.restartCluster(RestartClusterRequest.newBuilder().setSelector(selector).setIdempotencyKey(key).build());
            };
            while (stream.hasNext()) {
                ClusterOperationProgress p = stream.next();
                logDebug("Received progress: " + p);
                if (p.getStatus() == OperationStatus.OPERATION_STATUS_FAILED) {
                    throw new RuntimeException(p.getError().getMessage());
                }
            }
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    private void checkActionStatus(com.google.rpc.Status status) {
        logDebug("Received response: " + status);
        if (status.getCode() != com.google.rpc.Code.OK.getNumber())
            throw new RuntimeException(status.getMessage());
    }

    public List<PostgresCluster> listClusters(Map<String, String> tags) {
        ListClustersRequest request = ListClustersRequest.newBuilder().setEnvironmentId("local").putAllTags(tags).build();
        try {
            io.stackgres.proto.api.v1.ListClustersResponse response = stackGresClient.listClusters(request);
            logDebug("Received response: " + response);
            return response.getClusterList().stream()
                    .map(Mappers::mapCluster)
                    .toList();
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public List<String> listAvailableVersions(io.stackgres.postgres.Flavor flavor) {
        try {
            var response = stackGresClient.listVersions(io.stackgres.proto.api.v1.ListVersionsRequest.newBuilder()
                    .setEngine(Mappers.mapEngine(flavor))
                    .build());
            logDebug("Received response: " + response);
            return response.getVersionList();
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public PostgresCluster getCluster(String name) {
        // api.v1 GetCluster is by-id in the matriarch; resolve by name client-side via list.
        return listClusters(Map.of()).stream()
                .filter(c -> name.equals(c.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("The cluster with name " + name + " doesn't exist"));
    }

    public ExecSession execInCluster(String name, String instanceName, List<String> commands, Flow.Publisher<ByteString> cliExecSupplier, Consumer<ByteString> matriarchConsumer) {
        PostgresCluster cluster = getCluster(name);
        if (!cluster.isRunning())
            throw new RuntimeException("The cluster " + name + " is not running");
        UUID instanceId = instanceId(cluster, instanceName);
        if (!cluster.isStandalone() && instanceId == null)
            System.err.println(Strings.commentAnsi("No instance name is given, exec-ing in random cluster instance"));
        CompletableFuture<Integer> exitCode = new CompletableFuture<>();

        StreamObserver<CliExecMessage> cliExecObserver = asyncClient.execInCluster(new StreamObserver<>() {
            @Override
            public void onNext(MatriarchExecMessage message) {
                if (message.hasStatus()) {
                    exitCode.completeExceptionally(new IllegalStateException(message.getStatus().getMessage()));
                }
                if (message.hasBytes()) {
                    matriarchConsumer.accept(message.getBytes());
                }
                if (message.hasExitCode()) {
                    exitCode.complete(message.getExitCode());
                }
            }

            @Override
            public void onError(Throwable t) {
                // TODO handle more specific errors
                exitCode.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                if (!exitCode.isDone())
                    exitCode.completeExceptionally(new RuntimeException("Matriarch terminated unexpectedly"));
            }
        });

        cliExecSupplier.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteString item) {
                cliExecObserver.onNext(CliExecMessage.newBuilder().setBytes(item).build());
            }

            @Override
            public void onError(Throwable throwable) {
                cliExecObserver.onError(throwable);
            }

            @Override
            public void onComplete() {
                cliExecObserver.onCompleted();
            }
        });

        ExecCommand.Builder builder = ExecCommand.newBuilder()
                .setClusterId(mapUUID(cluster.getId()))
                .addAllCommand(commands);
        if (instanceId != null)
            builder.setInstanceId(mapUUID(instanceId));
        cliExecObserver.onNext(CliExecMessage.newBuilder().setExecCommand(builder).build());

        return new ExecSession(exitCode, cliExecObserver);
    }

    public String getClusterLogs(String name, String instanceName, String component) {
        PostgresCluster cluster = getCluster(name);
        UUID instanceId = instanceId(cluster, instanceName);
        if (!cluster.isStandalone() && instanceId == null) {
            System.err.println(Strings.commentAnsi("No instance name is given, selecting random cluster instance"));
        }
        GetClusterLogsRequest.Builder builder = GetClusterLogsRequest.newBuilder()
                .setClusterId(mapUUID(cluster.getId()))
                .setComponent(mapLogComponent(component, cluster));
        if (instanceId != null)
            builder.setInstanceId(mapUUID(instanceId));

        try {
            GetClusterLogsResponse response = clusterClient.getClusterLogs(builder.build());
            logDebug("Received response: " + response);
            if (response.hasLogs()) {
                return response.getLogs();
            } else if (response.hasStatus()) {
                throw new RuntimeException(response.getStatus().getMessage());
            }
            throw new IllegalStateException("No response sent");
        } catch (StatusException e) {
            if (e.getStatus().getCode() == Status.Code.UNIMPLEMENTED) {
                throw new IllegalStateException("This Matriarch server does not support cluster logs. Please connect to StackGres Cloud.");
            }
            throw handleStatusException(e);
        }
    }

    public Runnable followClusterLogs(String name, String instanceName, String component, Consumer<String> consumer, Consumer<String> onCompleted) {
        PostgresCluster cluster = getCluster(name);
        UUID instanceId = instanceId(cluster, instanceName);
        if (!cluster.isStandalone() && instanceId == null) {
            System.err.println(Strings.commentAnsi("No instance name is given, selecting random cluster instance"));
        }
        TailClusterLogsRequest.Builder builder = TailClusterLogsRequest.newBuilder().setClusterId(mapUUID(cluster.getId()));
        if (instanceId != null)
            builder.setInstanceId(mapUUID(instanceId));
        builder.setComponent(mapLogComponent(component, cluster));
        StreamObserver<TailClusterLogsResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(TailClusterLogsResponse response) {
                logDebug("Received response: " + response);
                if (response.hasStatus()) {
                    onCompleted.accept(response.getStatus().getMessage());
                } else if (response.hasLine()) {
                    consumer.accept(response.getLine());
                }
            }

            @Override
            public void onError(Throwable t) {
                logDebug("received error from server " + t);
                if (t instanceof StatusRuntimeException e && e.getStatus().getCode() == Status.Code.UNIMPLEMENTED)
                    onCompleted.accept("This Matriarch server does not support cluster logs. Please connect to StackGres Cloud.");
                else
                    onCompleted.accept(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
            }

            @Override
            public void onCompleted() {
                logDebug("received close stream from server");
                onCompleted.accept(null);
            }
        };
        StreamObserver<TailClusterLogsRequest> requestObserver = asyncClient.tailClusterLogs(responseObserver);
        requestObserver.onNext(builder.build());
        return requestObserver::onCompleted;
    }

    public ClusterCheckpoints getClusterCheckpoints(String name, String instanceName, String database, Instant start, Instant end, int maxResults) {
        PostgresCluster cluster = getCluster(name);
        UUID instanceId = instanceId(cluster, instanceName);
        GetClusterCheckpointsRequest.Builder builder = GetClusterCheckpointsRequest.newBuilder()
                .setClusterId(mapUUID(cluster.getId()))
                .setStart(mapTimestamp(start))
                .setEnd(mapTimestamp(end))
                .setMaxResults(maxResults)
                .setDirection(SortDirection.SORT_DIRECTION_DESC);
        if (instanceId != null)
            builder.setInstanceId(mapUUID(instanceId));
        if (database != null && !database.isBlank())
            builder.setDatabase(database);

        try {
            GetClusterCheckpointsResponse response = clusterClient.getClusterCheckpoints(builder.build());
            logDebug("Received response: " + response);
            if (response.hasCheckpoints())
                return response.getCheckpoints();
            if (response.hasStatus())
                throw new RuntimeException(response.getStatus().getMessage());
            throw new IllegalStateException("No response sent");
        } catch (StatusException e) {
            if (e.getStatus().getCode() == Status.Code.UNIMPLEMENTED) {
                throw new IllegalStateException("This Matriarch server does not support cluster metrics. Please connect to StackGres Cloud.");
            }
            throw handleStatusException(e);
        }
    }

    private static LogComponent mapLogComponent(String component, PostgresCluster cluster) {
        if (component == null)
            return cluster.isStandalone() ? LogComponent.LOG_COMPONENT_POSTGRES : LogComponent.LOG_COMPONENT_PATRONI;
        return switch (component.toLowerCase()) {
            case "postgres" -> LogComponent.LOG_COMPONENT_POSTGRES;
            case "patroni" -> LogComponent.LOG_COMPONENT_PATRONI;
            case "slon" -> LogComponent.LOG_COMPONENT_SLON;
            case "etcd" -> LogComponent.LOG_COMPONENT_ETCD;
            default -> throw new IllegalArgumentException("Unknown log component: " + component + ". Valid values: postgres, patroni, slon, etcd");
        };
    }

    public PgWireTunnel openPgWireTunnel(PostgresCluster cluster, String instanceName, PgWireTarget target, boolean warnOnRandomInstance,
                                         Consumer<byte[]> dataConsumer, Runnable onClosed, Consumer<String> onAborted, Consumer<String> onError) {
        UUID instanceId = instanceId(cluster, instanceName);
        if (!cluster.isStandalone() && instanceId == null && warnOnRandomInstance) {
            System.err.println(Strings.commentAnsi("No instance name is given, selecting random cluster instance"));
        }

        CompletableFuture<Boolean> opened = new CompletableFuture<>();

        StreamObserver<PgWireClientMessage> requestObserver = asyncClient.pgWireTunnel(new StreamObserver<>() {
            @Override
            public void onNext(PgWireServerMessage message) {
                logDebug("Received tunnel message: " + message.getKindCase());
                if (message.hasOpened())
                    opened.complete(true);
                if (message.hasData())
                    dataConsumer.accept(message.getData().getData().toByteArray());
                if (message.hasClosed()) {
                    if (!opened.isDone())
                        opened.completeExceptionally(new RuntimeException("Tunnel closed before opening"));
                    else
                        onClosed.run();
                }
                if (message.hasAborted()) {
                    if (!opened.isDone())
                        opened.completeExceptionally(new RuntimeException("Tunnel aborted: " + message.getAborted().getReason()));
                    else
                        onAborted.accept(message.getAborted().getReason());
                }
            }

            @Override
            public void onError(Throwable t) {
                logDebug("Tunnel error: " + t.getMessage());
                if (!opened.isDone())
                    opened.completeExceptionally(t);
                else
                    onError.accept(t.getMessage());
            }

            @Override
            public void onCompleted() {
                logDebug("Tunnel stream completed");
                if (!opened.isDone())
                    opened.completeExceptionally(new RuntimeException("Stream completed before tunnel opened"));
                else
                    onClosed.run();
            }
        });

        PgWireTunnelOpen.Builder builder = PgWireTunnelOpen.newBuilder().setClusterId(mapUUID(cluster.getId())).setTarget(target);
        if (instanceId != null)
            builder.setInstanceId(mapUUID(instanceId));
        requestObserver.onNext(PgWireClientMessage.newBuilder().setOpen(builder.build()).build());

        try {
            opened.get(30, SECONDS);
        } catch (TimeoutException e) {
            requestObserver.onCompleted();
            throw new RuntimeException("Timeout waiting for tunnel to open", e);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause instanceof StatusRuntimeException sre)
                throw new RuntimeException(sre.getStatus().getDescription(), sre);
            throw new RuntimeException(e.getMessage(), e);
        }

        return new PgWireTunnel(requestObserver);
    }

    private static UUID instanceId(PostgresCluster cluster, String instanceName) {
        if (instanceName == null)
            return null;
        return cluster.getInstances().stream()
                .filter(i -> i.getName().equals(instanceName))
                .findFirst()
                .map(ClusterInstance::getId)
                .orElseThrow(() -> new IllegalArgumentException("Instance '" + instanceName + "' not found in cluster '" + cluster.getName() + "'"));
    }

    public String getAccount() {
        try {
            GetAccountResponse response = accountClient.getAccount(GetAccountRequest.newBuilder().build());
            logDebug("Received response: " + response);
            return response.getUser();
        } catch (StatusException e) {
            if (e.getStatus().getCode() == Status.Code.UNIMPLEMENTED) {
                throw new IllegalStateException("This Matriarch server does not support account management (only local anonymous user). Please connect to StackGres Cloud.");
            }
            throw handleStatusException(e);
        }
    }

    public List<Slony> listSlonys() {
        return listSlonys(Map.of());
    }

    public List<Slony> listSlonys(Map<String, String> tags) {
        try {
            ListSlonysResponse response = resourceClient.listSlonys(ListSlonysRequest.newBuilder().putAllTags(tags).build());
            logDebug("Received response: " + response);
            return Mappers.mapSlonys(response.getSlonyList());
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public Map<String, String> addNodeTags(UUID slonyId, Map<String, String> tags) {
        AddNodeTagsRequest request = AddNodeTagsRequest.newBuilder().setId(mapUUID(slonyId)).putAllTags(tags).build();
        try {
            AddNodeTagsResponse response = resourceClient.addNodeTags(request);
            checkActionStatus(response.getStatus());
            return response.getCurrentTagsMap();
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public Map<String, String> removeNodeTags(UUID slonyId, List<String> keys) {
        RemoveNodeTagsRequest request = RemoveNodeTagsRequest.newBuilder().setId(mapUUID(slonyId)).addAllKey(keys).build();
        try {
            RemoveNodeTagsResponse response = resourceClient.removeNodeTags(request);
            checkActionStatus(response.getStatus());
            return response.getCurrentTagsMap();
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public void deleteSlony(UUID slonyId, Consumer<String> deletionConsumer) {
        DeleteSlonyRequest request = DeleteSlonyRequest.newBuilder().setId(mapUUID(slonyId)).build();
        BlockingClientCall<?, DeleteSlonyResponse> invocation = resourceClient.deleteSlony(request);
        try {
            while (invocation.hasNext()) {
                DeleteSlonyResponse response = invocation.read();
                logDebug("Received response: " + response);
                if (response.hasDeleted()) {
                    deletionConsumer.accept(response.getDeleted().getHostname());
                } else if (response.hasStatus()) {
                    throw new RuntimeException(response.getStatus().getMessage());
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public List<Slon> listSlons() {
        try {
            ListSlonsResponse response = resourceClient.listSlons(ListSlonsRequest.newBuilder().build());
            logDebug("Received response: " + response);
            return Mappers.mapSlons(response.getSlonList());
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public List<Event> getClusterEvents(String name) {
        PostgresCluster cluster = getCluster(name);   // resolve name → id (api.v1 events are by-id)
        var request = io.stackgres.proto.api.v1.GetClusterEventsRequest.newBuilder()
                .setEnvironmentId("local")
                .setClusterId(Id.newBuilder().setValue(cluster.getId().toString()))
                .build();
        try {
            var response = stackGresClient.getClusterEvents(request);
            logDebug("Received response: " + response);
            return response.getEventList().stream().map(Mappers::mapEventV1).toList();
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public List<Event> getNodeEvents(UUID slonyId) {
        GetNodeEventsRequest request = GetNodeEventsRequest.newBuilder().setSlonyId(mapUUID(slonyId)).build();
        try {
            GetNodeEventsResponse response = resourceClient.getNodeEvents(request);
            logDebug("Received response: " + response);
            if (response.hasStatus())
                throw new RuntimeException(response.getStatus().getMessage());
            return response.getEvents().getEventList();
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public String getNodeLogs(UUID slonyId) {
        GetNodeLogsRequest request = GetNodeLogsRequest.newBuilder().setSlonyId(mapUUID(slonyId)).build();
        try {
            GetNodeLogsResponse response = resourceClient.getNodeLogs(request);
            logDebug("Received response: " + response);
            if (response.hasLogs())
                return response.getLogs();
            else if (response.hasStatus())
                throw new RuntimeException(response.getStatus().getMessage());
            throw new IllegalStateException("No response sent");
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public Runnable followNodeLogs(UUID slonyId, Consumer<String> consumer, Consumer<String> onCompleted) {
        TailNodeLogsRequest request = TailNodeLogsRequest.newBuilder().setSlonyId(mapUUID(slonyId)).build();
        StreamObserver<TailNodeLogsResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(TailNodeLogsResponse response) {
                logDebug("Received response: " + response);
                if (response.hasStatus())
                    onCompleted.accept(response.getStatus().getMessage());
                else if (response.hasLine())
                    consumer.accept(response.getLine());
            }

            @Override
            public void onError(Throwable t) {
                logDebug("received error from server " + t);
                onCompleted.accept(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
            }

            @Override
            public void onCompleted() {
                logDebug("received close stream from server");
                onCompleted.accept(null);
            }
        };
        StreamObserver<TailNodeLogsRequest> requestObserver = asyncResourceClient.tailNodeLogs(responseObserver);
        requestObserver.onNext(request);
        return requestObserver::onCompleted;
    }

    public ClusterDiagnostics getClusterDiagnostics(String name) {
        GetClusterDiagnosticsRequest request = GetClusterDiagnosticsRequest.newBuilder().setName(name).build();
        try {
            GetClusterDiagnosticsResponse response = clusterClient.getClusterDiagnostics(request);
            logDebug("Received response: " + response);
            if (response.hasStatus())
                throw new RuntimeException(response.getStatus().getMessage());
            return Mappers.mapClusterDiagnostics(response.getDiagnostics());
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    private RuntimeException handleStatusException(StatusException e) {
        if (e.getStatus().getCode() == Status.Code.UNAVAILABLE) {
            String message = e.getCause() != null ? "Error: " + e.getCause().getMessage() : "";
            throw new IllegalStateException("Count not connect to the Matriarch server. " + message, e);
        }
        throw new RuntimeException(e);
    }

    public void setDebug() {
        debug = true;
    }

    public void setDebug(ProgressMessages messages) {
        debug = true;
        debugMessages = messages;
    }

    private void logDebug(String string) {
        if (!debug) return;
        if (debugMessages != null)
            debugMessages.add(commentAnsi(string));
        else
            System.err.println(commentAnsi(string));
    }

    public List<io.stackgres.postgres.Extension> listAvailableExtensions(io.stackgres.postgres.Flavor flavor, String version) {
        try {
            var response = stackGresClient.listExtensions(io.stackgres.proto.api.v1.ListExtensionsRequest.newBuilder()
                    .setEngine(Mappers.mapEngine(flavor))
                    .setVersion(version)
                    .build());
            logDebug("Received response: " + response);
            return response.getExtensionList().stream()
                    .map(e -> new io.stackgres.postgres.Extension(e.getName(),
                            e.getVersion().isBlank() ? null : e.getVersion(),
                            e.getRevision().isBlank() ? null : e.getRevision()))
                    .toList();
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

}