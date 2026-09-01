package io.stackgres.cli.client;

import com.google.protobuf.ByteString;
import io.grpc.*;
import io.grpc.stub.BlockingClientCall;
import io.grpc.stub.StreamObserver;
import io.stackgres.cli.CliContext;
import io.stackgres.cli.Strings;
import io.stackgres.cli.config.ResolvedContext;
import io.stackgres.cli.commands.ProgressMessages;
import io.stackgres.cli.postgres.ClusterDiagnostics;
import io.stackgres.cli.postgres.ClusterRow;
import io.stackgres.cli.postgres.EnvironmentInfo;
import io.stackgres.cli.postgres.Slon;
import io.stackgres.cli.postgres.Slony;
import io.stackgres.postgres.ClusterInstance;
import io.stackgres.postgres.PostgresCluster;
import io.stackgres.proto.api.v1.*;
import io.stackgres.proto.api.v1.CreateClusterRequest;
import io.stackgres.proto.api.v1.DeleteClusterRequest;
import io.stackgres.proto.api.v1.ListClustersRequest;
import io.stackgres.proto.api.v1.ListVersionsRequest;
import io.stackgres.proto.api.v1.RestartClusterRequest;
import io.stackgres.proto.api.v1.StackGresApiGrpc.StackGresApiBlockingStub;
import io.stackgres.proto.api.v1.StartClusterRequest;
import io.stackgres.proto.api.v1.StopClusterRequest;
import io.stackgres.proto.cli.*;
import io.stackgres.proto.cli.GetClusterDiagnosticsRequest;
import io.stackgres.proto.cli.GetClusterDiagnosticsResponse;
import io.stackgres.proto.types.v1.Id;

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

    private ManagedChannel channel;
    private ClusterServiceGrpc.ClusterServiceBlockingV2Stub clusterStub;
    private ClusterServiceGrpc.ClusterServiceStub clusterAsyncStub;
    private AccountServiceGrpc.AccountServiceBlockingV2Stub accountStub;
    private ResourceServiceGrpc.ResourceServiceBlockingV2Stub resourceStub;
    private ResourceServiceGrpc.ResourceServiceStub resourceAsyncStub;
    // stackgres.api.v1 — cluster CRUD + log tail talk this; the rest still use the cli.proto stubs above.
    private StackGresApiBlockingStub stackGresStub;
    private io.stackgres.proto.api.v1.StackGresApiGrpc.StackGresApiStub stackGresAsyncStub;

    private String matriarchUrl;
    private boolean debug;
    private ProgressMessages debugMessages;

    public MatriarchClient() {
        // Cheap on purpose: the channel and stubs open lazily on first use (ensureConnected), AFTER
        // picocli has injected the global --context/--endpoint/--token/-E options, so the resolved
        // target (flag > env > context file > default) is the one actually dialed.
    }

    /** Resolve the target once ({@link CliContext#resolve()}) and open the gRPC channel + stubs. */
    private synchronized void ensureConnected() {
        if (channel != null) {
            return;
        }
        ResolvedContext ctx = CliContext.resolve();
        this.matriarchUrl = ctx.endpoint();
        ChannelCredentials channelCredentials = ctx.tls() ? TlsChannelCredentials.create() : InsecureChannelCredentials.create();
        this.channel = Grpc.newChannelBuilder(ctx.endpoint(), channelCredentials).build();
        JwtCredential credential = ctx.token() != null ? new JwtCredential(ctx.token()) : null;
        clusterStub = ClusterServiceGrpc.newBlockingV2Stub(channel).withCallCredentials(credential);
        clusterAsyncStub = ClusterServiceGrpc.newStub(channel).withCallCredentials(credential);
        accountStub = AccountServiceGrpc.newBlockingV2Stub(channel).withCallCredentials(credential);
        resourceStub = ResourceServiceGrpc.newBlockingV2Stub(channel).withCallCredentials(credential);
        resourceAsyncStub = ResourceServiceGrpc.newStub(channel).withCallCredentials(credential);
        stackGresStub = StackGresApiGrpc.newBlockingStub(channel).withCallCredentials(credential);
        stackGresAsyncStub = StackGresApiGrpc.newStub(channel).withCallCredentials(credential);
    }

    private StackGresApiBlockingStub stackGresClient() {
        ensureConnected();
        return stackGresStub;
    }

    private io.stackgres.proto.api.v1.StackGresApiGrpc.StackGresApiStub asyncStackGresClient() {
        ensureConnected();
        return stackGresAsyncStub;
    }

    private ClusterServiceGrpc.ClusterServiceBlockingV2Stub clusterClient() {
        ensureConnected();
        return clusterStub;
    }

    private ClusterServiceGrpc.ClusterServiceStub asyncClient() {
        ensureConnected();
        return clusterAsyncStub;
    }

    private AccountServiceGrpc.AccountServiceBlockingV2Stub accountClient() {
        ensureConnected();
        return accountStub;
    }

    private ResourceServiceGrpc.ResourceServiceBlockingV2Stub resourceClient() {
        ensureConnected();
        return resourceStub;
    }

    private ResourceServiceGrpc.ResourceServiceStub asyncResourceClient() {
        ensureConnected();
        return resourceAsyncStub;
    }

    // The cloud (skvorets) serves only api.v1; commands still on the old cli.proto services (psql, tunnel,
    // exec, node writes/logs/events, checkpoints, diagnostics, slon) come back UNIMPLEMENTED — or, behind
    // Cloudflare, an HTTP 500 with no gRPC content-type. Recognise that so we say so cleanly, not a raw 500.
    private static final String CLOUD_UNSUPPORTED =
            "This command isn't available over the cloud connection yet — it needs a direct connection to a local "
            + "matriarch (point STACKGRES_ENDPOINT_URL at the matriarch, or use a local context).";

    static boolean isCloudUnsupported(Throwable t) {
        io.grpc.Status s = io.grpc.Status.fromThrowable(t);
        if (s.getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
            return true;
        }
        String msg = t.getMessage() == null ? "" : t.getMessage();
        return s.getCode() == io.grpc.Status.Code.UNKNOWN
                && (msg.contains("invalid content-type") || msg.contains("HTTP status code 5"));
    }

    private RuntimeException statusError(StatusRuntimeException e) {
        if (isCloudUnsupported(e)) {
            return new IllegalStateException(CLOUD_UNSUPPORTED);
        }
        String detail = e.getStatus().getDescription() != null ? e.getStatus().getDescription() : e.getMessage();
        if (e.getStatus().getCode() == io.grpc.Status.Code.UNAVAILABLE) {
            return new RuntimeException("cannot reach the matriarch at " + matriarchUrl + " — is it running? Set STACKGRES_ENDPOINT_URL (default localhost:50051; the api.v1 matriarch serves 9000). [" + detail + "]");
        }
        return new RuntimeException(detail);
    }

    public void createCluster(PostgresCluster cluster, Map<String, String> nodeSelector, Consumer<ClusterCreationUpdate> statusConsumer) {
        // Create is scoped to the active environment (required — we won't guess where to place it).
        CreateClusterRequest request = Mappers.createClusterRequest(cluster).toBuilder()
                .setEnvironmentId(activeEnvironment()).build();
        logDebug("Creating cluster: ", request);

        boolean announced = false;
        boolean succeeded = false;
        boolean pending = false;
        String lastId = "";
        String lastName = cluster.getName() == null ? "" : cluster.getName();
        try {
            Iterator<ClusterOperationProgress> stream = stackGresClient().createCluster(request);
            while (stream.hasNext()) {
                ClusterOperationProgress p = stream.next();
                logDebug("Received progress: ", p);
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
                .setEnvironmentId(activeEnvironment())
                .setClusterId(Id.newBuilder().setValue(clusterId))
                .build();
        try {
            return stackGresClient().getClusterCredentials(request).getPassword();
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public void deleteCluster(String clusterName, Consumer<String> deletionConsumer) {
        PostgresCluster cluster = getCluster(clusterName);
        deleteById(cluster.getId().toString(), clusterName, deletionConsumer);
    }

    public void deleteAllClusters(Consumer<String> deletionConsumer) {
        for (PostgresCluster cluster : listActiveClusters(Map.of()))
            deleteById(cluster.getId().toString(), cluster.getName(), deletionConsumer);
    }

    public void deleteClusters(Map<String, String> tags, Consumer<String> deletionConsumer) {
        for (PostgresCluster cluster : listActiveClusters(tags))
            deleteById(cluster.getId().toString(), cluster.getName(), deletionConsumer);
    }

    // api.v1 delete is by-id; the CLI resolves the name/tags → id(s) client-side above.
    private void deleteById(String id, String name, Consumer<String> deletionConsumer) {
        DeleteClusterRequest request = DeleteClusterRequest.newBuilder()
                .setSelector(io.stackgres.proto.api.v1.ClusterSelector.newBuilder()
                        .setEnvironmentId(activeEnvironment())
                        .setId(Id.newBuilder().setValue(id)))
                .setIdempotencyKey(id)
                .build();
        try {
            Iterator<ClusterOperationProgress> stream = stackGresClient().deleteCluster(request);
            while (stream.hasNext()) {
                ClusterOperationProgress p = stream.next();
                logDebug("Received progress: ", p);
                if (p.getStatus() == OperationStatus.OPERATION_STATUS_FAILED) {
                    throw new RuntimeException(p.getError().getMessage());
                }
            }
            // The server closed the stream without a failure. A SUCCEEDED frame is the normal case, but an
            // idempotent replay of an already-accepted delete completes with only an ACCEPTED frame — treat
            // any clean completion as done so the spinner always resolves (never left spinning on exit).
            deletionConsumer.accept(name);
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public void startCluster(String clusterName) {
        lifecycle(LifecycleVerb.START, getCluster(clusterName).getId().toString());
    }

    public void startAllClusters() {
        for (PostgresCluster c : listActiveClusters(Map.of())) if (!c.isRunning()) lifecycle(LifecycleVerb.START, c.getId().toString());
    }

    public void startClusters(Map<String, String> tags) {
        for (PostgresCluster c : listActiveClusters(tags)) if (!c.isRunning()) lifecycle(LifecycleVerb.START, c.getId().toString());
    }

    public void stopCluster(String clusterName) {
        lifecycle(LifecycleVerb.STOP, getCluster(clusterName).getId().toString());
    }

    public void stopAllClusters() {
        for (PostgresCluster c : listActiveClusters(Map.of())) if (c.isRunning()) lifecycle(LifecycleVerb.STOP, c.getId().toString());
    }

    public void stopClusters(Map<String, String> tags) {
        for (PostgresCluster c : listActiveClusters(tags)) if (c.isRunning()) lifecycle(LifecycleVerb.STOP, c.getId().toString());
    }

    public void restartCluster(String clusterName) {
        lifecycle(LifecycleVerb.RESTART, getCluster(clusterName).getId().toString());
    }

    public void restartAllClusters() {
        for (PostgresCluster c : listActiveClusters(Map.of())) lifecycle(LifecycleVerb.RESTART, c.getId().toString());
    }

    public void restartClusters(Map<String, String> tags) {
        for (PostgresCluster c : listActiveClusters(tags)) lifecycle(LifecycleVerb.RESTART, c.getId().toString());
    }

    private enum LifecycleVerb {START, STOP, RESTART}

    // api.v1 lifecycle is by-id (name/tags resolved client-side); streams progress to a terminal frame.
    private void lifecycle(LifecycleVerb verb, String id) {
        var selector = io.stackgres.proto.api.v1.ClusterSelector.newBuilder()
                .setEnvironmentId(activeEnvironment())
                .setId(Id.newBuilder().setValue(id))
                .build();
        String key = UUID.randomUUID().toString();
        try {
            Iterator<ClusterOperationProgress> stream = switch (verb) {
                case START -> stackGresClient().startCluster(StartClusterRequest.newBuilder().setSelector(selector).setIdempotencyKey(key).build());
                case STOP -> stackGresClient().stopCluster(StopClusterRequest.newBuilder().setSelector(selector).setIdempotencyKey(key).build());
                case RESTART -> stackGresClient().restartCluster(RestartClusterRequest.newBuilder().setSelector(selector).setIdempotencyKey(key).build());
            };
            while (stream.hasNext()) {
                ClusterOperationProgress p = stream.next();
                logDebug("Received progress: ", p);
                if (p.getStatus() == OperationStatus.OPERATION_STATUS_FAILED) {
                    throw new RuntimeException(p.getError().getMessage());
                }
            }
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    private void checkActionStatus(com.google.rpc.Status status) {
        logDebug("Received response: ", status);
        if (status.getCode() != com.google.rpc.Code.OK.getNumber())
            throw new RuntimeException(status.getMessage());
    }

    // ---- environment resolution (kubectl-namespace model) ----

    private String activeEnvironmentCache;

    /** The environment configured for this invocation (flag &gt; env var &gt; context); "" = unset (all). */
    public String configuredEnvironment() {
        return CliContext.environment();
    }

    /**
     * The active environment for a single-cluster or mutating op: the configured one if set, else the
     * sole environment the endpoint exposes (a local matriarch, or a single-environment cloud). If the
     * endpoint exposes several and none is chosen, fail with guidance — we will not guess which
     * environment to create in, or which same-named cluster to act on.
     */
    public String activeEnvironment() {
        if (activeEnvironmentCache != null) {
            return activeEnvironmentCache;
        }
        String configured = configuredEnvironment();
        if (configured != null && !configured.isBlank()) {
            warnIfEnvironmentStale(configured);   // D: warn (don't fail) when the pinned env is stale
            return activeEnvironmentCache = configured;
        }
        List<EnvironmentInfo> envs = listEnvironments();
        if (envs.size() == 1) {
            return activeEnvironmentCache = envs.get(0).id();
        }
        if (envs.isEmpty()) {
            throw new RuntimeException("no environments are available on this endpoint");
        }
        String ids = envs.stream().map(EnvironmentInfo::id).collect(java.util.stream.Collectors.joining(", "));
        throw new RuntimeException("no active environment selected — this endpoint exposes several ("
                + ids + "). Choose one with 'stackgres environment use <id>' or pass -E <id>.");
    }

    /**
     * Warn (warm amber, on stderr) when {@code environmentId} is disconnected or gone, suggesting the
     * connected environment(s) to switch to. Best-effort: never fails the command it guards.
     */
    public void warnIfEnvironmentStale(String environmentId) {
        if (environmentId == null || environmentId.isBlank()) {
            return;
        }
        try {
            warnIfEnvironmentStale(environmentId, listEnvironments());
        } catch (RuntimeException ignore) {
            // best-effort — a staleness hint must never block the command
        }
    }

    /** As {@link #warnIfEnvironmentStale(String)}, reusing an already-fetched environment list. */
    public void warnIfEnvironmentStale(String environmentId, List<EnvironmentInfo> environments) {
        if (environmentId == null || environmentId.isBlank()) {
            return;
        }
        EnvironmentInfo current = environments.stream()
                .filter(e -> environmentId.equals(e.id())).findFirst().orElse(null);
        String problem;
        if (current == null) {
            problem = "Environment '" + environmentId + "' no longer exists.";
        } else if ("Disconnected".equalsIgnoreCase(current.health())) {
            problem = "Environment '" + environmentId + "' is disconnected.";
        } else {
            return; // connected — nothing to warn about
        }
        List<String> connected = environments.stream()
                .filter(e -> "Connected".equalsIgnoreCase(e.health()))
                .map(EnvironmentInfo::id).toList();
        String hint;
        if (connected.size() == 1) {
            hint = " Did you mean '" + connected.get(0) + "'?  Switch with: stackgres environment use " + connected.get(0);
        } else if (!connected.isEmpty()) {
            hint = " Connected: " + String.join(", ", connected) + ".  Switch with: stackgres environment use <id>";
        } else {
            hint = "";
        }
        System.err.println(Strings.warnAnsi(problem + hint));
    }

    /** Rows for {@code cluster list}: {@code environmentId} = "" means all environments (aggregated). */
    public List<ClusterRow> listClusterRows(String environmentId, Map<String, String> tags) {
        ListClustersRequest request = ListClustersRequest.newBuilder()
                .setEnvironmentId(environmentId == null ? "" : environmentId).putAllTags(tags).build();
        try {
            io.stackgres.proto.api.v1.ListClustersResponse response = stackGresClient().listClusters(request);
            logDebug("Received response: ", response);
            List<ClusterRow> rows = new java.util.ArrayList<>();
            for (io.stackgres.proto.api.v1.Cluster c : response.getClusterList()) {
                rows.add(new ClusterRow(c.getEnvironmentId(), Mappers.mapCluster(c)));
            }
            return rows;
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    /** Clusters within the ACTIVE environment — for name resolution and bulk mutations. */
    private List<PostgresCluster> listActiveClusters(Map<String, String> tags) {
        return listClusterRows(activeEnvironment(), tags).stream().map(ClusterRow::cluster).toList();
    }

    public List<EnvironmentInfo> listEnvironments() {
        try {
            io.stackgres.proto.api.v1.ListEnvironmentsResponse response = stackGresClient().listEnvironments(ListEnvironmentsRequest.newBuilder().build());
            logDebug("Received response: ", response);
            List<EnvironmentInfo> out = new java.util.ArrayList<>();
            for (io.stackgres.proto.api.v1.Environment env : response.getEnvironmentList()) {
                out.add(mapEnvironment(env, response.getSourceInfoMap().get(env.getId().getValue())));
            }
            return out;
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public void deleteEnvironment(String environmentId) {
        try {
            stackGresClient().deleteEnvironment(io.stackgres.proto.api.v1.DeleteEnvironmentRequest.newBuilder()
                    .setEnvironmentId(environmentId).build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
                throw new RuntimeException("this endpoint does not support deleting environments "
                        + "(a local matriarch is its own single environment — connect to the cloud)");
            }
            throw statusError(e);
        }
    }

    public EnvironmentInfo getEnvironment(String environmentId) {
        try {
            GetEnvironmentResponse response = stackGresClient().getEnvironment(GetEnvironmentRequest.newBuilder().setEnvironmentId(environmentId).build());
            logDebug("Received response: ", response);
            return mapEnvironment(response.getEnvironment(), response.getSourceInfo());
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    private static EnvironmentInfo mapEnvironment(io.stackgres.proto.api.v1.Environment env, io.stackgres.proto.types.v1.SourceInfo si) {
        String kind = kindName(env.getKind());
        String source = si == null ? "" : sourceName(si.getSource());
        String health = si == null ? "" : healthName(si.getEnvironmentHealth());
        Instant asOf = (si != null && si.hasAsOf()) ? Instant.ofEpochSecond(si.getAsOf().getSeconds(), si.getAsOf().getNanos()) : null;
        List<String> surfaces = env.getSurfaceList().stream().map(s -> titleCase(s.name().replace("API_SURFACE_", ""))).toList();
        return new EnvironmentInfo(env.getId().getValue(), kind, source, health, asOf, surfaces);
    }

    // Render the api.v1 enums as human-friendly names for display (Bare Metal, K8s, Live, Connected, ...).
    private static String kindName(io.stackgres.proto.api.v1.Environment.Kind kind) {
        return switch (kind) {
            case KIND_BARE_METAL -> "Bare Metal";
            case KIND_K8S_STACKGRES -> "K8s (StackGres)";
            case KIND_K8S_NATIVE -> "K8s (Native)";
            case KIND_EXTERNAL -> "External";
            default -> "Unknown";
        };
    }

    private static String sourceName(io.stackgres.proto.types.v1.SourceInfo.Source source) {
        return switch (source) {
            case LIVE -> "Live";
            case CACHED -> "Cached";
            default -> "Unknown";
        };
    }

    private static String healthName(io.stackgres.proto.types.v1.SourceInfo.EnvironmentHealth health) {
        return switch (health) {
            case CONNECTED -> "Connected";
            case DEGRADED -> "Degraded";
            case DISCONNECTED -> "Disconnected";
            default -> "Unknown";
        };
    }

    // "CLUSTER_LIFECYCLE" -> "Cluster Lifecycle", "EVENTS" -> "Events".
    private static String titleCase(String upperSnake) {
        StringBuilder sb = new StringBuilder();
        for (String part : upperSnake.split("_")) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public List<String> listAvailableVersions(io.stackgres.postgres.Flavor flavor) {
        try {
            var response = stackGresClient().listVersions(ListVersionsRequest.newBuilder().setEngine(Mappers.mapEngine(flavor)).build());
            logDebug("Received response: ", response);
            return response.getVersionList();
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public PostgresCluster getCluster(String name) {
        // api.v1 GetCluster is by-id in the matriarch; resolve by name client-side via list.
        return listActiveClusters(Map.of()).stream()
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

        StreamObserver<CliExecMessage> cliExecObserver = asyncClient().execInCluster(new StreamObserver<>() {
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
                exitCode.completeExceptionally(isCloudUnsupported(t) ? new IllegalStateException(CLOUD_UNSUPPORTED) : t);
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

    // api.v1 TailLogs (component logs, served from the cluster's slon(s), fanned into one LogLine stream):
    // follow=false streams a recent snapshot then completes; follow=true streams new lines until cancelled.
    public String getClusterLogs(String name, String instanceName, String component) {
        StringBuilder block = new StringBuilder();
        try {
            Iterator<LogLine> stream = stackGresClient().tailLogs(tailLogsRequest(name, instanceName, component, false));
            while (stream.hasNext()) {
                block.append(stream.next().getLine()).append('\n');
            }
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
        return block.toString();
    }

    public Runnable followClusterLogs(String name, String instanceName, String component, Consumer<String> consumer, Consumer<String> onCompleted) {
        TailLogsRequest request = tailLogsRequest(name, instanceName, component, true);
        // Cancel the server-stream by cancelling this context (the CLI's shutdown hook runs the returned Runnable).
        io.grpc.Context.CancellableContext ctx = io.grpc.Context.current().withCancellation();
        StreamObserver<LogLine> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(LogLine line) {
                consumer.accept(line.getLine());
            }

            @Override
            public void onError(Throwable t) {
                logDebug("received error from server " + t);
                Status.Code code = t instanceof StatusRuntimeException e ? e.getStatus().getCode() : Status.Code.UNKNOWN;
                if (code == Status.Code.CANCELLED) {
                    onCompleted.accept(null);   // our own cancel (Ctrl-C) — a clean end
                } else if (code == Status.Code.UNIMPLEMENTED) {
                    onCompleted.accept("This environment does not support log tail.");
                } else {
                    onCompleted.accept(t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
                }
            }

            @Override
            public void onCompleted() {
                logDebug("received close stream from server");
                onCompleted.accept(null);
            }
        };
        ctx.run(() -> asyncStackGresClient().tailLogs(request, responseObserver));
        return () -> ctx.cancel(null);
    }

    // Resolve name -> cluster (+ optional instance name -> id) and build the api.v1 TailLogs request.
    private TailLogsRequest tailLogsRequest(String name, String instanceName, String component, boolean follow) {
        PostgresCluster cluster = getCluster(name);
        UUID instanceId = instanceId(cluster, instanceName);
        TailLogsRequest.Builder builder = TailLogsRequest.newBuilder()
                .setEnvironmentId(activeEnvironment())
                .setClusterId(Id.newBuilder().setValue(cluster.getId().toString()))
                .addComponent(componentName(component, cluster))
                .setFollow(follow);
        if (instanceId != null) {
            builder.setInstanceId(Id.newBuilder().setValue(instanceId.toString()));
        }
        return builder.build();
    }

    private static String componentName(String component, PostgresCluster cluster) {
        if (component == null || component.isBlank()) {
            return cluster.isStandalone() ? "postgres" : "patroni";
        }
        return switch (component.toLowerCase()) {
            case "postgres", "patroni", "slon", "etcd" -> component.toLowerCase();
            default -> throw new IllegalArgumentException(
                    "Unknown log component: " + component + ". Valid values: postgres, patroni, slon, etcd");
        };
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
            GetClusterCheckpointsResponse response = clusterClient().getClusterCheckpoints(builder.build());
            logDebug("Received response: ", response);
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

    public PgWireTunnel openPgWireTunnel(PostgresCluster cluster, String instanceName, PgWireTarget target, boolean warnOnRandomInstance,
                                         Consumer<byte[]> dataConsumer, Runnable onClosed, Consumer<String> onAborted, Consumer<String> onError) {
        UUID instanceId = instanceId(cluster, instanceName);
        if (!cluster.isStandalone() && instanceId == null && warnOnRandomInstance) {
            System.err.println(Strings.commentAnsi("No instance name is given, selecting random cluster instance"));
        }

        CompletableFuture<Boolean> opened = new CompletableFuture<>();

        StreamObserver<PgWireClientMessage> requestObserver = asyncClient().pgWireTunnel(new StreamObserver<>() {
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
                Throwable err = isCloudUnsupported(t) ? new IllegalStateException(CLOUD_UNSUPPORTED) : t;
                if (!opened.isDone())
                    opened.completeExceptionally(err);
                else
                    onError.accept(err.getMessage());
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
            GetAccountResponse response = accountClient().getAccount(GetAccountRequest.newBuilder().build());
            logDebug("Received response: ", response);
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
            ListNodesResponse response = stackGresClient().listNodes(
                    ListNodesRequest.newBuilder().setEnvironmentId(configuredEnvironment()).putAllTags(tags).build());
            logDebug("Received response: ", response);
            return Mappers.mapNodes(response.getNodeList());
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public Map<String, String> addNodeTags(UUID slonyId, Map<String, String> tags) {
        AddNodeTagsRequest request = AddNodeTagsRequest.newBuilder().setId(mapUUID(slonyId)).putAllTags(tags).build();
        try {
            AddNodeTagsResponse response = resourceClient().addNodeTags(request);
            checkActionStatus(response.getStatus());
            return response.getCurrentTagsMap();
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public Map<String, String> removeNodeTags(UUID slonyId, List<String> keys) {
        RemoveNodeTagsRequest request = RemoveNodeTagsRequest.newBuilder().setId(mapUUID(slonyId)).addAllKey(keys).build();
        try {
            RemoveNodeTagsResponse response = resourceClient().removeNodeTags(request);
            checkActionStatus(response.getStatus());
            return response.getCurrentTagsMap();
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public void deleteSlony(UUID slonyId, Consumer<String> deletionConsumer) {
        DeleteSlonyRequest request = DeleteSlonyRequest.newBuilder().setId(mapUUID(slonyId)).build();
        BlockingClientCall<?, DeleteSlonyResponse> invocation = resourceClient().deleteSlony(request);
        try {
            while (invocation.hasNext()) {
                DeleteSlonyResponse response = invocation.read();
                logDebug("Received response: ", response);
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
            ListSlonsResponse response = resourceClient().listSlons(ListSlonsRequest.newBuilder().build());
            logDebug("Received response: ", response);
            return Mappers.mapSlons(response.getSlonList());
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    public List<Event> getClusterEvents(String name) {
        PostgresCluster cluster = getCluster(name);   // resolve name → id (api.v1 events are by-id)
        var request = io.stackgres.proto.api.v1.GetClusterEventsRequest.newBuilder()
                .setEnvironmentId(activeEnvironment())
                .setClusterId(Id.newBuilder().setValue(cluster.getId().toString()))
                .build();
        try {
            var response = stackGresClient().getClusterEvents(request);
            logDebug("Received response: ", response);
            return response.getEventList().stream().map(Mappers::mapEventV1).toList();
        } catch (StatusRuntimeException e) {
            throw statusError(e);
        }
    }

    public List<Event> getNodeEvents(UUID slonyId) {
        GetNodeEventsRequest request = GetNodeEventsRequest.newBuilder().setSlonyId(mapUUID(slonyId)).build();
        try {
            GetNodeEventsResponse response = resourceClient().getNodeEvents(request);
            logDebug("Received response: ", response);
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
            GetNodeLogsResponse response = resourceClient().getNodeLogs(request);
            logDebug("Received response: ", response);
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
                logDebug("Received response: ", response);
                if (response.hasStatus())
                    onCompleted.accept(response.getStatus().getMessage());
                else if (response.hasLine())
                    consumer.accept(response.getLine());
            }

            @Override
            public void onError(Throwable t) {
                logDebug("received error from server " + t);
                onCompleted.accept(isCloudUnsupported(t) ? CLOUD_UNSUPPORTED
                        : (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName()));
            }

            @Override
            public void onCompleted() {
                logDebug("received close stream from server");
                onCompleted.accept(null);
            }
        };
        StreamObserver<TailNodeLogsRequest> requestObserver = asyncResourceClient().tailNodeLogs(responseObserver);
        requestObserver.onNext(request);
        return requestObserver::onCompleted;
    }

    public ClusterDiagnostics getClusterDiagnostics(String name) {
        GetClusterDiagnosticsRequest request = GetClusterDiagnosticsRequest.newBuilder().setName(name).build();
        try {
            GetClusterDiagnosticsResponse response = clusterClient().getClusterDiagnostics(request);
            logDebug("Received response: ", response);
            if (response.hasStatus())
                throw new RuntimeException(response.getStatus().getMessage());
            return Mappers.mapClusterDiagnostics(response.getDiagnostics());
        } catch (StatusException e) {
            throw handleStatusException(e);
        }
    }

    private RuntimeException handleStatusException(StatusException e) {
        if (isCloudUnsupported(e)) {
            throw new IllegalStateException(CLOUD_UNSUPPORTED);
        }
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

    // Log a protobuf message WITHOUT calling its toString(): TextFormat's debug-redaction path reflects
    // DescriptorProtos$FieldOptions (getCtype/...), which crashes in the native image. We log the message
    // type only — same approach the slon/slony agents use to stay native-safe.
    private void logDebug(String prefix, com.google.protobuf.MessageLite message) {
        if (!debug) return;
        logDebug(prefix + (message == null ? "null" : message.getClass().getSimpleName()));
    }

    public List<io.stackgres.postgres.Extension> listAvailableExtensions(io.stackgres.postgres.Flavor flavor, String version) {
        try {
            var response = stackGresClient().listExtensions(io.stackgres.proto.api.v1.ListExtensionsRequest.newBuilder()
                    .setEngine(Mappers.mapEngine(flavor))
                    .setVersion(version)
                    .build());
            logDebug("Received response: ", response);
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