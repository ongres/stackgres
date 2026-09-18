package io.stackgres.matriarch.model.spec;

/**
 * Desired run intent of a cluster — the durable lifecycle state (§3.6), distinct from the observed
 * {@link io.stackgres.matriarch.model.status.RunStatus} the substrate <em>reports</em>. Persisted in
 * the {@link io.stackgres.matriarch.spi.StateStore} so a stop/start survives a crash; the transition
 * <strong>rules live here in code</strong>, never as data in the store (the store holds only the
 * current intent as a fact).
 */
public enum RunIntent {
    /** The user wants the cluster running. The default for a freshly created cluster. */
    RUNNING,
    /** The user stopped the cluster; it must stay stopped until started again. */
    STOPPED,
    /** Teardown is in flight; no start/stop may be issued until the cluster is gone. */
    DELETING;

    /**
     * Whether a move to {@code target} is legal. RUNNING and STOPPED may freely toggle or move to
     * DELETING; DELETING is terminal (only the removal that clears the intent leaves it). Same-state
     * moves are idempotent and always legal.
     */
    public boolean canTransitionTo(RunIntent target) {
        if (this == target) {
            return true;
        }
        return switch (this) {
            case RUNNING, STOPPED -> true;   // may stop, start, or begin deleting
            case DELETING -> false;          // no start/stop once deleting
        };
    }
}
