package io.stackgres.matriarch.quarkus.grpc;

import com.google.protobuf.ByteString;
import io.stackgres.matriarch.quarkus.resources.Slons;
import io.stackgres.matriarch.quarkus.resources.UuidCodec;
import io.stackgres.proto.cli.PgWireServerMessage;
import io.stackgres.proto.slon.MatriarchMessage;
import io.stackgres.proto.slon.OpenPgWireTunnelCommand;
import io.grpc.stub.StreamObserver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Relays raw PgWire bytes between a CLI {@code PgWireTunnel} stream and a slon agent (bridge, plan B):
 * the matriarch is a byte pump keyed by {@code tunnelId}. CLI→slon uses the slon
 * {@code OpenPgWireTunnelCommand}/{@code PgWireTunnelData}; slon→CLI callbacks (invoked from
 * {@link SlonBridgeService}) push {@code PgWireServerMessage}s back to the client. The bytes never
 * traverse api.v1 (§6) — the ticket/Truba data-plane is the cloud-scale successor.
 */
@ApplicationScoped
public class TunnelRelay {

    @Inject
    Slons slons;

    private final Map<UUID, StreamObserver<PgWireServerMessage>> clientByTunnel = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> instanceByTunnel = new ConcurrentHashMap<>();

    // ---- CLI -> slon ----

    /**
     * Open a tunnel to the instance's Postgres ({@code targetPort} null = default PG port). False if no slon.
     */
    boolean open(UUID tunnelId, UUID instanceId, Integer targetPort, StreamObserver<PgWireServerMessage> client) {
        clientByTunnel.put(tunnelId, client);
        instanceByTunnel.put(tunnelId, instanceId);
        OpenPgWireTunnelCommand.Builder cmd = io.stackgres.proto.slon.OpenPgWireTunnelCommand.newBuilder().setTunnelId(UuidCodec.toProto(tunnelId));
        if (targetPort != null) {
            cmd.setTargetPort(targetPort);
        }
        boolean sent = slons.send(instanceId, MatriarchMessage.newBuilder().setOpenPgWireTunnelCommand(cmd).build());
        if (!sent) {
            clientByTunnel.remove(tunnelId);
            instanceByTunnel.remove(tunnelId);
        }
        return sent;
    }

    void fromClient(UUID tunnelId, ByteString data) {
        UUID instanceId = instanceByTunnel.get(tunnelId);
        if (instanceId == null) {
            return;
        }
        slons.send(instanceId, MatriarchMessage.newBuilder()
                .setPgWireTunnelData(io.stackgres.proto.slon.PgWireTunnelData.newBuilder()
                        .setTunnelId(UuidCodec.toProto(tunnelId)).setData(data))
                .build());
    }

    /**
     * CLI-requested (or stream-ended) close: tell the slon and drop the tunnel.
     */
    void close(UUID tunnelId) {
        UUID instanceId = instanceByTunnel.remove(tunnelId);
        StreamObserver<PgWireServerMessage> client = clientByTunnel.remove(tunnelId);
        if (instanceId != null) {
            slons.send(instanceId, MatriarchMessage.newBuilder()
                    .setClosePgWireTunnelCommand(io.stackgres.proto.slon.ClosePgWireTunnelCommand.newBuilder()
                            .setTunnelId(UuidCodec.toProto(tunnelId)))
                    .build());
        }
        if (client != null) {
            try {
                client.onCompleted();
            } catch (RuntimeException ignore) {
                /* client stream already closed */
            }
        }
    }

    // ---- slon -> CLI (called by SlonBridgeService, serialized per slon stream) ----

    void onOpened(UUID tunnelId) {
        StreamObserver<PgWireServerMessage> client = clientByTunnel.get(tunnelId);
        if (client != null) {
            client.onNext(PgWireServerMessage.newBuilder().setOpened(io.stackgres.proto.cli.PgWireTunnelOpened.newBuilder()).build());
        }
    }

    void onData(UUID tunnelId, ByteString data) {
        StreamObserver<PgWireServerMessage> client = clientByTunnel.get(tunnelId);
        if (client != null) {
            client.onNext(PgWireServerMessage.newBuilder().setData(io.stackgres.proto.cli.PgWireTunnelData.newBuilder().setData(data)).build());
        }
    }

    void onClosed(UUID tunnelId) {
        instanceByTunnel.remove(tunnelId);
        StreamObserver<PgWireServerMessage> client = clientByTunnel.remove(tunnelId);   // remove() dedupes vs a concurrent CLI close()
        if (client != null) {
            client.onNext(PgWireServerMessage.newBuilder().setClosed(io.stackgres.proto.cli.PgWireTunnelClosed.newBuilder()).build());
            try {
                client.onCompleted();
            } catch (RuntimeException ignore) {
                /* client stream already closed */
            }
        }
    }

}