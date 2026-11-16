package io.stackgres.systemtest;

/**
 * The agent fidelity for a run — the second axis beside {@link Target} (transport). The scenarios are
 * agent-agnostic; this only decides what provides the node + clusters under the matriarch.
 */
public enum AgentMode {

    /** In-process behavioral mock slony/slon — containerd-free, no Postgres, with fault injection. */
    MOCK,

    /** A real slony (+ real slon containers via containerd) started out-of-band by the environment. */
    REAL;

    public boolean isMock() {
        return this == MOCK;
    }

    public boolean isReal() {
        return this == REAL;
    }
}
