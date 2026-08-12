package io.stackgres.matriarch.quarkus.grpc;
import io.stackgres.matriarch.quarkus.event.NodeEventStore;
import io.stackgres.matriarch.quarkus.resources.*;

import io.stackgres.matriarch.Matriarch;
import io.stackgres.proto.slony.*;
import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.GrpcService;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Matriarch side of the EXISTING slony {@code Transfer} stream (bridge, plan B):
 * the real slony agent dials in here. On registration it (a) attaches the agent
 * stream and (b) feeds the agent's existing-instance list into the matriarch so a
 * freshly-(re)started matriarch recovers clusters it no longer remembers (§3.2).
 * Active only when the slony bridge is selected (the default).
 */
@GrpcService
public class SlonyBridgeService extends SlonyServiceGrpc.SlonyServiceImplBase {

    private static final Logger LOG = Logger.getLogger(SlonyBridgeService.class);

    @Inject
    SlonyLinuxExecutor executor;

    @Inject
    Slonys slonys;

    @Inject
    Matriarch matriarch;

    @Inject
    NodeEventStore nodeEventStore;

    @Inject
    NodeLogRelay nodeLogRelay;

    @Override
    public StreamObserver<SlonyMessage> transfer(StreamObserver<MatriarchMessage> responseObserver) {
        return new StreamObserver<>() {
            @Override
            public void onNext(SlonyMessage msg) {
                switch (msg.getKindCase()) {
                    case REGISTRATION -> {
                        Registration reg = msg.getRegistration();
                        slonys.attach(responseObserver, reg);
                        matriarch.adopt(SlonyMapper.recover(reg.getExternalAddress(), reg.getCpu(), reg.getMemory(), reg.getInstanceList()));
                        for (ClusterInstance ci : reg.getInstanceList()) {
                            // Seed the credential the agent reports so it survives a matriarch restart (§3.7).
                            matriarch.recordCredential(new io.stackgres.matriarch.model.ClusterId(ci.getClusterId().getValue().toStringUtf8()), ci.getPassword());
                            // Seed the executor's instance maps so adopted clusters get live status pushes + a real port.
                            executor.adoptInstance(
                                    java.util.UUID.fromString(ci.getId().getValue().toStringUtf8()),
                                    java.util.UUID.fromString(ci.getClusterId().getValue().toStringUtf8()),
                                    ci.getPort());
                            LOG.infof("recovered %s — host=127.0.0.1 port=%d user=%s", ci.getClusterName(), ci.getPort(), ci.getUsername(), ci.getPassword());
                        }
                        // Agent available now — provision any specs left PENDING while none was connected.
                        // Adopted clusters start UNKNOWN and are NOT reconciled (re-applying would re-init
                        // their DB); their true status arrives via the slon's live status pushes.
                        matriarch.reconcile();
                    }
                    case UNUSEDPORT -> executor.onUnusedPort(UuidCodec.fromProto(msg.getUnusedPort().getId()), msg.getUnusedPort().getPort());
                    case CLUSTERINSTANCECREATED -> executor.onInstanceCreated(UuidCodec.fromProto(msg.getClusterInstanceCreated().getId()));
                    case CLUSTERINSTANCEDELETED -> executor.onInstanceDeleted(UuidCodec.fromProto(msg.getClusterInstanceDeleted().getId()));
                    case HEARTBEAT -> {
                        if (slonys.onHeartbeat()) {
                            // Heartbeats resumed after a stall — provision anything left PENDING meanwhile.
                            matriarch.reconcile();
                        }
                    }
                    case EVENT -> nodeEventStore.record(msg.getEvent());
                    case LOG -> nodeLogRelay.onLog(msg.getLog());
                    default -> { /* log: ignored in cut #1 */ }
                }
            }

            @Override
            public void onError(Throwable t) {
                // agent disconnected (process killed / network) — drop the dead stream so the next
                // create rests at PENDING instead of writing to a completed stream. Recovered desired
                // state stays in memory and re-provisions when an agent reconnects (reconcile).
                slonys.detach(responseObserver);
            }

            @Override
            public void onCompleted() {
                slonys.detach(responseObserver);
                responseObserver.onCompleted();
            }
        };
    }

}