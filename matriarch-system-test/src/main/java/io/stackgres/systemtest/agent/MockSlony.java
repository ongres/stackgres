package io.stackgres.systemtest.agent;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.stackgres.proto.slony.ClusterInstanceCreated;
import io.stackgres.proto.slony.ClusterInstanceDeleted;
import io.stackgres.proto.slony.CreateClusterCommand;
import io.stackgres.proto.slony.CreateInstance;
import io.stackgres.proto.slony.Event;
import io.stackgres.proto.slony.Log;
import io.stackgres.proto.slony.MatriarchMessage;
import io.stackgres.proto.slony.Registration;
import io.stackgres.proto.slony.ReportUnusedPortCommand;
import io.stackgres.proto.slony.SlonyMessage;
import io.stackgres.proto.slony.SlonyServiceGrpc;
import io.stackgres.proto.slony.UnusedPort;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A behavioral mock of the {@code slony} host agent. It dials the matriarch's {@code SlonyService.Transfer}
 * stream, registers as a node, and plays the host side of the protocol — containerd-free, no Postgres:
 * <ul>
 *   <li><b>Node ops</b> (list/get/delete/tags/events/logs) work off the registration; it answers node-log
 *       requests and can push node events.</li>
 *   <li><b>Cluster create</b>: it answers the port handshake, and on {@code CreateClusterCommand} spawns a
 *       {@link MockSlon} per instance (each on its own {@code SlonService} stream reporting HEALTHY) and
 *       replies {@code ClusterInstanceCreated}; delete stops the slon and replies {@code ...Deleted}.</li>
 * </ul>
 * Fault injection for negative scenarios: {@link #failLogs}, {@link #failCreate}, {@link #slonFailOnStart}.
 */
public final class MockSlony implements AutoCloseable {

    private final String host;
    private final int port;
    private final String nodeId;
    private final String hostname;
    private final Map<String, String> tags;
    private final ManagedChannel channel;
    private final Map<String, MockSlon> slons = new ConcurrentHashMap<>();   // instanceId -> mock slon

    private StreamObserver<SlonyMessage> toMatriarch;
    private volatile boolean failLogs;
    private volatile boolean failCreate;
    private volatile boolean slonFailOnStart;

    private MockSlony(String host, int port, String nodeId, String hostname, Map<String, String> tags) {
        this.host = host;
        this.port = port;
        this.nodeId = nodeId;
        this.hostname = hostname;
        this.tags = tags;
        this.channel = NettyChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    /** Connect to the matriarch and register a node; returns the started mock. */
    public static MockSlony start(String host, int port, Map<String, String> tags) {
        MockSlony mock = new MockSlony(host, port, UUID.randomUUID().toString(), "mock-" + System.nanoTime(), tags);
        mock.open();
        return mock;
    }

    public String nodeId() {
        return nodeId;
    }

    /** Push a node event up to the matriarch (surfaces on {@code node events} / the Activity feed). */
    public synchronized void pushEvent(String type, Map<String, String> data) {
        toMatriarch.onNext(SlonyMessage.newBuilder()
                .setEvent(Event.newBuilder().setType(type).putAllData(data).build())
                .build());
    }

    /** Make node-log requests fail (negative node-log scenario). */
    public void failLogs(boolean fail) {
        this.failLogs = fail;
    }

    /** Make the next create fail at the agent (ClusterInstanceCreated non-OK) → cluster FAILED before any slon. */
    public void failCreate(boolean fail) {
        this.failCreate = fail;
    }

    /** Make spawned mock-slons report FAILED instead of HEALTHY on StartDb → cluster FAILED while converging. */
    public void slonFailOnStart(boolean fail) {
        this.slonFailOnStart = fail;
    }

    @Override
    public synchronized void close() {
        slons.values().forEach(MockSlon::close);
        slons.clear();
        try {
            if (toMatriarch != null) {
                toMatriarch.onCompleted();
            }
        } catch (RuntimeException ignored) {
            // already torn down
        }
        channel.shutdownNow();
        try {
            channel.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void open() {
        SlonyServiceGrpc.SlonyServiceStub stub = SlonyServiceGrpc.newStub(channel);
        this.toMatriarch = stub.transfer(new StreamObserver<>() {
            @Override
            public void onNext(MatriarchMessage msg) {
                handle(msg);
            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
            }
        });
        send(SlonyMessage.newBuilder().setRegistration(Registration.newBuilder()
                .setId(common.Common.UUID.newBuilder().setValue(ByteString.copyFromUtf8(nodeId)))
                .setHostname(hostname)
                .setOs("linux")
                .setArch("amd64")
                .setVersion("mock-slony")
                .setCpu(4)
                .setMemory(8L * 1024 * 1024 * 1024)
                .putAllTags(tags)
                .build()).build());
    }

    private void handle(MatriarchMessage msg) {
        switch (msg.getKindCase()) {
            case GETSLONYLOGSCOMMAND -> {
                var cmd = msg.getGetSlonyLogsCommand();
                var id = cmd.getId();
                if (failLogs) {
                    send(SlonyMessage.newBuilder().setLog(Log.newBuilder().setId(id)
                            .setStatus(com.google.rpc.Status.newBuilder().setCode(9).setMessage("mock log failure"))).build());
                } else {
                    send(SlonyMessage.newBuilder().setLog(Log.newBuilder().setId(id)
                            .setContents("mock slony log line\n")).build());
                    if (!cmd.getFollow()) {   // snapshot ends with a terminal status; a follow stays open until abort
                        send(SlonyMessage.newBuilder().setLog(Log.newBuilder().setId(id)
                                .setStatus(com.google.rpc.Status.newBuilder().setCode(0))).build());
                    }
                }
            }
            case ABORTSLONYLOGSCOMMAND -> send(SlonyMessage.newBuilder().setLog(Log.newBuilder()   // stop a follow
                    .setId(msg.getAbortSlonyLogsCommand().getId())
                    .setStatus(com.google.rpc.Status.newBuilder().setCode(0))).build());
            case REPORTUNUSEDPORTCOMMAND -> {
                ReportUnusedPortCommand c = msg.getReportUnusedPortCommand();
                send(SlonyMessage.newBuilder().setUnusedPort(UnusedPort.newBuilder()
                        .setId(c.getId()).setPort(c.getDesiredPort())).build());   // "allocate" the asked-for port
            }
            case CREATECLUSTERCOMMAND -> onCreate(msg.getCreateClusterCommand());
            case DELETEINSTANCECOMMAND -> {
                var id = msg.getDeleteInstanceCommand().getId();
                MockSlon slon = slons.remove(id.getValue().toStringUtf8());
                if (slon != null) {
                    slon.close();
                }
                send(SlonyMessage.newBuilder().setClusterInstanceDeleted(ClusterInstanceDeleted.newBuilder().setId(id)).build());
            }
            case SHUTDOWNCOMMAND -> close();
            default -> {
                // AbortSlonyLogs etc. — nothing to do in this cut
            }
        }
    }

    private void onCreate(CreateClusterCommand cmd) {
        String clusterId = cmd.getId().getValue().toStringUtf8();
        for (CreateInstance ci : cmd.getInstanceList()) {
            String instanceId = ci.getId().getValue().toStringUtf8();
            ClusterInstanceCreated.Builder created = ClusterInstanceCreated.newBuilder()
                    .setId(ci.getId()).setImageName("mock-slon").setImageDigest("sha256:mock");
            if (failCreate) {
                created.setStatus(com.google.rpc.Status.newBuilder().setCode(9).setMessage("mock create failure"));
            } else {
                MockSlon slon = new MockSlon(host, port, new MockSlon.Spec(
                        instanceId, clusterId, cmd.getName(), ci.getPort(), ci.getName(),
                        cmd.getUsername(), cmd.getPassword(), ci.getListenAddress(), slonFailOnStart));
                slon.open();
                slons.put(instanceId, slon);
            }
            send(SlonyMessage.newBuilder().setClusterInstanceCreated(created).build());
        }
    }

    private synchronized void send(SlonyMessage msg) {
        toMatriarch.onNext(msg);
    }
}
