package io.stackgres.systemtest.cluster;

import io.stackgres.proto.types.v1.ClusterStatus;
import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data-plane: `cluster exec <name> -- echo <token>` runs a command in the instance and streams its output
 * back (CLI → cli.proto ExecInCluster → matriarch ExecRelay → slon). Both modes — the mock slon fakes
 * {@code echo}, a real slon runs it in the container. Run under a PTY ({@link io.stackgres.systemtest.cli.Cli#runPty})
 * because exec drives a JLine terminal.
 */
class ClusterExecTest extends BaseNodeTest {

    @Test
    void execRunsCommandInInstance() {
        requireDirect();   // exec is the cli.proto data plane — not routed over the cloud
        String name = "systest-exec-" + Long.toHexString(System.nanoTime());
        String token = "ping-" + Long.toHexString(System.nanoTime());
        try {
            assertThat(cli.run("cluster", "create", "-n", name).isSuccess()).as("create").isTrue();
            Poll.until(TestConfig.HEALTHY_TIMEOUT, Duration.ofSeconds(1), name + " HEALTHY",
                    () -> clusterStatus(name).filter(s -> s == ClusterStatus.CLUSTER_STATUS_HEALTHY).isPresent());

            CommandResult exec = cli.runPty("cluster", "exec", name, "--", "echo", token);
            assertThat(exec.isSuccess()).as("exec failed: %s", exec.output()).isTrue();
            assertThat(exec.output()).contains(token);
        } finally {
            cli.run("cluster", "delete", name, "-f");
        }
    }
}
