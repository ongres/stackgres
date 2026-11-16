package io.stackgres.systemtest.grpc;

import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.netty.NettyChannelBuilder;
import io.stackgres.proto.api.v1.Cluster;
import io.stackgres.proto.api.v1.GetClusterEventsRequest;
import io.stackgres.proto.api.v1.ListClustersRequest;
import io.stackgres.proto.api.v1.ListNodesRequest;
import io.stackgres.proto.api.v1.ListVersionsRequest;
import io.stackgres.proto.api.v1.Node;
import io.stackgres.proto.api.v1.StackGresApiGrpc;
import io.stackgres.proto.types.v1.DatabaseEngine;
import io.stackgres.proto.types.v1.Event;
import io.stackgres.proto.types.v1.Id;
import io.stackgres.systemtest.TestConfig;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Thin api.v1 gRPC client for white-box assertions (fields the CLI output doesn't show). Mirrors the
 * target's transport: plaintext for a direct matriarch; TLS + Bearer-JWT for the cloud.
 */
public final class MatriarchApiClient implements AutoCloseable {

    private final ManagedChannel channel;
    private final StackGresApiGrpc.StackGresApiBlockingStub stub;

    public MatriarchApiClient() {
        NettyChannelBuilder builder = NettyChannelBuilder.forAddress(TestConfig.host(), TestConfig.port());
        channel = (TestConfig.TLS ? builder.useTransportSecurity() : builder.usePlaintext()).build();
        StackGresApiGrpc.StackGresApiBlockingStub s = StackGresApiGrpc.newBlockingStub(channel);
        if (!TestConfig.TOKEN.isBlank()) {
            s = s.withCallCredentials(bearer(TestConfig.TOKEN));
        }
        this.stub = s;
    }

    /** The stub with a per-call deadline, so a hung cloud call fails fast instead of waiting for the edge 524. */
    private StackGresApiGrpc.StackGresApiBlockingStub deadlined() {
        return stub.withDeadlineAfter(TestConfig.COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    }

    public List<Node> listNodes() {
        ListNodesRequest.Builder req = ListNodesRequest.newBuilder();
        if (!TestConfig.ENVIRONMENT.isBlank()) {
            req.setEnvironmentId(TestConfig.ENVIRONMENT);
        }
        return deadlined().listNodes(req.build()).getNodeList();
    }

    public List<Cluster> listClusters() {
        ListClustersRequest.Builder req = ListClustersRequest.newBuilder();
        if (!TestConfig.ENVIRONMENT.isBlank()) {
            req.setEnvironmentId(TestConfig.ENVIRONMENT);
        }
        return deadlined().listClusters(req.build()).getClusterList();
    }

    public List<Event> clusterEvents(String clusterId) {
        GetClusterEventsRequest.Builder req = GetClusterEventsRequest.newBuilder()
                .setClusterId(Id.newBuilder().setValue(clusterId));
        if (!TestConfig.ENVIRONMENT.isBlank()) {
            req.setEnvironmentId(TestConfig.ENVIRONMENT);
        }
        return deadlined().getClusterEvents(req.build()).getEventList();
    }

    /** Versions the catalog advertises — used to pick a valid version to create with (not for catalog tests,
     *  which drive the CLI directly). */
    public List<String> listVersions() {
        return deadlined().listVersions(ListVersionsRequest.newBuilder()
                .setEngine(DatabaseEngine.DATABASE_ENGINE_POSTGRES).build()).getVersionList();
    }

    @Override
    public void close() {
        channel.shutdownNow();
        try {
            channel.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static CallCredentials bearer(String token) {
        return new CallCredentials() {
            @Override
            public void applyRequestMetadata(RequestInfo requestInfo, Executor appExecutor, MetadataApplier applier) {
                Metadata headers = new Metadata();
                headers.put(Metadata.Key.of("authorization", Metadata.ASCII_STRING_MARSHALLER), "Bearer " + token);
                applier.apply(headers);
            }
        };
    }
}
