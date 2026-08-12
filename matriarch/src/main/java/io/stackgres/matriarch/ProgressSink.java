package io.stackgres.matriarch;

import io.stackgres.matriarch.model.ClusterOperationProgress;

/**
 * Where an accepted-then-watch operation streams its progress. The caller (a
 * gRPC adapter, the CLI, a test) supplies one; the core drives it. A plain
 * callback rather than a {@code Flow.Publisher} so there is no subscribe/submit
 * race — the sink is live before the first frame — and the core stays zero-dep.
 *
 * <p>Threading: {@link #onProgress} frames are delivered in order and never
 * concurrently; {@link #onComplete} (or {@link #onError}) is terminal and called
 * exactly once.
 */
public interface ProgressSink {

    void onProgress(ClusterOperationProgress progress);

    void onComplete();

    default void onError(Throwable error) {
        onComplete();
    }
}
