package io.stackgres.systemtest.environment;

import io.stackgres.systemtest.BaseSystemTest;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * `environment list` / `get` / `use`. Self-consistent: it reads a name (an environment's identifier) from the CLI's own `list -q` output
 * and then fetches/selects it, so it works for a direct matriarch (its single environment) and the cloud alike.
 */
class EnvironmentTest extends BaseSystemTest {

    @Test
    void listThenGet() {
        CommandResult list = cli.run("environment", "list");
        assertThat(list.isSuccess()).as("environment list failed: %s", list.output()).isTrue();

        String name = anEnvironmentName();
        assumeTrue(name != null, "no environment reported for this target");

        CommandResult get = cli.run("environment", "get", name);
        assertThat(get.isSuccess()).as("environment get %s failed: %s", name, get.output()).isTrue();
        assertThat(get.output()).contains(name);
    }

    @Test
    void useSelectsAndClearsEnvironment() {
        String name = anEnvironmentName();
        assumeTrue(name != null, "no environment to select");
        // `environment use` persists the selection onto a context, so operate on a throwaway one via --context
        // (keeps it out of the shared config's "current").
        String ctx = "systest-use-" + Long.toHexString(System.nanoTime());
        try {
            assertThat(cli.run("context", "set", ctx, "--endpoint", TestConfig.ENDPOINT,
                    "--tls", String.valueOf(TestConfig.TLS)).isSuccess()).as("context set").isTrue();
            assertThat(cli.run("environment", "use", name, "--context", ctx).isSuccess())
                    .as("environment use %s", name).isTrue();
            assertThat(cli.run("environment", "use", "-", "--context", ctx).isSuccess())
                    .as("environment use - (clear)").isTrue();
        } finally {
            cli.run("context", "delete", ctx);
        }
    }

    private String anEnvironmentName() {
        return cli.run("environment", "list", "-q").output()
                .lines().map(String::trim).filter(s -> !s.isEmpty()).findFirst().orElse(null);
    }
}
