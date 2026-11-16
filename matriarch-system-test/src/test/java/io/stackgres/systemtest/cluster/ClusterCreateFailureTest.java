package io.stackgres.systemtest.cluster;

import io.stackgres.proto.api.v1.Cluster;
import io.stackgres.proto.types.v1.ClusterStatus;
import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.agent.MockSlony;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

/**
 * Negative: when the instance can't come up (the mock slon reports FAILED on start), the cluster converges
 * to FAILED — exercising the matriarch's failure path via the agent fault hook.
 */
class ClusterCreateFailureTest extends BaseNodeTest {

    @Test
    void failedWhenInstanceCannotStart() {
        MockSlony mock = requireMock();
        mock.slonFailOnStart(true);
        String name = "systest-fail-" + Long.toHexString(System.nanoTime());
        try {
            cli.run("cluster", "create", "-n", name);   // create may return non-zero; assert the observed status
            Poll.until(TestConfig.HEALTHY_TIMEOUT, Duration.ofSeconds(1), name + " FAILED",
                    () -> status(name).filter(s -> s == ClusterStatus.CLUSTER_STATUS_FAILED).isPresent());
        } finally {
            mock.slonFailOnStart(false);
            cli.run("cluster", "delete", name, "-f");
        }
    }

    private Optional<ClusterStatus> status(String name) {
        return api.listClusters().stream()
                .filter(c -> c.getName().equals(name))
                .map(Cluster::getStatus)
                .findFirst();
    }
}
