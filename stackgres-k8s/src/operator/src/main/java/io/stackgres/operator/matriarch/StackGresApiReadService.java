package io.stackgres.operator.matriarch;

import io.stackgres.matriarch.Matriarch;
import io.stackgres.matriarch.event.ClusterEvent;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.model.ClusterNotFoundException;
import io.stackgres.proto.api.v1.Environment;
import io.stackgres.proto.api.v1.GetClusterEventsRequest;
import io.stackgres.proto.api.v1.GetClusterEventsResponse;
import io.stackgres.proto.api.v1.GetClusterRequest;
import io.stackgres.proto.api.v1.GetClusterResponse;
import io.stackgres.proto.api.v1.GetEnvironmentRequest;
import io.stackgres.proto.api.v1.GetEnvironmentResponse;
import io.stackgres.proto.api.v1.ListClustersRequest;
import io.stackgres.proto.api.v1.ListClustersResponse;
import io.stackgres.proto.api.v1.ListEnvironmentsRequest;
import io.stackgres.proto.api.v1.ListEnvironmentsResponse;
import io.stackgres.proto.api.v1.StackGresApiGrpc;
import io.stackgres.proto.types.v1.ApiSurface;
import io.stackgres.proto.types.v1.Id;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import io.stackgres.operator.app.OperatorInstallationInfoHolder;
import jakarta.inject.Inject;

/**
 * Read-only {@code stackgres.api.v1.StackGresApi} surface over the embedded Matriarch core (v1 slice:
 * {@code list} + {@code get} + {@code events}). Every other RPC stays UNIMPLEMENTED (the ImplBase
 * default) — that is the read-only posture. The core state is fed by {@code StackGresObserver}; the
 * api.v1 {@code environment_id} is the StackGres installation id.
 */
@GrpcService
public class StackGresApiReadService extends StackGresApiGrpc.StackGresApiImplBase {

  @Inject
  Matriarch matriarch;

  @Inject
  ClusterEventStore clusterEvents;

  @Inject
  OperatorInstallationInfoHolder installationInfoHolder;

  /** The environment identity for this StackGres install (falls back to "local"). */
  private String environmentId() {
    String id = installationInfoHolder.getInstallationId();
    return id == null || id.isBlank() ? "local" : id;
  }

  /**
   * This StackGres install as one api.v1 Environment — itself. Serving ListEnvironments/GetEnvironment
   * (a single LIVE k8s-stackgres entry) lets the CLI treat local and cloud uniformly and lets env
   * auto-resolution see exactly one environment. Read-only, so only the events surface is advertised.
   */
  private Environment environment() {
    String id = environmentId();
    return Environment.newBuilder()
        .setId(Id.newBuilder().setValue(id))
        .setKind(Environment.Kind.KIND_K8S_STACKGRES)
        .addSurface(ApiSurface.API_SURFACE_EVENTS)
        .build();
  }

  @Override
  public void listEnvironments(ListEnvironmentsRequest request,
      StreamObserver<ListEnvironmentsResponse> responseObserver) {
    Environment env = environment();
    responseObserver.onNext(ListEnvironmentsResponse.newBuilder()
        .addEnvironment(env)
        .putSourceInfo(env.getId().getValue(), ClusterProtoMapper.liveSourceInfo())
        .build());
    responseObserver.onCompleted();
  }

  @Override
  public void getEnvironment(GetEnvironmentRequest request,
      StreamObserver<GetEnvironmentResponse> responseObserver) {
    Environment env = environment();
    String requested = request.getEnvironmentId();
    if (requested != null && !requested.isBlank() && !requested.equals(env.getId().getValue())) {
      responseObserver.onError(Status.NOT_FOUND
          .withDescription("no such environment: " + requested).asRuntimeException());
      return;
    }
    responseObserver.onNext(GetEnvironmentResponse.newBuilder()
        .setEnvironment(env)
        .setSourceInfo(ClusterProtoMapper.liveSourceInfo())
        .build());
    responseObserver.onCompleted();
  }

  @Override
  public void listClusters(ListClustersRequest request, StreamObserver<ListClustersResponse> responseObserver) {
    String envId = environmentId();
    ListClustersResponse.Builder resp = ListClustersResponse.newBuilder();
    for (Cluster cluster : matriarch.listClusters()) {
      resp.addCluster(ClusterProtoMapper.toProto(cluster, envId));
    }
    resp.putSourceInfo(envId, ClusterProtoMapper.liveSourceInfo());
    responseObserver.onNext(resp.build());
    responseObserver.onCompleted();
  }

  @Override
  public void getCluster(GetClusterRequest request, StreamObserver<GetClusterResponse> responseObserver) {
    if (!request.hasId()) {
      responseObserver.onError(Status.UNIMPLEMENTED
          .withDescription("get is supported by cluster id only").asRuntimeException());
      return;
    }
    ClusterId id = new ClusterId(request.getId().getValue());
    try {
      Cluster cluster = matriarch.requireCluster(id);
      responseObserver.onNext(GetClusterResponse.newBuilder()
          .setCluster(ClusterProtoMapper.toProto(cluster, environmentId()))
          .setSourceInfo(ClusterProtoMapper.liveSourceInfo())
          .build());
      responseObserver.onCompleted();
    } catch (ClusterNotFoundException e) {
      responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
    }
  }

  @Override
  public void getClusterEvents(GetClusterEventsRequest request,
      StreamObserver<GetClusterEventsResponse> responseObserver) {
    ClusterId id = new ClusterId(request.getClusterId().getValue());
    GetClusterEventsResponse.Builder resp = GetClusterEventsResponse.newBuilder();
    for (ClusterEvent event : clusterEvents.events(id)) {   // chronological snapshot; WatchEvents stays UNIMPLEMENTED
      resp.addEvent(ClusterProtoMapper.toProtoEvent(event));
    }
    resp.putSourceInfo(environmentId(), ClusterProtoMapper.liveSourceInfo());
    responseObserver.onNext(resp.build());
    responseObserver.onCompleted();
  }
}
