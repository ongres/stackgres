package io.stackgres.systemtest.agent;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.stackgres.proto.slon.ExecExit;
import io.stackgres.proto.slon.ExecMessage;
import io.stackgres.proto.slon.Log;
import io.stackgres.proto.slon.MatriarchMessage;
import io.stackgres.proto.slon.PgWireTunnelClosed;
import io.stackgres.proto.slon.PgWireTunnelData;
import io.stackgres.proto.slon.PgWireTunnelOpened;
import io.stackgres.proto.slon.Registration;
import io.stackgres.proto.slon.ReplicationStatus;
import io.stackgres.proto.slon.ReplicationStatusUpdate;
import io.stackgres.proto.slon.SlonMessage;
import io.stackgres.proto.slon.SlonServiceGrpc;
import io.stackgres.proto.slon.SlonStatus;
import io.stackgres.proto.slon.StatusUpdate;

import java.util.concurrent.TimeUnit;

/**
 * A per-instance mock of the {@code slon} agent, spawned by {@link MockSlony} on create. It opens its OWN
 * {@code SlonService.Transfer} stream (as the real per-instance slon does), registers, and drives the exact
 * status machine the matriarch expects: push {@code CREATED} → on {@code InitDbCommand} push {@code INITDB}
 * → on {@code StartDbCommand} push {@code STARTED} + {@code HEALTHY} (or {@code FAILED} when told). Answers
 * component-log requests. No real Postgres.
 */
final class MockSlon implements AutoCloseable {

    /** What the matriarch told slony to create (mirrored back on the slon stream). */
    record Spec(String instanceId, String clusterId, String clusterName, int port, String instanceName,
                String username, String password, String listenAddress, boolean failOnStart) {
    }

    private final ManagedChannel channel;
    private final Spec spec;
    private StreamObserver<SlonMessage> toMatriarch;

    MockSlon(String host, int port, Spec spec) {
        this.spec = spec;
        this.channel = NettyChannelBuilder.forAddress(host, port).usePlaintext().build();
    }

    void open() {
        SlonServiceGrpc.SlonServiceStub stub = SlonServiceGrpc.newStub(channel);
        toMatriarch = stub.transfer(new StreamObserver<>() {
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
        send(SlonMessage.newBuilder().setRegistration(Registration.newBuilder()
                .setId(uuid(spec.instanceId()))
                .setClusterId(uuid(spec.clusterId()))
                .setClusterName(spec.clusterName())
                .setPort(spec.port())
                .setInstanceName(spec.instanceName())
                .setOs("linux").setArch("amd64").setVersion("mock-slon")
                .setCpu(4).setMemory(8L * 1024 * 1024 * 1024)
                .setUsername(spec.username()).setPassword(spec.password())
                .setListenAddress(spec.listenAddress().isEmpty() ? "0.0.0.0" : spec.listenAddress())
                .build()).build());
        send(status(SlonStatus.STATUS_CREATED));   // kicks off InitDb → StartDb → HEALTHY on the matriarch
    }

    private void handle(MatriarchMessage msg) {
        switch (msg.getKindCase()) {
            case INITDBCOMMAND -> send(status(SlonStatus.STATUS_INITDB));
            case STARTDBCOMMAND -> {
                if (spec.failOnStart()) {
                    send(failed("mock slon: postgres failed to start"));
                } else {
                    send(SlonMessage.newBuilder().setReplicationStatusUpdate(ReplicationStatusUpdate.newBuilder()
                            .setReplicationStatus(ReplicationStatus.REPLICATION_STATUS_STANDALONE)).build());
                    send(status(SlonStatus.STATUS_STARTED));
                    send(status(SlonStatus.STATUS_HEALTHY));
                }
            }
            case STOPDBCOMMAND -> send(status(SlonStatus.STATUS_STOPPED));
            case GETPOSTGRESLOGSCOMMAND -> replyLog(msg.getGetPostgresLogsCommand().getId(), msg.getGetPostgresLogsCommand().getFollow());
            case GETPATRONILOGSCOMMAND -> replyLog(msg.getGetPatroniLogsCommand().getId(), msg.getGetPatroniLogsCommand().getFollow());
            case GETSLONLOGSCOMMAND -> replyLog(msg.getGetSlonLogsCommand().getId(), msg.getGetSlonLogsCommand().getFollow());
            case GETETCDLOGSCOMMAND -> replyLog(msg.getGetEtcdLogsCommand().getId(), msg.getGetEtcdLogsCommand().getFollow());
            case ABORTLOGSCOMMAND -> send(SlonMessage.newBuilder().setLog(Log.newBuilder()   // stop a follow
                    .setId(msg.getAbortLogsCommand().getId()).setStatus(ok())).build());
            case EXECCOMMAND -> {
                var cmd = msg.getExecCommand();
                // Fake `echo <args>` so the same exec scenario passes on mock and real; anything else echoes
                // the command for visibility.
                var args = cmd.getCommandList();
                String out = args.size() >= 2 && args.get(0).equals("echo")
                        ? String.join(" ", args.subList(1, args.size())) + "\n"
                        : "mock exec: " + String.join(" ", args) + "\n";
                // Exec output is 1-byte-channel-framed: the real slon prepends a 0 (stdout) byte, the CLI
                // drops bytes[0] before writing. Omit it and the first real byte gets eaten.
                ByteString framed = ByteString.copyFrom(new byte[]{0}).concat(ByteString.copyFromUtf8(out));
                send(SlonMessage.newBuilder().setExec(ExecMessage.newBuilder()
                        .setId(cmd.getId()).setBytes(framed)).build());
                send(SlonMessage.newBuilder().setExecExit(ExecExit.newBuilder()
                        .setId(cmd.getId()).setCode(0)).build());
            }
            case OPENPGWIRETUNNELCOMMAND -> send(SlonMessage.newBuilder().setPgWireTunnelOpened(
                    PgWireTunnelOpened.newBuilder().setTunnelId(msg.getOpenPgWireTunnelCommand().getTunnelId())).build());
            case PGWIRETUNNELDATA -> {
                var d = msg.getPgWireTunnelData();   // echo the bytes straight back (no real Postgres)
                send(SlonMessage.newBuilder().setPgWireTunnelData(PgWireTunnelData.newBuilder()
                        .setTunnelId(d.getTunnelId()).setData(d.getData())).build());
            }
            case CLOSEPGWIRETUNNELCOMMAND -> send(SlonMessage.newBuilder().setPgWireTunnelClosed(
                    PgWireTunnelClosed.newBuilder().setTunnelId(msg.getClosePgWireTunnelCommand().getTunnelId())).build());
            case SHUTDOWNCOMMAND -> close();
            default -> {
                // diagnostics, exec-message passthrough, etc. — not needed for the current scenarios
            }
        }
    }

    private void replyLog(common.Common.UUID id, boolean follow) {
        send(SlonMessage.newBuilder().setLog(Log.newBuilder().setId(id).setContents("mock slon log line\n")).build());
        if (!follow) {   // a snapshot ends with a terminal status; a follow stays open until AbortLogsCommand
            send(SlonMessage.newBuilder().setLog(Log.newBuilder().setId(id).setStatus(ok())).build());
        }
    }

    private SlonMessage status(SlonStatus slonStatus) {
        return SlonMessage.newBuilder().setStatusUpdate(StatusUpdate.newBuilder()
                .setSlonStatus(slonStatus).setStatus(ok())).build();
    }

    private SlonMessage failed(String message) {
        return SlonMessage.newBuilder().setStatusUpdate(StatusUpdate.newBuilder()
                .setSlonStatus(SlonStatus.STATUS_FAILED)
                .setStatus(com.google.rpc.Status.newBuilder().setCode(9).setMessage(message))).build();
    }

    private static com.google.rpc.Status ok() {
        return com.google.rpc.Status.newBuilder().setCode(0).build();
    }

    private static common.Common.UUID uuid(String value) {
        return common.Common.UUID.newBuilder().setValue(ByteString.copyFromUtf8(value)).build();
    }

    private synchronized void send(SlonMessage msg) {
        toMatriarch.onNext(msg);
    }

    @Override
    public synchronized void close() {
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
}
