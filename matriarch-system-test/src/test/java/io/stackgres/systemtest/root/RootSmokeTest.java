package io.stackgres.systemtest.root;

import io.stackgres.systemtest.BaseSystemTest;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Root-level smoke: `version` (client-only needs no server; plain needs the matriarch) and `status`.
 * {@code version --client} is the one command that works with no matriarch at all — a cheap liveness check
 * of the CLI binary and its output formats.
 */
class RootSmokeTest extends BaseSystemTest {

    @Test
    void clientVersion() {
        CommandResult r = cli.run("version", "--client");
        assertThat(r.isSuccess()).as("version --client failed: %s", r.output()).isTrue();
        assertThat(r.output()).isNotBlank();
    }

    @Test
    void clientVersionJson() {
        CommandResult r = cli.run("version", "--client", "-o", "json");
        assertThat(r.isSuccess()).as("version --client -o json failed: %s", r.output()).isTrue();
        assertThat(r.output()).contains("{").contains("}");
    }

    @Test
    void clientVersionYaml() {
        CommandResult r = cli.run("version", "--client", "-o", "yaml");
        assertThat(r.isSuccess()).as("version --client -o yaml failed: %s", r.output()).isTrue();
        assertThat(r.output()).contains(":");
    }

    @Test
    void serverVersion() {
        CommandResult r = cli.run("version");
        assertThat(r.isSuccess()).as("version (server) failed: %s", r.output()).isTrue();
        assertThat(r.output()).isNotBlank();
    }

    @Test
    void status() {
        CommandResult r = cli.run("status");
        assertThat(r.isSuccess()).as("status failed: %s", r.output()).isTrue();
        assertThat(r.output()).isNotBlank();
    }
}
