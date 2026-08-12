package io.stackgres.matriarch.quarkus.grpc;
import io.stackgres.matriarch.quarkus.resources.*;

import io.stackgres.proto.cli.GetNodeLogsResponse;
import io.stackgres.proto.cli.TailNodeLogsResponse;
import io.stackgres.proto.slony.Log;
import io.grpc.stub.StreamObserver;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Request/response relay for the node-log RPCs (bridge, plan B): mirrors the old matriarch's
 * on-demand pull — the CLI asks for a node's logs, the matriarch sends a {@code GetSlonyLogsCommand}
 * to the slony agent, and the agent streams {@code Log} chunks back keyed by a minted logId,
 * terminated by a {@code Log.status}.
 *
 * <p>Two shapes share the {@link #onLog(Log)} dispatch via a {@link Sink}:
 * <ul>
 *   <li>{@code GetNodeLogs} (unary, {@code follow=false}) — accumulate the chunks, emit one response
 *       on the terminal status.</li>
 *   <li>{@code TailNodeLogs} (server stream, {@code follow=true}) — forward each chunk as a line,
 *       complete on the terminal status or on a client-side {@link #abort(UUID)}.</li>
 * </ul>
 * One in-flight request per logId; not durable — this matriarch session only.
 */
@ApplicationScoped
public class NodeLogRelay {

    /**
     * How a pending request consumes the agent's {@code Log} chunks + terminal.
     */
    private interface Sink {
        void chunk(String contents);

        void terminal(com.google.rpc.Status status);

        void fail(String message);
    }

    private final Map<UUID, Sink> byLog = new ConcurrentHashMap<>();

    /**
     * {@code GetNodeLogs} (unary): accumulate, emit one response on the terminal status.
     */
    void begin(UUID logId, StreamObserver<GetNodeLogsResponse> client) {
        byLog.put(logId, new Sink() {
            private final StringBuilder buffer = new StringBuilder();

            @Override
            public void chunk(String contents) {
                buffer.append(contents);
            }

            @Override
            public void terminal(com.google.rpc.Status status) {
                if (status.getCode() == 0) {   // 0 = OK — whole log delivered
                    client.onNext(GetNodeLogsResponse.newBuilder().setLogs(buffer.toString()).build());
                } else {
                    client.onNext(GetNodeLogsResponse.newBuilder().setStatus(status).build());
                }
                client.onCompleted();
            }

            @Override
            public void fail(String message) {
                client.onNext(GetNodeLogsResponse.newBuilder().setStatus(failedPrecondition(message)).build());
                client.onCompleted();
            }
        });
    }

    /**
     * {@code TailNodeLogs} (server stream): forward each chunk as a line as it arrives.
     */
    void beginFollow(UUID logId, StreamObserver<TailNodeLogsResponse> client) {
        byLog.put(logId, new Sink() {
            @Override
            public void chunk(String contents) {
                client.onNext(TailNodeLogsResponse.newBuilder().setLine(contents).build());
            }

            @Override
            public void terminal(com.google.rpc.Status status) {
                if (status.getCode() != 0) {   // surface an error tail; a clean end is just onCompleted
                    client.onNext(TailNodeLogsResponse.newBuilder().setStatus(status).build());
                }
                client.onCompleted();
            }

            @Override
            public void fail(String message) {
                client.onNext(TailNodeLogsResponse.newBuilder().setStatus(failedPrecondition(message)).build());
                client.onCompleted();
            }
        });
    }

    /**
     * The agent couldn't be reached — complete the request with a FAILED_PRECONDITION status.
     */
    void fail(UUID logId, String message) {
        Sink s = byLog.remove(logId);
        if (s != null) {
            s.fail(message);
        }
    }

    /**
     * The client cancelled a follow — drop the entry so a late terminal {@code Log} is ignored.
     */
    void abort(UUID logId) {
        byLog.remove(logId);
    }

    /**
     * A {@code Log} chunk (contents) and/or terminal ({@code status}) from the agent.
     */
    void onLog(Log log) {
        UUID logId = UuidCodec.fromProto(log.getId());
        Sink s = byLog.get(logId);
        if (s == null) {
            return;   // unknown / already-completed / aborted log id
        }
        if (log.hasContents()) {
            s.chunk(log.getContents());
        }
        if (log.hasStatus()) {   // terminal
            byLog.remove(logId);
            s.terminal(log.getStatus());
        }
    }

    private static com.google.rpc.Status failedPrecondition(String message) {
        return com.google.rpc.Status.newBuilder()
                .setCode(io.grpc.Status.Code.FAILED_PRECONDITION.value())
                .setMessage(message).build();
    }

}