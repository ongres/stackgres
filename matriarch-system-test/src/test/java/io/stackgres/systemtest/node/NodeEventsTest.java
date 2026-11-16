package io.stackgres.systemtest.node;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.agent.MockSlony;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * `environment node events`. The command itself is exercised in both modes; asserting a <em>specific</em>
 * event requires injecting one, which only the mock can do (a real slony emits events organically and
 * unpredictably).
 */
class NodeEventsTest extends BaseNodeTest {

    @Test
    void eventsCommandSucceeds() {
        CommandResult r = cli.run("environment", "node", "events", nodeIdOrSkip());
        assertThat(r.isSuccess()).as("node events failed: %s", r.output()).isTrue();
    }

    @Test
    void pushedEventShows() {
        MockSlony mock = requireMock();   // synthetic push — only the mock can emit a known event on demand
        mock.pushEvent("systest.ping", Map.of("source", "mock"));
        Poll.until(TestConfig.COMMAND_TIMEOUT, Duration.ofSeconds(1), "node event systest.ping visible",
                () -> cli.run("environment", "node", "events", nodeId()).output().contains("systest.ping"));
    }
}
