package io.stackgres.matriarch.quarkus.grpc;
import io.stackgres.matriarch.quarkus.resources.*;

import com.google.protobuf.ByteString;
import io.stackgres.proto.cli.MatriarchExecMessage;
import io.grpc.stub.StreamObserver;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Relays an exec session (stdin / stdout+stderr / exit) between a CLI {@code ExecInCluster} stream
 * and a slon agent (bridge, plan B), keyed by {@code execId}. The twin of {@link TunnelRelay}:
 * CLI→slon uses the slon {@code ExecCommand}/{@code ExecMessage}; slon→CLI callbacks (invoked from
 * {@link SlonBridgeService}) push {@code MatriarchExecMessage}s back. The bytes never traverse
 * api.v1 (§6) — the ticket/Truba data-plane is the cloud-scale successor.
 */
@ApplicationScoped
public class ExecRelay {

    @Inject
    Slons slons;

    private final Map<UUID, StreamObserver<MatriarchExecMessage>> clientByExec = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> instanceByExec = new ConcurrentHashMap<>();

    // ---- CLI -> slon ----

    /**
     * Start {@code command} in the instance's container. False if no slon is connected.
     */
    boolean start(UUID execId, UUID instanceId, List<String> command, StreamObserver<MatriarchExecMessage> client) {
        clientByExec.put(execId, client);
        instanceByExec.put(execId, instanceId);
        boolean sent = slons.send(instanceId, io.stackgres.proto.slon.MatriarchMessage.newBuilder()
                .setExecCommand(io.stackgres.proto.slon.ExecCommand.newBuilder()
                        .setId(UuidCodec.toProto(execId)).addAllCommand(command))
                .build());
        if (!sent) {
            clientByExec.remove(execId);
            instanceByExec.remove(execId);
        }
        return sent;
    }

    void stdin(UUID execId, ByteString bytes) {
        UUID instanceId = instanceByExec.get(execId);
        if (instanceId == null) {
            return;
        }
        slons.send(instanceId, io.stackgres.proto.slon.MatriarchMessage.newBuilder()
                .setExecMessage(io.stackgres.proto.slon.ExecMessage.newBuilder()
                        .setId(UuidCodec.toProto(execId)).setBytes(bytes))
                .build());
    }

    /**
     * CLI-requested (or stream-ended) close: drop the session and complete the client stream. The
     * slon exec protocol has no kill command (matching the old matriarch) — the process ends on its
     * own exit / stdin EOF, which arrives as {@link #onExit}.
     */
    void close(UUID execId) {
        instanceByExec.remove(execId);
        StreamObserver<MatriarchExecMessage> client = clientByExec.remove(execId);
        if (client != null) {
            try {
                client.onCompleted();
            } catch (RuntimeException ignore) {
                /* client stream already closed */
            }
        }
    }

    // ---- slon -> CLI (called by SlonBridgeService, serialized per slon stream) ----

    void onOutput(UUID execId, ByteString bytes) {
        StreamObserver<MatriarchExecMessage> client = clientByExec.get(execId);
        if (client != null) {
            client.onNext(MatriarchExecMessage.newBuilder().setBytes(bytes).build());
        }
    }

    void onExit(UUID execId, int code) {
        instanceByExec.remove(execId);
        StreamObserver<MatriarchExecMessage> client = clientByExec.remove(execId);   // remove() dedupes vs a concurrent CLI close()
        if (client != null) {
            client.onNext(MatriarchExecMessage.newBuilder().setExitCode(code).build());
            try {
                client.onCompleted();
            } catch (RuntimeException ignore) {
                /* client stream already closed */
            }
        }
    }

}