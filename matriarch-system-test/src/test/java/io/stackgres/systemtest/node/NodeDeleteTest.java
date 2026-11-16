package io.stackgres.systemtest.node;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * `environment node delete` forgets the node from the matriarch — verified through the CLI: the id is in
 * `environment node list` before, and gone after.
 *
 * <p>Mock-only: delete calls {@code forgetRegistration()} on the matriarch's single slony slot, and a real,
 * still-connected slony does NOT re-register on its own (only on restart) — so deleting the one shared real
 * node would deregister it for every node/slon test that follows. The disposable per-class {@code MockSlony}
 * makes this safe to exercise. (Real node-delete needs a throwaway/second node — a future real-multi-node slice.)
 */
class NodeDeleteTest extends BaseNodeTest {

    @Test
    void deleteForgetsNode() {
        requireMock();
        String id = nodeId();
        assertThat(nodeListIds()).as("node should be registered before delete").contains(id);

        CommandResult delete = cli.run("environment", "node", "delete", id, "-f");
        assertThat(delete.isSuccess()).as("delete failed: %s", delete.output()).isTrue();

        Poll.until(TestConfig.COMMAND_TIMEOUT, Duration.ofSeconds(1), "node " + id + " gone from `node list`",
                () -> !nodeListIds().contains(id));
    }

    /** The node ids the CLI reports (`environment node list -q`, one id per line). */
    private java.util.List<String> nodeListIds() {
        return cli.run("environment", "node", "list", "-q").output()
                .lines().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
