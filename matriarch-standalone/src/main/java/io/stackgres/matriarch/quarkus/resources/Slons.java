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

    public boolean isConnected(UUID instanceId) {
        return connections.containsKey(instanceId);
    }

    /**
     * Ask an instance's slon to stream one component's log back (keyed by {@code logId}); {@code follow}
     * streams new lines, otherwise a recent snapshot. The slon replies with {@code Log} chunks routed to
     * {@link io.stackgres.matriarch.quarkus.grpc.ClusterLogRelay}. Returns false if the component is
     * unknown or the slon stream is gone.
     */
    public boolean requestLogs(UUID instanceId, UUID logId, String component, boolean follow) {
        common.Common.UUID id = UuidCodec.toProto(logId);
        MatriarchMessage msg = switch (component) {
            case "postgres" -> MatriarchMessage.newBuilder()
                    .setGetPostgresLogsCommand(GetPostgresLogsCommand.newBuilder().setId(id).setFollow(follow)).build();
            case "patroni" -> MatriarchMessage.newBuilder()
                    .setGetPatroniLogsCommand(GetPatroniLogsCommand.newBuilder().setId(id).setFollow(follow)).build();
            case "slon" -> MatriarchMessage.newBuilder()
                    .setGetSlonLogsCommand(GetSlonLogsCommand.newBuilder().setId(id).setFollow(follow)).build();
            case "etcd" -> MatriarchMessage.newBuilder()
                    .setGetEtcdLogsCommand(GetEtcdLogsCommand.newBuilder().setId(id).setFollow(follow)).build();
            default -> null;
        };
        return msg != null && send(instanceId, msg);
    }

    /** Tell an instance's slon to stop a log follow (by its {@code logId}). */
    public boolean abortLogs(UUID instanceId, UUID logId) {
        return send(instanceId, MatriarchMessage.newBuilder()
                .setAbortLogsCommand(AbortLogsCommand.newBuilder().setId(UuidCodec.toProto(logId))).build());
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