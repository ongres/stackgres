package io.stackgres.systemtest.config;

import io.stackgres.systemtest.BaseSystemTest;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLI-local plumbing (no matriarch call): `context set/list/current/use/delete` round-trips against the
 * isolated config home {@link io.stackgres.systemtest.cli.Cli} pins. Uses a uniquely-named context and
 * deletes it, so it can't leak into other scenarios (which drive the endpoint via env vars regardless).
 */
class ContextTest extends BaseSystemTest {

    @Test
    void contextRoundTrip() {
        String ctx = "systest-ctx-" + Long.toHexString(System.nanoTime());
        try {
            assertThat(cli.run("context", "set", ctx, "--endpoint", TestConfig.ENDPOINT, "--tls", "false").isSuccess())
                    .as("context set").isTrue();

            CommandResult list = cli.run("context", "list");
            assertThat(list.isSuccess()).as("context list failed: %s", list.output()).isTrue();
            assertThat(list.output()).contains(ctx);

            assertThat(cli.run("context", "use", ctx).isSuccess()).as("context use").isTrue();
            CommandResult current = cli.run("context", "current");
            assertThat(current.isSuccess()).as("context current failed: %s", current.output()).isTrue();
            assertThat(current.output()).contains(ctx);

            assertThat(cli.run("context", "delete", ctx).isSuccess()).as("context delete").isTrue();
            assertThat(cli.run("context", "list").output()).doesNotContain(ctx);
        } finally {
            cli.run("context", "delete", ctx);   // idempotent cleanup in case an assertion aborted early
        }
    }

    @Test
    void logoutRemovesASession() {
        String ctx = "systest-logout-" + Long.toHexString(System.nanoTime());
        try {
            assertThat(cli.run("context", "set", ctx, "--endpoint", TestConfig.ENDPOINT,
                    "--tls", "false", "--token", "systest-dummy-token").isSuccess())
                    .as("context set with a token").isTrue();
            assertThat(cli.run("logout", ctx).isSuccess()).as("logout removes the saved session").isTrue();
        } finally {
            cli.run("context", "delete", ctx);
        }
    }
}
