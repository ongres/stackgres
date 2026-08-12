package io.stackgres.matriarch.quarkus.grpc;
import io.stackgres.matriarch.quarkus.resources.*;

import io.stackgres.proto.slon.MatriarchMessage;
import io.stackgres.proto.slon.SlonMessage;
import io.stackgres.proto.slon.SlonServiceGrpc;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;

import java.util.UUID;

/**
 * Matriarch side of the EXISTING slon {@code Transfer} stream (bridge, plan B):
 * slon (inside the container) dials in here once its container starts. On
 * registration it hands its response stream to {@link SlonyLinuxExecutor}; status
 * updates drive the InitDb→StartDb→HEALTHY lifecycle (and surface FAILED). A
 * dropped stream fails any in-flight wait so reconcile doesn't hang.
 */
@GrpcService
public class SlonBridgeService extends SlonServiceGrpc.SlonServiceImplBase {

    @Inject
    SlonyLinuxExecutor executor;

    @Inject
    Slons slons;

    @Inject
    TunnelRelay tunnelRelay;

    @Inject
    ExecRelay execRelay;

    @Override
    public StreamObserver<SlonMessage> transfer(StreamObserver<MatriarchMessage> responseObserver) {
        return new StreamObserver<>() {
            private UUID instanceId;

            @Override
            public void onNext(SlonMessage msg) {
                switch (msg.getKindCase()) {
                    case REGISTRATION -> {
                        instanceId = UuidCodec.fromProto(msg.getRegistration().getId());
                        slons.attach(instanceId, responseObserver);
                    }
                    case STATUSUPDATE -> {
                        if (instanceId != null) {
                            executor.onSlonStatus(instanceId, msg.getStatusUpdate());
                        }
                    }
                    case DIAGNOSTICS -> {
                        if (instanceId != null) {
                            executor.onDiagnostics(instanceId, msg.getDiagnostics().getDbSize());
                        }
                    }
                    case PGWIRETUNNELOPENED -> tunnelRelay.onOpened(UuidCodec.fromProto(msg.getPgWireTunnelOpened().getTunnelId()));
                    case PGWIRETUNNELDATA -> tunnelRelay.onData(UuidCodec.fromProto(msg.getPgWireTunnelData().getTunnelId()), msg.getPgWireTunnelData().getData());
                    case PGWIRETUNNELCLOSED -> tunnelRelay.onClosed(UuidCodec.fromProto(msg.getPgWireTunnelClosed().getTunnelId()));
                    case EXEC -> execRelay.onOutput(UuidCodec.fromProto(msg.getExec().getId()), msg.getExec().getBytes());
                    case EXECEXIT -> execRelay.onExit(UuidCodec.fromProto(msg.getExecExit().getId()), msg.getExecExit().getCode());
                    default -> { /* replication/log/event: ignored in cut #1 */ }
                }
            }

            @Override
            public void onError(Throwable t) {
                if (instanceId != null) {
                    executor.onSlonDisconnected(instanceId);
                }
            }

            @Override
            public void onCompleted() {
                if (instanceId != null) {
                    executor.onSlonDisconnected(instanceId);
                }
                responseObserver.onCompleted();
            }
        };
    }

}