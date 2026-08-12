package io.stackgres.matriarch.quarkus.resources;

import io.stackgres.proto.slon.*;
import io.grpc.stub.StreamObserver;
import jakarta.enterprise.context.ApplicationScoped;

import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance slon connection registry (mirrors the old matriarch's {@code Slons}): owns the live
 * slon streams keyed by instanceId and builds+sends the slon {@link MatriarchMessage}s. The executor
 * drives lifecycle through the intent methods ({@link #initDb}/{@link #startDb}/{@link #stopDb}); the
 * data-plane relays push tunnel/exec bytes through {@link #send}. Streams are kept past HEALTHY so
 * start/stop/restart and tunnels can still reach the slon.
 *
 * <p>This is the only class that constructs the slon lifecycle {@code MatriarchMessage}s — the
 * executor speaks intent, not wire messages.
 */
@ApplicationScoped
public class Slons {

    private static final System.Logger LOG = System.getLogger(Slons.class.getName());

    private final Map<UUID, StreamObserver<MatriarchMessage>> connections = new ConcurrentHashMap<>();

    /**
     * Register (or replace) an instance's live slon stream; kept past HEALTHY for lifecycle + tunnels.
     */
    public void attach(UUID instanceId, StreamObserver<MatriarchMessage> out) {
        connections.put(instanceId, out);
        LOG.log(Level.INFO, "slon registered for instance {0}", instanceId);
    }

    void remove(UUID instanceId) {
        connections.remove(instanceId);
    }

    boolean isConnected(UUID instanceId) {
        return connections.containsKey(instanceId);
    }

    /**
     * Send to a specific instance's live slon stream (lifecycle + tunnel/exec paths); false if the
     * stream is gone or dead (a dead stream is dropped so the next attempt sees "no slon").
     */
    public boolean send(UUID instanceId, MatriarchMessage msg) {
        StreamObserver<MatriarchMessage> out = connections.get(instanceId);
        if (out == null) {
            return false;
        }
        try {
            synchronized (out) {
                out.onNext(msg);
            }
            return true;
        } catch (RuntimeException e) {
            connections.remove(instanceId);
            return false;
        }
    }

    boolean initDb(UUID instanceId) {
        return send(instanceId, MatriarchMessage.newBuilder()
                .setInitDbCommand(InitDbCommand.newBuilder()
                        .setTls(TlsConfig.newBuilder().setSelfSignedCerts(SelfSignedCerts.newBuilder())))
                .build());
    }

    boolean startDb(UUID instanceId) {
        return send(instanceId, MatriarchMessage.newBuilder()
                .setStartDbCommand(StartDbCommand.newBuilder())
                .build());
    }

    boolean stopDb(UUID instanceId) {
        return send(instanceId, MatriarchMessage.newBuilder()
                .setStopDbCommand(StopDbCommand.newBuilder())
                .build());
    }

}