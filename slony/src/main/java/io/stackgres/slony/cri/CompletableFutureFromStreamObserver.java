package io.stackgres.slony.cri;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class CompletableFutureFromStreamObserver<T> implements StreamObserver<T> {

    private final CompletableFuture<T> completableFuture = new CompletableFuture<>();
    private T value;

    @Override
    public void onNext(T value) {
        this.value = value;
    }

    @Override
    public void onError(Throwable t) {
        completableFuture.completeExceptionally(t);
    }

    @Override
    public void onCompleted() {
        completableFuture.complete(value);
    }

    public T get() throws ExecutionException, InterruptedException {
        return completableFuture.get();
    }

}
