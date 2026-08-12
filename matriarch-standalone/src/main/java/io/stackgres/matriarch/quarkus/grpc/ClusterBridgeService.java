package io.stackgres.matriarch.quarkus.grpc;

import io.stackgres.matriarch.Matriarch;
import io.stackgres.matriarch.model.Cluster;
import io.stackgres.matriarch.model.ClusterId;
import io.stackgres.matriarch.quarkus.resources.UuidCodec;
import io.stackgres.proto.cli.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * CLI side of the cli.proto {@code ClusterService} data-plane RPCs the new matriarch serves as a
 * bridge (plan B): {@code PgWireTunnel} (psql) and {@code ExecInCluster} (exec) — bidirectional byte
 * streams pumped through {@link TunnelRelay}/{@link ExecRelay} to the slon agent. Every other
 * {@code ClusterService} RPC stays UNIMPLEMENTED (the CLI's remaining cli.proto calls target the old
 * system). Keeping these bytes on cli.proto rather than api.v1 honors §6 ("no DB/exec bytes traverse
 * the control plane").
 */
@GrpcService
public class ClusterBridgeService extends ClusterServiceGrpc.ClusterServiceImplBase {

    @Inject
    TunnelRelay tunnelRelay;

    @Inject
    ExecRelay execRelay;

    @Inject
    Matriarch matriarch;

    @Override
    public StreamObserver<PgWireClientMessage> pgWireTunnel(StreamObserver<PgWireServerMessage> responseObserver) {
        return new StreamObserver<>() {
            private UUID tunnelId;

            @Override
            public void onNext(PgWireClientMessage msg) {
                try {
                    if (msg.hasOpen()) {
                        handleOpen(msg.getOpen());
                    } else if (msg.hasData()) {
                        if (tunnelId != null) {
                            tunnelRelay.fromClient(tunnelId, msg.getData().getData());
                        }
                    } else if (msg.hasClose()) {
                        if (tunnelId != null) {
                            tunnelRelay.close(tunnelId);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
                } catch (IllegalStateException e) {
                    responseObserver.onError(Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
                } catch (RuntimeException e) {
                    responseObserver.onError(Status.INTERNAL.withDescription(String.valueOf(e.getMessage())).asRuntimeException());
                }
            }

            private void handleOpen(PgWireTunnelOpen open) {
                UUID instanceId = resolveInstance(UuidCodec.fromProto(open.getClusterId()), open.hasInstanceId() ? UuidCodec.fromProto(open.getInstanceId()) : null);
                tunnelId = UUID.randomUUID();
                // Leave targetPort unset → the slon tunnels to the cluster's Postgres port (the psql case).
                if (!tunnelRelay.open(tunnelId, instanceId, null, responseObserver)) {
                    throw new IllegalStateException("no slon agent connected for this cluster — cannot open a tunnel");
                }
            }

            @Override
            public void onError(Throwable t) {
                if (tunnelId != null) {
                    tunnelRelay.close(tunnelId);
                }
            }

            @Override
            public void onCompleted() {
                if (tunnelId != null) {
                    tunnelRelay.close(tunnelId);
                }
            }
        };
    }

    @Override
    public StreamObserver<CliExecMessage> execInCluster(StreamObserver<MatriarchExecMessage> responseObserver) {
        return new StreamObserver<>() {
            private UUID execId;

            @Override
            public void onNext(CliExecMessage msg) {
                try {
                    if (msg.hasExecCommand()) {
                        handleStart(msg.getExecCommand());
                    } else if (msg.hasBytes()) {
                        if (execId != null) {
                            execRelay.stdin(execId, msg.getBytes());
                        }
                    }
                } catch (IllegalArgumentException e) {
                    responseObserver.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
                } catch (IllegalStateException e) {
                    responseObserver.onError(Status.UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
                } catch (RuntimeException e) {
                    responseObserver.onError(Status.INTERNAL.withDescription(String.valueOf(e.getMessage())).asRuntimeException());
                }
            }

            private void handleStart(ExecCommand command) {
                UUID instanceId = resolveInstance(UuidCodec.fromProto(command.getClusterId()),
                        command.hasInstanceId() ? UuidCodec.fromProto(command.getInstanceId()) : null);
                execId = UUID.randomUUID();
                if (!execRelay.start(execId, instanceId, command.getCommandList(), responseObserver)) {
                    throw new IllegalStateException("no slon agent connected for this cluster — cannot exec");
                }
            }

            @Override
            public void onError(Throwable t) {
                if (execId != null) {
                    execRelay.close(execId);
                }
            }

            @Override
            public void onCompleted() {
                if (execId != null) {
                    execRelay.close(execId);
                }
            }
        };
    }

    /**
     * The explicit instance if given, else the cluster's primary. Throws if the cluster is unknown.
     */
    private UUID resolveInstance(UUID clusterId, UUID explicitInstanceId) {
        Cluster cluster = matriarch.getCluster(new ClusterId(clusterId.toString()));
        if (cluster == null) {
            throw new IllegalArgumentException("cluster " + clusterId + " not found");
        }
        return explicitInstanceId != null ? explicitInstanceId : UUID.fromString(cluster.spec().instances().get(0).id().value());
    }

}