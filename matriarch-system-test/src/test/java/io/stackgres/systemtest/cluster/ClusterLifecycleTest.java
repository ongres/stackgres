package io.stackgres.systemtest.cluster;

import io.stackgres.proto.api.v1.Cluster;
import io.stackgres.proto.types.v1.ClusterStatus;
import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Create a (standalone) cluster, watch it reach HEALTHY, delete it — the whole slony/slon orchestration
 * driven by the mock agents (port handshake → CreateClusterCommand → slon InitDb→StartDb→HEALTHY).
 */
class ClusterLifecycleTest extends BaseNodeTest {

    @Test
    void createHealthyDelete() {
        String name = "systest-" + Long.toHexString(System.nanoTime());

        CommandResult create = cli.run("cluster", "create", "-n", name);
        assertThat(create.isSuccess()).as("create failed: %s", create.output()).isTrue();

        Poll.until(TestConfig.HEALTHY_TIMEOUT, Duration.ofSeconds(1), name + " HEALTHY",
                () -> status(name).filter(s -> s == ClusterStatus.CLUSTER_STATUS_HEALTHY).isPresent());

        CommandResult delete = cli.run("cluster", "delete", name, "-f");
        assertThat(delete.isSuccess()).as("delete failed: %s", delete.output()).isTrue();

        Poll.until(TestConfig.COMMAND_TIMEOUT, Duration.ofSeconds(1), name + " gone",
                () -> status(name).isEmpty());
    }

    private Optional<ClusterStatus> status(String name) {
        return api.listClusters().stream()
                .filter(c -> c.getName().equals(name))
                .map(Cluster::getStatus)
                .findFirst();
    }
}
