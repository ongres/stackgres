package io.stackgres.systemtest.cluster;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.cli.Cli;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * `cluster logs` snapshots by component and format, in both modes. Mock asserts the slon's synthetic log
 * line; real asserts only that the snapshot came back non-empty (real component logs are host-specific). One
 * shared cluster for the class.
 */
class ClusterLogsTest extends BaseNodeTest {

    private static final String MOCK_LINE = "mock slon log line";
    private String name;

    @BeforeAll
    void setUp() {
        name = "systest-logs-" + Long.toHexString(System.nanoTime());
        assertThat(cli.run("cluster", "create", "-n", name).isSuccess()).as("create").isTrue();
        waitHealthy(name);
    }

    @AfterAll
    void tearDown() {
        if (name != null) {
            cli.run("cluster", "delete", name, "-f");
        }
    }

    @Test
    void postgresComponentPlain() {
        CommandResult r = cli.run("cluster", "logs", name, "-c", "postgres", "--format", "plain");
        assertThat(r.isSuccess()).as("logs -c postgres failed: %s", r.output()).isTrue();
        assertLogContent(r);
    }

    @Test
    void slonComponentPlain() {
        CommandResult r = cli.run("cluster", "logs", name, "-c", "slon", "--format", "plain");
        assertThat(r.isSuccess()).as("logs -c slon failed: %s", r.output()).isTrue();
        assertLogContent(r);
    }

    @Test
    void jsonFormat() {
        CommandResult r = cli.run("cluster", "logs", name, "-c", "slon", "--format", "json");
        assertThat(r.isSuccess()).as("logs --format json failed: %s", r.output()).isTrue();
        assertLogContent(r);
    }

    @Test
    void followStreamsLogs() {
        try (Cli.Background follow = cli.start(List.of("cluster", "logs", name, "-c", "slon", "-f"))) {
            Poll.until(TestConfig.COMMAND_TIMEOUT, Duration.ofMillis(300), "follow streams a log line",
                    () -> follow.isAlive() && (node != null
                            ? follow.output().contains(MOCK_LINE)
                            : !follow.output().isBlank()));
        }
    }

    /** Mock emits a known line; real logs are host-specific, so there we only require the snapshot be non-empty. */
    private void assertLogContent(CommandResult r) {
        if (node != null) {
            assertThat(r.output()).contains(MOCK_LINE);
        } else {
            assertThat(r.output()).as("real component logs should not be empty").isNotBlank();
        }
    }
}
