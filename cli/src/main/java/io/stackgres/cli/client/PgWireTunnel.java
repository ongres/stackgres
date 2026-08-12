package io.stackgres.cli.client;

import com.google.protobuf.ByteString;
import io.stackgres.proto.cli.PgWireClientMessage;
import io.stackgres.proto.cli.PgWireTunnelCloseRequest;
import io.stackgres.proto.cli.PgWireTunnelData;
import io.grpc.stub.StreamObserver;

public class PgWireTunnel {

    private final StreamObserver<PgWireClientMessage> requestObserver;
    private volatile boolean closed = false;

    PgWireTunnel(StreamObserver<PgWireClientMessage> requestObserver) {
        this.requestObserver = requestObserver;
    }

    public void send(byte[] data) {
        if (!closed)
            requestObserver.onNext(PgWireClientMessage.newBuilder()
                    .setData(PgWireTunnelData.newBuilder()
                            .setData(ByteString.copyFrom(data))
                            .build())
                    .build());
    }

    public void close() {
        if (!closed) {
            closed = true;
            requestObserver.onNext(PgWireClientMessage.newBuilder()
                    .setClose(PgWireTunnelCloseRequest.newBuilder().build())
                    .build());
            requestObserver.onCompleted();
        }
    }

}