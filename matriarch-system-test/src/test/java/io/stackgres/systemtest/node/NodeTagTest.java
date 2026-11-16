package io.stackgres.systemtest.node;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** `stackgres node tag add/remove`, verified through `environment node list --show-tags`. */
class NodeTagTest extends BaseNodeTest {

    @Test
    void addShowRemoveTags() {
        String id = nodeId();

        assertThat(cli.run("environment", "node", "tag", "add", id, "env=prod,role=primary").isSuccess())
                .as("tag add").isTrue();
        assertThat(showTags()).as("both tags visible after add").contains("env=prod").contains("role=primary");

        assertThat(cli.run("environment", "node", "tag", "remove", id, "role").isSuccess())
                .as("tag remove").isTrue();
        assertThat(showTags()).as("env kept, role removed").contains("env=prod").doesNotContain("role=primary");
    }

    /** `environment node list --show-tags` output (fails the test if the command fails). */
    private String showTags() {
        CommandResult r = cli.run("environment", "node", "list", "--show-tags");
        assertThat(r.isSuccess()).as("node list --show-tags failed: %s", r.output()).isTrue();
        return r.output();
    }
}
