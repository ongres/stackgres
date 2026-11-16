package io.stackgres.systemtest.node;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.agent.MockSlony;
import io.stackgres.systemtest.cli.Cli;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * `environment node logs` end-to-end (CLI → api.v1 TailNodeLogs → matriarch → slony). api.v1, so it runs on
 * any target (it needs a node — {@link #nodeIdOrSkip()} skips a nodeless environment). The snapshot runs in
 * both agent modes — mock asserts its synthetic content, real/cloud just that the tail completed (host log
 * content is unpredictable). The fault case stays mock-only: only the mock can be told to fail the log request.
 */
class NodeLogsTest extends BaseNodeTest {

    @Test
    void snapshotReturnsAgentLogs() {
        if (node != null) {
            node.failLogs(false);
        }
        CommandResult result = cli.run("environment", "node", "logs", nodeIdOrSkip());
        assertThat(result.isSuccess()).as("node logs failed: %s", result.output()).isTrue();
        if (node != null) {
            assertThat(result.output()).contains("mock slony log line");
        }
        // real: the tail completed; content comes from the host and isn't asserted.
    }

    @Test
    void snapshotHonorsFormat() {
        if (node != null) {
            node.failLogs(false);
        }
        String id = nodeIdOrSkip();
        for (String format : new String[]{"plain", "json"}) {
            CommandResult result = cli.run("environment", "node", "logs", id, "--format", format);
            assertThat(result.isSuccess()).as("node logs --format %s failed: %s", format, result.output()).isTrue();
            if (node != null) {
                assertThat(result.output()).as("mock line rendered in %s", format).contains("mock slony log line");
            }
        }
    }

    @Test
    void followStreamsNodeLogs() {
        if (node != null) {
            node.failLogs(false);
        }
        String id = nodeIdOrSkip();
        try (Cli.Background follow = cli.start(List.of("environment", "node", "logs", id, "-f"))) {
            Poll.until(TestConfig.COMMAND_TIMEOUT, Duration.ofMillis(300), "node log follow streams a line",
                    () -> follow.isAlive() && (node != null
                            ? follow.output().contains("mock slony log line")
                            : !follow.output().isBlank()));
        }
    }

    @Test
    void agentLogFailureSurfaces() {
        MockSlony mock = requireMock();   // fault injection — only the mock can fail a log request on demand
        mock.failLogs(true);
        try {
            CommandResult result = cli.run("environment", "node", "logs", nodeId());
            assertThat(result.isSuccess()).as("expected a non-zero exit when the agent fails the log request").isFalse();
        } finally {
            mock.failLogs(false);
        }
    }
}
