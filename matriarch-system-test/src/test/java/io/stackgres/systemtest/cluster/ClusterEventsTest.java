package io.stackgres.systemtest.cluster;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * After create→HEALTHY, the cluster's event history includes the lifecycle events. Checked through the
 * user-facing command: {@code cluster events <name>} prints each event's type, so we poll it for
 * {@code cluster.healthy}. (Convergence to HEALTHY is still read via the status oracle.)
 */
class ClusterEventsTest extends BaseNodeTest {

    @Test
    void healthyEmitsClusterEvents() {
        String name = "systest-ev-" + Long.toHexString(System.nanoTime());
        try {
            assertThat(cli.run("cluster", "create", "-n", name).isSuccess()).as("create").isTrue();
            waitHealthy(name);

            Poll.until(TestConfig.COMMAND_TIMEOUT, Duration.ofSeconds(1), name + " has a cluster.healthy event",
                    () -> cli.run("cluster", "events", name).output().contains("cluster.healthy"));
        } finally {
            cli.run("cluster", "delete", name, "-f");
        }
    }
}
