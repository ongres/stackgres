package io.stackgres.systemtest.cluster;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Versions;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * `cluster create` options, verified through {@code cluster get} (the command that renders them): `-v`
 * (version), `-p` (port), `-t` (tags), `--node` (placement), and `-f` (a YAML definition). A concrete
 * catalog version is used so {@code get} shows it verbatim. Convergence to HEALTHY is read via the status
 * oracle. ({@code -u} is exercised but not asserted — `cluster get` doesn't print the username; the password/
 * credentials path is covered by the psql scenario.)
 */
class ClusterCreateOptionsTest extends BaseNodeTest {

    @Test
    void versionPortAndTagsShowInGet() {
        String version = concreteVersion();
        assumeTrue(version != null, "catalog reports no concrete postgres version");
        String name = "systest-opts-" + Long.toHexString(System.nanoTime());
        int port = 5455;
        try {
            assertThat(cli.run("cluster", "create", "-n", name, "-v", version,
                    "-p", String.valueOf(port), "-u", "appuser", "-t", "team=qa").isSuccess()).as("create").isTrue();
            waitHealthy(name);

            String out = get(name);
            assertThat(out).as("version").contains(version);
            assertThat(out).as("tags").contains("team=qa");
            if (node != null) {
                assertThat(out).as("mock honors -p").containsPattern("Port:\\s+" + port);
            } else {
                // real: whether the host accepts the exact requested port depends on the slony's port logic;
                // require only that a numeric port was assigned.
                assertThat(out).as("a port was assigned").containsPattern("Port:\\s+\\d+");
            }
        } finally {
            cli.run("cluster", "delete", name, "-f");
        }
    }

    @Test
    void createOnSpecificNode() {
        String name = "systest-opts-node-" + Long.toHexString(System.nanoTime());
        String node = nodeId();
        try {
            assertThat(cli.run("cluster", "create", "-n", name, "--node", node).isSuccess())
                    .as("create --node").isTrue();
            // --node is accepted and the cluster provisions; placement isn't surfaced by `cluster get` over
            // api.v1 (no Node ID line), so we assert convergence rather than the node id.
            waitHealthy(name);
        } finally {
            cli.run("cluster", "delete", name, "-f");
        }
    }

    @Test
    void createFromYamlFile() throws IOException {
        String version = concreteVersion();
        assumeTrue(version != null, "catalog reports no concrete postgres version");
        String name = "systest-opts-yaml-" + Long.toHexString(System.nanoTime());
        Path yaml = Files.createTempFile("systest-cluster", ".yaml");
        try {
            Files.writeString(yaml, """
                    name: '%s'
                    version: '%s'
                    highAvailability: false
                    tags:
                      src: yaml
                    """.formatted(name, version));
            assertThat(cli.run("cluster", "create", "-f", yaml.toString()).isSuccess()).as("create -f").isTrue();
            waitHealthy(name);

            String out = get(name);
            assertThat(out).as("version from yaml").contains(version);
            assertThat(out).as("tags from yaml").contains("src=yaml");
        } finally {
            cli.run("cluster", "delete", name, "-f");
            Files.deleteIfExists(yaml);
        }
    }

    @Test
    void extensionShowsInGet() {
        String name = "systest-opts-ext-" + Long.toHexString(System.nanoTime());
        try {
            CommandResult create = cli.run("cluster", "create", "-n", name, "-e", "amcheck");
            assertThat(create.isSuccess()).as("create -e amcheck: %s", create.output()).isTrue();
            waitHealthy(name);
            assertThat(get(name)).as("the requested extension is listed").contains("amcheck");
        } finally {
            cli.run("cluster", "delete", name, "-f");
        }
    }

    @Test
    void nodeSelectorProvisionsWhenTagMatches() {
        String node = nodeId();
        String name = "systest-opts-sel-" + Long.toHexString(System.nanoTime());
        try {
            // tag the node so the selector has a match, then require the cluster to provision. Placement
            // isn't observable via the CLI, and exclusion (a non-matching selector) needs a multi-node env,
            // so this is the happy path — the option is accepted and provisions when a match exists.
            assertThat(cli.run("environment", "node", "tag", "add", node, "systest-sel=yes").isSuccess())
                    .as("tag node").isTrue();
            assertThat(cli.run("cluster", "create", "-n", name, "--node-selector", "systest-sel=yes").isSuccess())
                    .as("create --node-selector").isTrue();
            waitHealthy(name);
        } finally {
            cli.run("cluster", "delete", name, "-f");
            cli.run("environment", "node", "tag", "remove", node, "systest-sel");
        }
    }

    /** `cluster get <name>` output (fails the test if the command fails). */
    private String get(String name) {
        CommandResult r = cli.run("cluster", "get", name);
        assertThat(r.isSuccess()).as("cluster get failed: %s", r.output()).isTrue();
        return r.output();
    }

    /** The highest concrete {@code <major>.<minor>} the CLI advertises — a real version `get` echoes verbatim. */
    private String concreteVersion() {
        CommandResult r = cli.run("cluster", "version", "list", "-F", "postgres");
        return r.isSuccess() ? Versions.highestConcrete(Versions.concrete(r.output())) : null;
    }
}
