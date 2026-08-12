package io.stackgres.matriarch.quarkus.grpc;
import io.stackgres.matriarch.quarkus.event.NodeEventStore;
import io.stackgres.matriarch.quarkus.resources.*;

import com.google.protobuf.Timestamp;
import io.stackgres.proto.cli.*;
import io.stackgres.proto.slony.ClusterInstance;
import io.stackgres.proto.slony.Registration;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

import java.util.Map;

/**
 * CLI side of the cli.proto {@code ResourceService} node commands, served as a bridge (plan B): the
 * new matriarch exposes the single connected slony-linux agent as a "node" — {@code ListSlonys},
 * {@code ListSlons}, tag add/remove, delete — built from the agent's Registration + the executor's
 * liveness. {@code ListK8s} is empty (bare-metal). {@code GetNodeEvents} serves a snapshot of the
 * agent's Event pushes (via {@link NodeEventStore}); {@code GetNodeLogs} / {@code TailNodeLogs} pull
 * the agent's logs on demand — snapshot and follow respectively (via {@link NodeLogRelay}). The
 * api.v1 {@code Environment} is the north-star model.
 */
@GrpcService
public class ResourceBridgeService extends ResourceServiceGrpc.ResourceServiceImplBase {

    @Inject
    Slonys slonys;

    @Inject
    NodeEventStore nodeEventStore;

    @Inject
    NodeLogRelay nodeLogRelay;

    @Override
    public void listSlonys(ListSlonysRequest request, StreamObserver<ListSlonysResponse> responseObserver) {
        ListSlonysResponse.Builder resp = ListSlonysResponse.newBuilder();
        Registration reg = slonys.registration();
        if (reg != null && matchesTags(slonys.nodeTags(), request.getTagsMap())) {
            resp.addSlony(buildSlony(reg));
        }
        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listSlons(ListSlonsRequest request, StreamObserver<ListSlonsResponse> responseObserver) {
        ListSlonsResponse.Builder resp = ListSlonsResponse.newBuilder();
        Registration reg = slonys.registration();
        if (reg != null) {
            for (ClusterInstance ci : reg.getInstanceList()) {
                resp.addSlon(Slon.newBuilder()
                        .setId(ci.getId())
                        .setPort(ci.getPort())
                        .setName(ci.getName())
                        .setOs(reg.getOs())          // per-instance os/arch/cpu/memory aren't reported;
                        .setArch(reg.getArch())      // reuse the host's (matches the old matriarch).
                        .setVersion(ci.getVersion())
                        .setCpu(reg.getCpu())
                        .setMemory(reg.getMemory())
                        .build());
            }
        }
        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listK8s(ListK8sRequest request, StreamObserver<ListK8sResponse> responseObserver) {
        responseObserver.onNext(ListK8sResponse.newBuilder().build());   // bare-metal — no k8s nodes
        responseObserver.onCompleted();
    }

    @Override
    public void addNodeTags(AddNodeTagsRequest request, StreamObserver<AddNodeTagsResponse> responseObserver) {
        if (slonys.registration() == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("no node connected").asRuntimeException());
            return;
        }
        slonys.nodeTags().putAll(request.getTagsMap());
        responseObserver.onNext(AddNodeTagsResponse.newBuilder()
                .setStatus(com.google.rpc.Status.newBuilder().setCode(0).build())   // 0 = OK
                .putAllCurrentTags(slonys.nodeTags())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeNodeTags(RemoveNodeTagsRequest request, StreamObserver<RemoveNodeTagsResponse> responseObserver) {
        if (slonys.registration() == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("no node connected").asRuntimeException());
            return;
        }
        request.getKeyList().forEach(slonys.nodeTags()::remove);
        responseObserver.onNext(RemoveNodeTagsResponse.newBuilder()
                .setStatus(com.google.rpc.Status.newBuilder().setCode(0).build())
                .putAllCurrentTags(slonys.nodeTags())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void deleteSlony(DeleteSlonyRequest request, StreamObserver<DeleteSlonyResponse> responseObserver) {
        Registration reg = slonys.registration();
        if (reg == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("no node to delete").asRuntimeException());
            return;
        }
        String hostname = reg.getHostname();
        slonys.forgetRegistration();   // detach/forget the single local agent
        responseObserver.onNext(DeleteSlonyResponse.newBuilder()
                .setDeleted(SlonyDeleted.newBuilder().setHostname(hostname).build())
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void getNodeLogs(GetNodeLogsRequest request, StreamObserver<GetNodeLogsResponse> responseObserver) {
        if (slonys.registration() == null) {
            responseObserver.onNext(GetNodeLogsResponse.newBuilder()
                    .setStatus(com.google.rpc.Status.newBuilder()
                            .setCode(Status.Code.FAILED_PRECONDITION.value())
                            .setMessage("no node connected").build())
                    .build());
            responseObserver.onCompleted();
            return;
        }
        // On-demand pull (bridge): mint a logId, register the waiting client, ask the agent to stream its
        // logs back. The relay completes the (unary) response when the agent's terminal Log arrives.
        java.util.UUID logId = java.util.UUID.randomUUID();
        nodeLogRelay.begin(logId, responseObserver);
        if (!slonys.requestLogs(logId, false)) {
            nodeLogRelay.fail(logId, "slony-linux agent disconnected");
        }
    }

    @Override
    public StreamObserver<TailNodeLogsRequest> tailNodeLogs(StreamObserver<TailNodeLogsResponse> responseObserver) {
        // Bidi (follow): the first request opens a follow; the client half-closing (Ctrl+C) aborts it.
        return new StreamObserver<>() {
            private java.util.UUID logId;

            @Override
            public void onNext(TailNodeLogsRequest request) {
                if (logId != null) {
                    return;   // one follow per stream
                }
                if (slonys.registration() == null) {
                    responseObserver.onNext(TailNodeLogsResponse.newBuilder()
                            .setStatus(com.google.rpc.Status.newBuilder()
                                    .setCode(Status.Code.FAILED_PRECONDITION.value())
                                    .setMessage("no node connected").build())
                            .build());
                    responseObserver.onCompleted();
                    return;
                }
                logId = java.util.UUID.randomUUID();
                nodeLogRelay.beginFollow(logId, responseObserver);
                if (!slonys.requestLogs(logId, true)) {
                    nodeLogRelay.fail(logId, "slony-linux agent disconnected");
                    logId = null;   // already completed — nothing to abort on half-close
                }
            }

            @Override
            public void onError(Throwable t) {
                onCompleted();   // treat a broken request stream like a half-close
            }

            @Override
            public void onCompleted() {
                if (logId != null) {
                    slonys.abortLogs(logId);   // tell the agent to stop tailing
                    nodeLogRelay.abort(logId);        // drop the entry; ignore any late terminal Log
                    try {
                        responseObserver.onCompleted();
                    } catch (RuntimeException ignore) {
                        // already completed by a terminal Log — fine
                    }
                }
            }
        };
    }

    @Override
    public void getNodeEvents(GetNodeEventsRequest request, StreamObserver<GetNodeEventsResponse> responseObserver) {
        NodeEvents.Builder events = NodeEvents.newBuilder();
        for (NodeEventStore.Stored stored : nodeEventStore.events()) {   // chronological snapshot of the agent's Event pushes
            io.stackgres.proto.slony.Event e = stored.event();
            Event.Builder b = Event.newBuilder()
                    .setTimestamp(stored.timestampMillis())   // agent Events carry no time — stamped on receipt
                    .setType(e.getType())
                    .putAllData(e.getDataMap());
            if (e.hasClusterId()) {
                b.setScope("cluster").setScopeId(e.getClusterId());   // common.UUID passes straight through
            } else if (e.hasInstanceId()) {
                b.setScope("instance").setScopeId(e.getInstanceId());
            } else {
                b.setScope("node");
            }
            events.addEvent(b);
        }
        responseObserver.onNext(GetNodeEventsResponse.newBuilder().setEvents(events).build());
        responseObserver.onCompleted();
    }

    private Slony buildSlony(io.stackgres.proto.slony.Registration reg) {
        long ms = slonys.lastHeartbeatMillis();
        Slony.Builder b = Slony.newBuilder()
                .setId(reg.getId())
                .setHostname(reg.getHostname())
                .setOs(reg.getOs())
                .setArch(reg.getArch())
                .setVersion(reg.getVersion())
                .setCpu(reg.getCpu())
                .setMemory(reg.getMemory())
                .setStatus(slonys.agentActive() ? SlonyStatus.SLONY_STATUS_ACTIVE : SlonyStatus.SLONY_STATUS_INACTIVE)
                .setLastHeartbeat(Timestamp.newBuilder()
                        .setSeconds(ms / 1000).setNanos((int) ((ms % 1000) * 1_000_000)).build())
                .putAllTags(slonys.nodeTags());
        if (reg.hasCloud()) b.setCloud(reg.getCloud());
        if (reg.hasRegion()) b.setRegion(reg.getRegion());
        if (reg.hasAvailabilityZone()) b.setAvailabilityZone(reg.getAvailabilityZone());
        if (reg.hasComputeInstanceName()) b.setComputeInstanceName(reg.getComputeInstanceName());
        return b.build();
    }

    /**
     * A node matches when it carries every key=value in the filter (empty filter matches).
     */
    private static boolean matchesTags(Map<String, String> nodeTags, Map<String, String> filter) {
        return filter.entrySet().stream().allMatch(e -> e.getValue().equals(nodeTags.get(e.getKey())));
    }

}