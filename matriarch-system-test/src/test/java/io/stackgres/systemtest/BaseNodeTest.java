package io.stackgres.systemtest;

import io.stackgres.systemtest.agent.MockSlony;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A scenario needing a registered node/agent under the matriarch.
 * <ul>
 *   <li><b>mock</b> ({@code -Dstackgres.systemtest.agent=mock}, default): starts an in-process
 *       {@link MockSlony} — containerd-free, with fault injection.</li>
 *   <li><b>real</b>: starts nothing; a real slony (+ real slon containers) is expected to be running
 *       already (see {@code env/real-agents.sh}).</li>
 * </ul>
 * Agent-agnostic scenarios use {@link #nodeId()} (which resolves either way); scenarios that need the
 * mock's synthetic behavior (fault injection, pushed events) call {@link #requireMock()} and skip on real.
 */
public abstract class BaseNodeTest extends BaseSystemTest {

    /** Non-null only when {@code agent=mock}. */
    protected MockSlony node;

    @BeforeAll
    void startAgent() {
        // A mock dials the matriarch's plaintext SlonyService directly — only meaningful for a direct target.
        if (TestConfig.AGENT.isMock() && !TestConfig.TARGET.isCloud()) {
            node = MockSlony.start(TestConfig.host(), TestConfig.port(), Map.of());
        }
    }

    @AfterAll
    void stopAgent() {
        if (node != null) {
            node.close();
        }
    }

    /** The current node's id — the mock's, or (real) the one the matriarch reports. */
    protected String nodeId() {
        if (node != null) {
            return node.nodeId();
        }
        // Real: the slony's registration can lag the matriarch/test start, so give it a moment to appear.
        try {
            Poll.until(Duration.ofSeconds(15), Duration.ofSeconds(1), "a registered node (real slony)",
                    () -> !api.listNodes().isEmpty());
        } catch (RuntimeException timeout) {
            throw new AssertionError("no node registered — is a real slony connected and registered? "
                    + "verify with `stackgres environment node list`", timeout);
        }
        return api.listNodes().get(0).getId().getValue();
    }

    /**
     * Like {@link #nodeId()}, but on the cloud a nodeless environment (e.g. k8s-backed) skips the scenario
     * instead of failing — a bare-metal node isn't guaranteed there. On a direct target a node is expected,
     * so this falls through to {@link #nodeId()} (poll-then-fail).
     */
    protected String nodeIdOrSkip() {
        if (node != null) {
            return node.nodeId();
        }
        if (TestConfig.TARGET.isCloud()) {
            var nodes = api.listNodes();
            assumeTrue(!nodes.isEmpty(), "no node in this cloud environment — skipping node scenario");
            return nodes.get(0).getId().getValue();
        }
        return nodeId();
    }

    /** The mock agent, or skip the test: fault injection and pushed events are mock-only. */
    protected MockSlony requireMock() {
        assumeTrue(node != null, "requires agent=mock (synthetic behavior / fault injection)");
        return node;
    }

    /** Skip unless a real agent is running: some scenarios (psql) need a real Postgres, not the byte-echo mock. */
    protected void requireReal() {
        assumeTrue(TestConfig.AGENT.isReal(), "requires agent=real (a real Postgres, not the byte-echo mock)");
    }
}
