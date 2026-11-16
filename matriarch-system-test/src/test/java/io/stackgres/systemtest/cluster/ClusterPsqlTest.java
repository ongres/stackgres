package io.stackgres.systemtest.cluster;

import io.stackgres.proto.types.v1.ClusterStatus;
import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Data-plane: `cluster psql <name>` opens a tunnel, then spawns a local {@code psql} that speaks the Postgres
 * wire protocol to the cluster. Real-only — the byte-echo mock can't complete a wire handshake, so there's
 * nothing meaningful to assert in mock mode (the whole class aborts via {@link #setUp()}).
 *
 * <p>Driven non-interactively: psql has no {@code -c} on the CLI, but it reads SQL from stdin until EOF, and
 * inherits the CLI's stdin — so we feed a query through the PTY ({@link io.stackgres.systemtest.cli.Cli#runPty}).
 *
 * <p>One cluster is created for the class and shared by every scenario (each exercises a different option
 * combination with a read-only query), then deleted.
 */
class ClusterPsqlTest extends BaseNodeTest {

    private String cluster;

    @BeforeAll
    void setUp() {
        requireDirect();   // psql rides the cli.proto data-plane tunnel — not routed over the cloud
        requireReal();
        assumeTrue(onPath("psql"), "requires a local `psql` binary on PATH");
        cluster = "systest-psql-" + Long.toHexString(System.nanoTime());
        assertThat(cli.run("cluster", "create", "-n", cluster).isSuccess()).as("create").isTrue();
        Poll.until(TestConfig.HEALTHY_TIMEOUT, Duration.ofSeconds(1), cluster + " HEALTHY",
                () -> clusterStatus(cluster).filter(s -> s == ClusterStatus.CLUSTER_STATUS_HEALTHY).isPresent());
    }

    @AfterAll
    void tearDown() {
        if (cluster != null) {
            cli.run("cluster", "delete", cluster, "-f");
        }
    }

    /** The basic invocation: `cluster psql <name>` with no extra options — defaults for db, instance, target. */
    @Test
    void psqlWithDefaults() {
        CommandResult psql = cli.runPty("SELECT 6007 * 7001 AS systest_probe;\n",
                List.of("cluster", "psql", cluster));
        assertThat(psql.isSuccess()).as("psql failed: %s", psql.output()).isTrue();
        // The product appears only in the result, never in the echoed input — proves psql ran the query.
        assertThat(psql.output()).as("query result").contains("42055007");   // 6007 * 7001
    }

    /** `-d/--dbname` routes to a named database. */
    @Test
    void psqlWithExplicitDatabase() {
        CommandResult psql = cli.runPty("SELECT current_database();\n",
                List.of("cluster", "psql", cluster, "-d", "postgres"));
        assertThat(psql.isSuccess()).as("psql failed: %s", psql.output()).isTrue();
        assertThat(psql.output()).as("connected database").contains("postgres");
    }

    private static boolean onPath(String binary) {
        for (String dir : System.getenv().getOrDefault("PATH", "").split(":")) {
            if (!dir.isBlank() && new java.io.File(dir, binary).canExecute()) {
                return true;
            }
        }
        return false;
    }
}
