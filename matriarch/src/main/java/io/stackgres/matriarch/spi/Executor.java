package io.stackgres.matriarch.spi;

import io.stackgres.matriarch.model.spec.ClusterSpec;
import io.stackgres.matriarch.model.status.ClusterStatus;

import java.util.Set;

/**
 * Pluggable provisioning + lifecycle backend (§3.3) — the "diff-execution layer"
 * of the reconcile engine (§3.6). Level-triggered and <strong>non-blocking</strong>:
 * {@link #apply} and {@link #remove} ensure an end-state and return immediately;
 * the executor drives its substrate (with imperative verbs internally, if it
 * likes) and reports observed status via {@link io.stackgres.matriarch.Matriarch#notifyStatus(ClusterStatus)}.
 * It never waits for HEALTHY and never returns status — that is accepted-then-watch (§9.1).
 *
 * <p><strong>How an executor reaches that sink is a deployment detail, kept off
 * this SPI on purpose:</strong> a Quarkus wrapper wires it with CDI events; a
 * standalone wrapper hands the impl an {@code ObservationSink} directly. The core
 * stays framework-free (P2). Implementations: {@code slony-linux},
 * {@code k8s-stackgres}, {@code k8s-native}, {@code external}.
 */
public interface Executor {

    /**
     * Ensure the substrate matches {@code desired} (create or update). Idempotent, non-blocking.
     */
    void apply(ClusterSpec desired);

    /**
     * Ensure {@code spec}'s cluster is gone. Idempotent, symmetric with {@link #apply}.
     */
    void remove(ClusterSpec spec);

    /**
     * Start a stopped cluster's instances. Non-blocking; convergence to HEALTHY arrives as observed status.
     */
    void start(ClusterSpec spec);

    /**
     * Stop a running cluster's instances (data preserved). Non-blocking; STOPPED arrives as observed status.
     */
    void stop(ClusterSpec spec);

    /**
     * Restart (stop, then start) a running cluster. Non-blocking; HEALTHY arrives as observed status.
     */
    void restart(ClusterSpec spec);

    Set<String> capabilities();

}