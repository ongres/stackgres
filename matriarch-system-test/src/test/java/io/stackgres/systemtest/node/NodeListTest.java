package io.stackgres.systemtest.node;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * `environment node list` / `get` and the tag filter. Agent-agnostic — {@link #nodeId()} resolves the
 * registered node in either mode.
 */
class NodeListTest extends BaseNodeTest {

    @Test
    void listAndGet() {
        String id = nodeIdOrSkip();

        CommandResult list = cli.run("environment", "node", "list");
        assertThat(list.isSuccess()).as("node list failed: %s", list.output()).isTrue();
        assertThat(list.output()).contains(id);

        CommandResult quiet = cli.run("environment", "node", "list", "-q");
        assertThat(quiet.output()).contains(id);

        CommandResult tags = cli.run("environment", "node", "list", "--show-tags");
        assertThat(tags.isSuccess()).as("node list --show-tags failed: %s", tags.output()).isTrue();
        assertThat(tags.output()).contains(id);

        CommandResult get = cli.run("environment", "node", "get", id);
        assertThat(get.isSuccess()).as("node get failed: %s", get.output()).isTrue();
        assertThat(get.output()).contains(id);
    }

    @Test
    void tagFilterExcludesUntaggedNode() {
        String id = nodeIdOrSkip();
        CommandResult miss = cli.run("environment", "node", "list", "-t", "systest-absent=1");
        assertThat(miss.isSuccess()).as("node list -t failed: %s", miss.output()).isTrue();
        assertThat(miss.output()).doesNotContain(id);
    }
}
