package io.stackgres.matriarch.quarkus.grpc;

import com.google.protobuf.Timestamp;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.stackgres.matriarch.quarkus.resources.UuidCodec;
import io.stackgres.proto.api.v1.LogLine;
import io.stackgres.proto.slon.Log;
import io.stackgres.proto.types.v1.Id;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request/response relay for the {@code api.v1 TailLogs} RPC (component logs, Slon-sourced): the
 * matriarch mints one {@code logId} per instance/component, asks each slon for that component's log
 * (see {@link io.stackgres.matriarch.quarkus.resources.Slons#requestLogs}), and this relay fans the
 * returning {@code Log} chunks from all of them into the single {@link LogLine} stream the caller reads.
 *
 * <p>A {@link Session} groups the sub-requests of one TailLogs call. A snapshot ({@code follow=false})
 * completes when every sub has delivered its terminal {@code Log.status}; a follow keeps streaming until
 * the client cancels (the API resource then {@link #drop}s the session and tells the slons to abort).
 * The session is <em>armed</em> only after all sub-requests are registered, so a fast snapshot sub can't
 * complete the call mid-setup. Not durable — this matriarch session only.
 */
@ApplicationScoped
public class ClusterLogRelay {

    private static final Logger LOG = Logger.getLogger(ClusterLogRelay.class);

    /** One TailLogs call: fans N per-instance/component sub-requests into one LogLine stream. */
    public static final class Session {
        private final StreamObserver<LogLine> client;
        private final boolean follow;
        private final Set<UUID> pending = ConcurrentHashMap.newKeySet();
        private boolean armed;
        private boolean closed;

        private Session(StreamObserver<LogLine> client, boolean follow) {
            this.client = client;
            this.follow = follow;
        }

        private synchronized void emit(String instanceId, String component, String line) {
            if (closed || line.isEmpty()) {
                return;
            }
            Instant now = Instant.now();
            client.onNext(LogLine.newBuilder()
                    .setTimestamp(Timestamp.newBuilder().setSeconds(now.getEpochSecond()).setNanos(now.getNano()))
                    .setInstanceId(Id.newBuilder().setValue(instanceId))
                    .setComponent(component)
                    .setLine(line)
                    .build());
        }

        private synchronized void complete() {
            if (!closed) {
                closed = true;
                client.onCompleted();
            }
        }

        private synchronized void error(String message) {
            if (!closed) {
                closed = true;
                client.onError(Status.FAILED_PRECONDITION.withDescription(message).asRuntimeException());
            }
        }
    }

    private record Sub(Session session, String instanceId, String component) {
    }

    private final Map<UUID, Sub> byLog = new ConcurrentHashMap<>();

    public Session newSession(StreamObserver<LogLine> client, boolean follow) {
        return new Session(client, follow);
    }

    /** Register a per-instance/component sub-request against a session, before sending it to the slon. */
    public void register(UUID logId, Session session, String instanceId, String component) {
        session.pending.add(logId);
        byLog.put(logId, new Sub(session, instanceId, component));
    }

    /** Setup is done: allow snapshot completion (and complete now if every sub already terminated). */
    public void arm(Session session) {
        session.armed = true;
        maybeComplete(session);
    }

    /** Fail the whole call (e.g. no connected instance). */
    public void error(Session session, String message) {
        session.error(message);
    }

    /** A sub-request could not be sent (slon gone) — drop it and finish the call if it was the last. */
    public void fail(UUID logId) {
        Sub sub = byLog.remove(logId);
        if (sub != null) {
            sub.session.pending.remove(logId);
            maybeComplete(sub.session);
        }
    }

    /** A {@code Log} chunk/terminal from a slon, keyed by the sub-request logId. */
    public void onLog(Log log) {
        UUID logId = UuidCodec.fromProto(log.getId());
        Sub sub = byLog.get(logId);
        if (sub == null) {
            return;   // unknown / already-completed / aborted
        }
        if (log.hasContents()) {
            for (String line : log.getContents().split("\n", -1)) {
                if (line.isEmpty()) {
                    continue;
                }
                // Re-shape the raw component line into the canonical JSON the CLI formats/colours.
                sub.session.emit(sub.instanceId, sub.component,
                        ComponentLogNormalizer.normalize(sub.component, line));
            }
        }
        if (log.hasStatus()) {   // terminal for this sub (OK = snapshot done / clean stop; non-OK = e.g. component unavailable)
            byLog.remove(logId);
            sub.session.pending.remove(logId);
            if (log.getStatus().getCode() != 0) {
                LOG.debugf("component %s on instance %s ended: %s",
                        sub.component, sub.instanceId, log.getStatus().getMessage());
            }
            maybeComplete(sub.session);
        }
    }

    /** Client cancelled/finished — forget the session's sub-requests so late {@code Log}s are ignored. */
    public void drop(Session session) {
        byLog.values().removeIf(sub -> sub.session == session);
        session.pending.clear();
    }

    private void maybeComplete(Session session) {
        // Snapshot: done once every registered sub has delivered its terminal. Follow: only the client ends it.
        if (session.armed && !session.follow && session.pending.isEmpty()) {
            session.complete();
        }
    }

}
