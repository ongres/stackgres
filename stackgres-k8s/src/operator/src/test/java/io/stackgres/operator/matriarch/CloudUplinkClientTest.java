package io.stackgres.operator.matriarch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.stub.StreamObserver;
import io.stackgres.matriarch.Matriarch;
import io.stackgres.operator.app.OperatorInstallationInfoHolder;
import io.stackgres.proto.api.v1.Environment;
import io.stackgres.proto.control.v1.CloudMessage;
import io.stackgres.proto.control.v1.MatriarchMessage;
import io.stackgres.proto.control.v1.RegistrationAck;
import io.stackgres.proto.control.v1.UplinkGrpc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Acceptance for the matriarch → cloud uplink: stand up a fake {@code Uplink} server, point the client
 * at it, and assert it dials out, presents {@code Authorization: Bearer <token>}, registers its
 * environment (the installation id), and pushes a snapshot when the cloud asks — the real wire
 * behavior, without a live cloud or a populated core.
 */
class CloudUplinkClientTest {

  private final LinkedBlockingQueue<MatriarchMessage> received = new LinkedBlockingQueue<>();
  private final AtomicReference<String> authHeader = new AtomicReference<>();

  private Server server;
  private CloudUplinkClient client;

  @AfterEach
  void tearDown() {
    if (client != null) {
      client.stop();
    }
    if (server != null) {
      server.shutdownNow();
    }
  }

  @Test
  void dialsOutRegistersAndSnapshotsWithBearerToken() throws Exception {
    startFakeCloud();

    Matriarch matriarch = mock(Matriarch.class);
    when(matriarch.listClusters()).thenReturn(List.of());
    OperatorInstallationInfoHolder holder = mock(OperatorInstallationInfoHolder.class);
    when(holder.getInstallationId()).thenReturn("env-test");

    client = new CloudUplinkClient();
    client.matriarch = matriarch;
    client.installationInfoHolder = holder;
    client.enabled = true;
    client.url = "localhost:" + server.getPort();
    client.plaintext = true;
    client.token = "test-jwt";
    client.heartbeatInterval = Duration.ofSeconds(30);

    client.start();

    // First message: Registration (installation id as environment id + kind).
    MatriarchMessage reg = received.poll(5, TimeUnit.SECONDS);
    assertNotNull(reg, "expected a Registration");
    assertTrue(reg.hasRegistration());
    Environment env = reg.getRegistration().getEnvironment();
    assertEquals("env-test", env.getId().getValue());
    assertEquals(Environment.Kind.KIND_K8S_STACKGRES, env.getKind());

    // Second message: the snapshot the cloud asked for in its ack.
    MatriarchMessage snap = received.poll(5, TimeUnit.SECONDS);
    assertNotNull(snap, "expected a StateSnapshot after the ack");
    assertTrue(snap.hasSnapshot());
    assertEquals(0, snap.getSnapshot().getClusterCount());

    // The env credential rode along as a bearer token.
    assertEquals("Bearer test-jwt", authHeader.get());
  }

  private void startFakeCloud() throws Exception {
    UplinkGrpc.UplinkImplBase fake = new UplinkGrpc.UplinkImplBase() {
      @Override
      public StreamObserver<MatriarchMessage> connect(StreamObserver<CloudMessage> down) {
        return new StreamObserver<>() {
          @Override
          public void onNext(MatriarchMessage m) {
            received.add(m);
            if (m.hasRegistration()) {
              down.onNext(CloudMessage.newBuilder()
                  .setAck(RegistrationAck.newBuilder().setSnapshotRequired(true))
                  .build());
            }
          }

          @Override public void onError(Throwable t) { }

          @Override public void onCompleted() { down.onCompleted(); }
        };
      }
    };

    ServerInterceptor authCapture = new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
          ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        authHeader.set(headers.get(
            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)));
        return next.startCall(call, headers);
      }
    };

    server = ServerBuilder.forPort(0).addService(fake).intercept(authCapture).build().start();
  }
}
