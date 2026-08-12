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
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;


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

    @Override
    public void listClusters(ListClustersRequest request, StreamObserver<ListClustersResponse> responseObserver) {
        Map<String, String> filter = request.getTagsMap();   // empty → all; else only clusters carrying every key=value
        ListClustersResponse.Builder resp = ListClustersResponse.newBuilder();
        for (Cluster cluster : matriarch.listClusters())
            if (matchesTags(cluster.spec().tags(), filter))
                resp.addCluster(ProtoMapper.toProto(cluster));
        resp.putSourceInfo("local", ProtoMapper.liveSourceInfo());
        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    /**
     * A cluster matches when it carries every key=value in the filter (an empty filter matches all).
     */
    private static boolean matchesTags(java.util.Map<String, String> clusterTags, java.util.Map<String, String> filter) {
        return filter.entrySet().stream().allMatch(e -> e.getValue().equals(clusterTags.get(e.getKey())));
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
                    .setCluster(ProtoMapper.toProto(cluster))
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
        resp.putSourceInfo("local", ProtoMapper.liveSourceInfo());
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