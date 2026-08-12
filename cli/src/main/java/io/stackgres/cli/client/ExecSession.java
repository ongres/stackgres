package io.stackgres.cli.client;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;

public class ExecSession {

    private final CompletableFuture<Integer> exitCode;
    private final StreamObserver<?> requestObserver;
    private volatile boolean closed = false;

    ExecSession(CompletableFuture<Integer> exitCode, StreamObserver<?> requestObserver) {
        this.exitCode = exitCode;
        this.requestObserver = requestObserver;
    }

    public CompletableFuture<Integer> exitCode() {
        return exitCode;
    }

    public void close() {
        if (!closed) {
            closed = true;
            requestObserver.onCompleted();
        }
    }

}
