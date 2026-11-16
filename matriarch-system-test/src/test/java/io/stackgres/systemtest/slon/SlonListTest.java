package io.stackgres.systemtest.slon;

import io.stackgres.systemtest.BaseSystemTest;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The (hidden) `slon` resource commands. Note these reflect a <em>snapshot</em>: the matriarch's
 * {@code ListSlons} returns the instances carried in the slony's registration (captured when the slony
 * registered, from the clusters that existed then) — it is NOT updated when a cluster is created later. So a
 * create-then-list flow can't deterministically see a fresh slon; we smoke-test that the command works and,
 * if anything is listed, that `slon get` resolves it.
 */
class SlonListTest extends BaseSystemTest {

    @Test
    void listSucceedsAndGetResolves() {
        requireDirect();   // `slon` rides the old cli.proto — not routed over the cloud
        CommandResult list = cli.run("slon", "list", "-q");
        assertThat(list.isSuccess()).as("slon list failed: %s", list.output()).isTrue();

        list.output().lines().map(String::trim).filter(s -> !s.isEmpty()).findFirst().ifPresent(id -> {
            CommandResult get = cli.run("slon", "get", id);
            assertThat(get.isSuccess()).as("slon get %s failed: %s", id, get.output()).isTrue();
            assertThat(get.output()).contains(id);
        });
    }
}
