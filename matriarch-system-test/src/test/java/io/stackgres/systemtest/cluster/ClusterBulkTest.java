package io.stackgres.systemtest.cluster;

import io.stackgres.proto.types.v1.ClusterStatus;
import io.stackgres.systemtest.BaseNodeTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The bulk/selection paths of the lifecycle verbs: multiple names in one invocation, `-a/--all`, and
 * `-t key=value` selection. Each scenario names its own clusters and cleans them up, so `-a` only ever
 * acts on this test's clusters.
 */
class ClusterBulkTest extends BaseNodeTest {

    @Test
    void multipleNamesStopStartDelete() {
        String a = "systest-bulk-a-" + Long.toHexString(System.nanoTime());
        String b = "systest-bulk-b-" + Long.toHexString(System.nanoTime());
        try {
            create(a);
            create(b);
            waitHealthy(a);
            waitHealthy(b);

            assertThat(cli.run("cluster", "stop", a, b).isSuccess()).as("stop a b").isTrue();
            waitStatus(a, ClusterStatus.CLUSTER_STATUS_STOPPED);
            waitStatus(b, ClusterStatus.CLUSTER_STATUS_STOPPED);

            assertThat(cli.run("cluster", "start", a, b).isSuccess()).as("start a b").isTrue();
            waitHealthy(a);
            waitHealthy(b);

            assertThat(cli.run("cluster", "delete", a, b, "-f").isSuccess()).as("delete a b").isTrue();
            waitGone(a);
            waitGone(b);
        } finally {
            cli.run("cluster", "delete", a, b, "-f");
        }
    }

    @Test
    void allFlagStopsEveryCluster() {
        String c = "systest-bulk-c-" + Long.toHexString(System.nanoTime());
        String d = "systest-bulk-d-" + Long.toHexString(System.nanoTime());
        try {
            create(c);
            create(d);
            waitHealthy(c);
            waitHealthy(d);

            assertThat(cli.run("cluster", "stop", "-a").isSuccess()).as("stop -a").isTrue();
            waitStatus(c, ClusterStatus.CLUSTER_STATUS_STOPPED);
            waitStatus(d, ClusterStatus.CLUSTER_STATUS_STOPPED);
        } finally {
            cli.run("cluster", "delete", c, d, "-f");
        }
    }

    @Test
    void tagSelectionDeletesOnlyMatching() {
        String del = "systest-bulk-del-" + Long.toHexString(System.nanoTime());
        String keep = "systest-bulk-keep-" + Long.toHexString(System.nanoTime());
        try {
            create(del, "-t", "grp=del");
            create(keep, "-t", "grp=keep");
            waitHealthy(del);
            waitHealthy(keep);

            assertThat(cli.run("cluster", "delete", "-t", "grp=del", "-f").isSuccess()).as("delete -t grp=del").isTrue();
            waitGone(del);
            assertThat(cluster(keep)).as("non-matching cluster kept").isPresent();
        } finally {
            cli.run("cluster", "delete", keep, "-f");
            cli.run("cluster", "delete", del, "-f");
        }
    }

    private void create(String name, String... extra) {
        List<String> args = new ArrayList<>(List.of("cluster", "create", "-n", name));
        args.addAll(List.of(extra));
        assertThat(cli.run(args).isSuccess()).as("create %s", name).isTrue();
    }
}
