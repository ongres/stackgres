package io.stackgres.systemtest.cluster;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * `cluster list` projections and the tag filter: `-q` (names only), `--show-tags`, and `-t key=value`
 * (match + miss). Tags are also checked white-box via api.v1 so the filter assertions can't pass on a
 * false positive.
 */
class ClusterListTest extends BaseNodeTest {

    @Test
    void tagsAndFilters() {
        String name = "systest-list-" + Long.toHexString(System.nanoTime());
        try {
            assertThat(cli.run("cluster", "create", "-n", name, "-t", "team=qa").isSuccess()).as("create").isTrue();
            assertThat(cluster(name).orElseThrow().getTagsMap()).as("tag stored").containsEntry("team", "qa");

            CommandResult quiet = cli.run("cluster", "list", "-q");
            assertThat(quiet.isSuccess()).as("list -q failed: %s", quiet.output()).isTrue();
            assertThat(quiet.output()).contains(name);

            CommandResult showTags = cli.run("cluster", "list", "--show-tags");
            assertThat(showTags.isSuccess()).as("list --show-tags failed: %s", showTags.output()).isTrue();
            assertThat(showTags.output()).contains(name).contains("team").contains("qa");

            CommandResult match = cli.run("cluster", "list", "-t", "team=qa");
            assertThat(match.output()).as("tag match lists the cluster").contains(name);

            CommandResult miss = cli.run("cluster", "list", "-t", "team=nope");
            assertThat(miss.output()).as("non-matching tag excludes the cluster").doesNotContain(name);
        } finally {
            cli.run("cluster", "delete", name, "-f");
        }
    }
}
