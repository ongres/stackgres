package io.stackgres.systemtest.cluster;

import io.stackgres.proto.types.v1.ClusterStatus;
import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Lifecycle: stop → STOPPED, start → HEALTHY, restart → HEALTHY (slon driven by StopDb/StartDb commands). */
class ClusterStartStopRestartTest extends BaseNodeTest {

    @Test
    void stopStartRestart() {
        String name = "systest-ls-" + Long.toHexString(System.nanoTime());
        try {
            assertThat(cli.run("cluster", "create", "-n", name).isSuccess()).as("create").isTrue();
            awaitStatus(name, ClusterStatus.CLUSTER_STATUS_HEALTHY);

            assertThat(cli.run("cluster", "stop", name).isSuccess()).as("stop").isTrue();
            awaitStatus(name, ClusterStatus.CLUSTER_STATUS_STOPPED);

            assertThat(cli.run("cluster", "start", name).isSuccess()).as("start").isTrue();
            awaitStatus(name, ClusterStatus.CLUSTER_STATUS_HEALTHY);

            assertThat(cli.run("cluster", "restart", name).isSuccess()).as("restart").isTrue();
            awaitStatus(name, ClusterStatus.CLUSTER_STATUS_HEALTHY);
        } finally {
            cli.run("cluster", "delete", name, "-f");
        }
    }

    private void awaitStatus(String name, ClusterStatus expected) {
        Poll.until(TestConfig.HEALTHY_TIMEOUT, Duration.ofSeconds(1), name + " " + expected,
                () -> clusterStatus(name).filter(s -> s == expected).isPresent());
    }
}
