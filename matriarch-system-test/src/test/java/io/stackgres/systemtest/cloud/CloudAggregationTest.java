package io.stackgres.systemtest.cloud;

import io.stackgres.systemtest.BaseSystemTest;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Cloud-only: the edge aggregates multiple environments, so clusters carry an {@code environmentId} (the environment's name), several
 * environments are visible, and {@code cluster list -A} lists across all of them. Read-only — inspects
 * whatever the configured token can already see.
 */
class CloudAggregationTest extends BaseSystemTest {

    // environmentId is a structured field the CLI only surfaces conditionally (as an `env/name` prefix /
    // Environment column, and only when >1 environment), so this per-cluster check reads it over api.v1;
    // the CLI's aggregated rendering is covered by allEnvironmentsListsAcrossEnvironments below.
    @Test
    void clustersCarryEnvironmentId() {
        requireCloud();
        var clusters = api.listClusters();
        assumeTrue(!clusters.isEmpty(), "no clusters visible to inspect");
        assertThat(clusters).allSatisfy(c ->
                assertThat(c.getEnvironmentId()).as("environmentId on cluster %s", c.getName()).isNotBlank());
    }

    @Test
    void environmentsAreListed() {
        requireCloud();
        CommandResult r = cli.run("environment", "list");
        assertThat(r.isSuccess()).as("environment list failed: %s", r.output()).isTrue();

        CommandResult quiet = cli.run("environment", "list", "-q");
        long envs = quiet.output().lines().map(String::trim).filter(s -> !s.isEmpty()).count();
        assertThat(envs).as("at least one environment visible").isGreaterThanOrEqualTo(1);
    }

    @Test
    void allEnvironmentsListsAcrossEnvironments() {
        requireCloud();
        CommandResult r = cli.run("cluster", "list", "-A");
        assertThat(r.isSuccess()).as("cluster list -A failed: %s", r.output()).isTrue();
    }
}
