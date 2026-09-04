package io.stackgres.matriarch.quarkus.api;

import io.stackgres.matriarch.Matriarch;
import io.stackgres.matriarch.ProgressSink;
import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.*;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.spec.ClusterCreate;
import io.stackgres.matriarch.model.spec.DatabaseEngine;
import io.stackgres.matriarch.model.spec.Extension;
import io.stackgres.matriarch.quarkus.event.ClusterEventStore;
import io.stackgres.matriarch.spi.ExtensionCatalog;
import io.stackgres.matriarch.spi.VersionCatalog;
import io.stackgres.proto.api.v1.*;
import io.stackgres.proto.api.v1.ClusterOperationProgress;
import io.stackgres.proto.api.v1.postgres.Extension.Builder;
import io.grpc.Status;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import io.stackgres.matriarch.model.spec.InstanceSpec;
import io.stackgres.matriarch.quarkus.grpc.ClusterLogRelay;
import io.stackgres.matriarch.quarkus.resources.Slons;
import io.stackgres.matriarch.quarkus.resources.Slonys;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


/**
 * The gRPC surface ({@code stackgres.api.v1.StackGresApi}) over the local matriarch core.
 * Implements create / list / get / delete; maps requests to the domain (via
 * {@link ProtoMapper}) and streams results back. All orchestration lives in the
 * core; all protobuf lives at this edge.
 */
@GrpcService
public class StackGresApiResource extends StackGresApiGrpc.StackGresApiImplBase {

    private static final Logger LOG = Logger.getLogger(StackGresApiResource.class);

    @Inject
    Matriarch matriarch;

    @Inject
    VersionCatalog versionCatalog;

    @Inject
    ExtensionCatalog extensionCatalog;

    @Inject
    ClusterEventStore clusterEvents;

    @Inject
    io.stackgres.matriarch.quarkus.identity.EnvironmentIdentity identity;

    // This matriarch's build version (from the Maven project version, baked in by Quarkus at build time).
    @org.eclipse.microprofile.config.inject.ConfigProperty(name = "quarkus.application.version", defaultValue = "dev")
    String serverVersion;

    @Inject
    Slons slons;

    @Inject
    Slonys slonys;

    @Inject
    ClusterLogRelay clusterLogRelay;

    @Override
    public void createCluster(CreateClusterRequest request, StreamObserver<ClusterOperationProgress> responseObserver) {
        ClusterCreate intent = ProtoMapper.toCreate(request);
        try {
            matriarch.createCluster(intent, request.getIdempotencyKey(), progressSink(responseObserver));
        } catch (ClusterNameInUseException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            // unknown/unavailable engine version or extension (DOCIR resolution) — a client error
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalStateException e) {
            // the version/extension catalog (DOCIR) is unreachable — not a transport failure
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // Component log tail (API_SURFACE_LOG_TAIL): stream one or more components' logs from the cluster's
    // slon(s), fanned into a single LogLine stream by ClusterLogRelay. Partition-safe (served from source
    // over the agent link); historical/analytic QueryLogs stays cloud-only and is not implemented here.
    @Override
    public void tailLogs(TailLogsRequest request, StreamObserver<LogLine> responseObserver) {
        Cluster cluster = matriarch.getCluster(new ClusterId(request.getClusterId().getValue()));
        if (cluster == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("no such cluster").asRuntimeException());
            return;
        }
        List<InstanceSpec> instances = cluster.spec().instances();
        if (request.hasInstanceId()) {
            String wanted = request.getInstanceId().getValue();
            instances = instances.stream().filter(i -> i.id().value().equals(wanted)).toList();
            if (instances.isEmpty()) {
                responseObserver.onError(Status.NOT_FOUND.withDescription("no such instance in cluster").asRuntimeException());
                return;
            }
        }
        // Empty component list = all; the agent answers "not available" for components a given mode lacks
        // (patroni/etcd on a standalone), which the relay silently drops.
        List<String> components = request.getComponentList().isEmpty()
                ? List.of("postgres", "patroni", "slon", "etcd")
                : request.getComponentList();
        boolean follow = request.getFollow();

        ClusterLogRelay.Session session = clusterLogRelay.newSession(responseObserver, follow);
        List<Map.Entry<UUID, UUID>> subs = new ArrayList<>();   // (instanceId, logId) — for abort on cancel
        for (InstanceSpec instance : instances) {
            UUID instanceId = UUID.fromString(instance.id().value());
            if (!slons.isConnected(instanceId)) {
                continue;   // this instance's slon isn't connected — nothing to tail from it
            }
            for (String component : components) {
                UUID logId = UUID.randomUUID();
                clusterLogRelay.register(logId, session, instance.id().value(), component);
                if (slons.requestLogs(instanceId, logId, component, follow)) {
                    subs.add(Map.entry(instanceId, logId));
                } else {
                    clusterLogRelay.fail(logId);
                }
            }
        }
        if (subs.isEmpty()) {
            clusterLogRelay.error(session, "no connected instance for this cluster");
            return;
        }
        // A follow ends when the client goes away: stop the slon tails and forget the session.
        if (responseObserver instanceof ServerCallStreamObserver<LogLine> cancellable) {
            cancellable.setOnCancelHandler(() -> {
                for (Map.Entry<UUID, UUID> sub : subs) {
                    slons.abortLogs(sub.getKey(), sub.getValue());
                }
                clusterLogRelay.drop(session);
            });
        }
        clusterLogRelay.arm(session);   // setup done — a snapshot may now complete
    }

    @Override
    public void deleteCluster(DeleteClusterRequest request, StreamObserver<ClusterOperationProgress> responseObserver) {
        ClusterSelector selector = request.getSelector();
        if (!selector.hasId()) {
            responseObserver.onError(Status.UNIMPLEMENTED.withDescription("delete is supported by cluster id only in this build").asRuntimeException());
            return;
        }
        ClusterId id = new ClusterId(selector.getId().getValue());
        matriarch.deleteCluster(id, request.getIdempotencyKey(), progressSink(responseObserver));
    }

    @Override
    public void startCluster(io.stackgres.proto.api.v1.StartClusterRequest request, StreamObserver<ClusterOperationProgress> responseObserver) {
        lifecycle(request.getSelector(), request.getIdempotencyKey(), responseObserver, LifecycleVerb.START);
    }

    @Override
    public void stopCluster(io.stackgres.proto.api.v1.StopClusterRequest request, StreamObserver<ClusterOperationProgress> responseObserver) {
        lifecycle(request.getSelector(), request.getIdempotencyKey(), responseObserver, LifecycleVerb.STOP);
    }

    @Override
    public void restartCluster(io.stackgres.proto.api.v1.RestartClusterRequest request, StreamObserver<ClusterOperationProgress> responseObserver) {
        lifecycle(request.getSelector(), request.getIdempotencyKey(), responseObserver, LifecycleVerb.RESTART);
    }

    private enum LifecycleVerb {START, STOP, RESTART}

    private void lifecycle(io.stackgres.proto.api.v1.ClusterSelector selector, String idempotencyKey, StreamObserver<ClusterOperationProgress> responseObserver, LifecycleVerb verb) {
        if (!selector.hasId()) {
            responseObserver.onError(Status.UNIMPLEMENTED.withDescription(
                    verb.name().toLowerCase() + " is supported by cluster id only in this build").asRuntimeException());
            return;
        }
        ClusterId id = new ClusterId(selector.getId().getValue());
        ProgressSink sink = progressSink(responseObserver);
        try {
            switch (verb) {
                case START -> matriarch.startCluster(id, idempotencyKey, sink);
                case STOP -> matriarch.stopCluster(id, idempotencyKey, sink);
                case RESTART -> matriarch.restartCluster(id, idempotencyKey, sink);
            }
        } catch (ClusterNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (ClusterAlreadyRunningException | ClusterAlreadyStoppedException e) {
            // wrong-state (e.g. stop of an already-stopped cluster) — a precondition failure, not transport.
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    // The bare-metal matriarch is a single environment — itself. Serving api.v1 ListEnvironments/
    // GetEnvironment (returning that one LIVE entry) lets the CLI treat local and cloud uniformly:
    // `stackgres environment list` works against a local matriarch too, and env auto-resolution sees
    // exactly one. The id comes from EnvironmentIdentity (config or generated-and-persisted), so it is
    // stable and unique across matriarchs — and it matches the id stamped on the clusters below.

    /** Map a cluster and stamp this environment's id (the mapper leaves environment_id empty). */
    private io.stackgres.proto.api.v1.Cluster stampEnv(io.stackgres.matriarch.model.Cluster cluster) {
        return ProtoMapper.toProto(cluster).toBuilder().setEnvironmentId(identity.id()).build();
    }

    @Override
    public void listEnvironments(ListEnvironmentsRequest request, StreamObserver<ListEnvironmentsResponse> responseObserver) {
        Environment env = identity.environment();
        responseObserver.onNext(ListEnvironmentsResponse.newBuilder()
                .addEnvironment(env)
                .putSourceInfo(env.getId().getValue(), ProtoMapper.liveSourceInfo())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getEnvironment(GetEnvironmentRequest request, StreamObserver<GetEnvironmentResponse> responseObserver) {
        Environment env = identity.environment();
        String requested = request.getEnvironmentId();
        if (requested != null && !requested.isBlank() && !requested.equals(env.getId().getValue())) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("no such environment: " + requested).asRuntimeException());
            return;
        }
        responseObserver.onNext(GetEnvironmentResponse.newBuilder()
                .setEnvironment(env)
                .setSourceInfo(ProtoMapper.liveSourceInfo())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listClusters(ListClustersRequest request, StreamObserver<ListClustersResponse> responseObserver) {
        Map<String, String> filter = request.getTagsMap();   // empty → all; else only clusters carrying every key=value
        ListClustersResponse.Builder resp = ListClustersResponse.newBuilder();
        for (Cluster cluster : matriarch.listClusters())
            if (matchesTags(cluster.spec().tags(), filter))
                resp.addCluster(stampEnv(cluster));
        resp.putSourceInfo(identity.id(), ProtoMapper.liveSourceInfo());
        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    /**
     * A cluster matches when it carries every key=value in the filter (an empty filter matches all).
     */
    private static boolean matchesTags(java.util.Map<String, String> clusterTags, java.util.Map<String, String> filter) {
        return filter.entrySet().stream().allMatch(e -> e.getValue().equals(clusterTags.get(e.getKey())));
    }

    // Nodes — this matriarch's single slony host (or none), served from the live Slonys registry.
    @Override
    public void listNodes(ListNodesRequest request, StreamObserver<ListNodesResponse> responseObserver) {
        ListNodesResponse.Builder resp = ListNodesResponse.newBuilder();
        Node node = slonys.currentNode(identity.id());
        if (node != null && matchesTags(node.getTagsMap(), request.getTagsMap())) {
            resp.addNode(node);
        }
        resp.putSourceInfo(identity.id(), ProtoMapper.liveSourceInfo());
        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getNode(GetNodeRequest request, StreamObserver<GetNodeResponse> responseObserver) {
        Node node = slonys.currentNode(identity.id());
        if (node == null || !node.getId().getValue().equals(request.getId().getValue())) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("no such node").asRuntimeException());
            return;
        }
        responseObserver.onNext(GetNodeResponse.newBuilder()
                .setNode(node)
                .setSourceInfo(ProtoMapper.liveSourceInfo())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getCluster(GetClusterRequest request, StreamObserver<GetClusterResponse> responseObserver) {
        if (!request.hasId()) {
            responseObserver.onError(Status.UNIMPLEMENTED.withDescription("get is supported by cluster id only in this build").asRuntimeException());
            return;
        }
        ClusterId id = new ClusterId(request.getId().getValue());
        try {
            Cluster cluster = matriarch.requireCluster(id);
            responseObserver.onNext(GetClusterResponse.newBuilder()
                    .setCluster(stampEnv(cluster))
                    .setSourceInfo(ProtoMapper.liveSourceInfo())
                    .build());
            responseObserver.onCompleted();
        } catch (ClusterNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getClusterCredentials(io.stackgres.proto.api.v1.GetClusterCredentialsRequest request, StreamObserver<io.stackgres.proto.api.v1.GetClusterCredentialsResponse> responseObserver) {
        ClusterId id = new ClusterId(request.getClusterId().getValue());
        try {
            matriarch.requireCluster(id);   // 404 if absent, before resolving the secret (§3.7)
            Credentials creds = matriarch.getCredentials(id);   // secret-by-reference resolution
            responseObserver.onNext(io.stackgres.proto.api.v1.GetClusterCredentialsResponse.newBuilder()
                    .setUsername(creds.username() == null ? "" : creds.username())
                    .setPassword(creds.password() == null ? "" : creds.password())
                    .build());
            responseObserver.onCompleted();
        } catch (ClusterNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getServerInfo(io.stackgres.proto.api.v1.GetServerInfoRequest request,
                              StreamObserver<io.stackgres.proto.api.v1.GetServerInfoResponse> responseObserver) {
        responseObserver.onNext(io.stackgres.proto.api.v1.GetServerInfoResponse.newBuilder()
                .setVersion(serverVersion)
                .setComponent("matriarch")
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listVersions(io.stackgres.proto.api.v1.ListVersionsRequest request, StreamObserver<io.stackgres.proto.api.v1.ListVersionsResponse> responseObserver) {
        try {
            List<String> versions = versionCatalog.availableVersions(ProtoMapper.toDomain(request.getEngine()));
            responseObserver.onNext(io.stackgres.proto.api.v1.ListVersionsResponse.newBuilder().addAllVersion(versions).build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listExtensions(io.stackgres.proto.api.v1.ListExtensionsRequest request, StreamObserver<io.stackgres.proto.api.v1.ListExtensionsResponse> responseObserver) {
        try {
            DatabaseEngine engine = ProtoMapper.toDomain(request.getEngine());
            // The DOCIR extensions query needs an EXACT version; the client may pass a label ("17") or blank.
            String exact = versionCatalog.resolveVersion(engine, request.getVersion());
            ListExtensionsResponse.Builder resp = io.stackgres.proto.api.v1.ListExtensionsResponse.newBuilder();
            for (Extension e : extensionCatalog.availableExtensions(engine, exact)) {
                Builder eb = io.stackgres.proto.api.v1.postgres.Extension.newBuilder().setName(e.name());
                if (e.version() != null) eb.setVersion(e.version());
                if (e.revision() != null) eb.setRevision(e.revision());
                resp.addExtension(eb);
            }
            responseObserver.onNext(resp.build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalStateException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void getClusterEvents(io.stackgres.proto.api.v1.GetClusterEventsRequest request, StreamObserver<io.stackgres.proto.api.v1.GetClusterEventsResponse> responseObserver) {
        ClusterId id = new ClusterId(request.getClusterId().getValue());
        GetClusterEventsResponse.Builder resp = io.stackgres.proto.api.v1.GetClusterEventsResponse.newBuilder();
        for (ClusterEvent event : clusterEvents.events(id)) {   // chronological snapshot; WatchEvents stays UNIMPLEMENTED
            resp.addEvent(ProtoMapper.toProtoEvent(event));
        }
        resp.putSourceInfo(identity.id(), ProtoMapper.liveSourceInfo());
        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    /**
     * Bridges the core's ProgressSink to a gRPC ClusterOperationProgress stream. Transport
     * failures surface as a terminal FAILED frame (not an opaque UNKNOWN) and are
     * logged; provisioning failures arrive in-band as FAILED frames from the core.
     */
    private ProgressSink progressSink(StreamObserver<ClusterOperationProgress> responseObserver) {
        return new ProgressSink() {
            @Override
            public void onProgress(io.stackgres.matriarch.model.ClusterOperationProgress progress) {
                responseObserver.onNext(ProtoMapper.toProto(progress));
            }

            @Override
            public void onComplete() {
                responseObserver.onCompleted();
            }

            @Override
            public void onError(Throwable error) {
                String reason = error.getMessage() == null ? error.toString() : error.getMessage();
                LOG.errorf(error, "operation failed: %s", reason);
                responseObserver.onNext(ClusterOperationProgress.newBuilder()
                        .setStatus(OperationStatus.OPERATION_STATUS_FAILED)
                        .setError(com.google.rpc.Status.newBuilder().setCode(13).setMessage(reason).build())
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

}