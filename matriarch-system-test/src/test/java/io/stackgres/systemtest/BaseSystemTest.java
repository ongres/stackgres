package io.stackgres.systemtest;

import io.stackgres.proto.api.v1.Cluster;
import io.stackgres.proto.types.v1.ClusterStatus;
import io.stackgres.systemtest.cli.Cli;
import io.stackgres.systemtest.grpc.MatriarchApiClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base for every scenario: a {@link Cli} (drives the {@code stackgres} process) and a
 * {@link MatriarchApiClient} (white-box gRPC reads), both wired to the config-resolved {@link Target}.
 * Scenarios are target-agnostic; the active target is chosen purely by {@link TestConfig}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseSystemTest {

    protected Cli cli;
    protected MatriarchApiClient api;

    @BeforeAll
    void baseSetup() {
        cli = new Cli();
        api = new MatriarchApiClient();
    }

    @AfterAll
    void baseTeardown() {
        if (api != null) {
            api.close();
        }
    }

    /** The api.v1 cluster by name, or empty if it doesn't exist (e.g. after delete). */
    protected Optional<Cluster> cluster(String name) {
        return api.listClusters().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst();
    }

    /** The api.v1 status of a cluster by name, or empty if it doesn't exist (e.g. after delete). */
    protected Optional<ClusterStatus> clusterStatus(String name) {
        return cluster(name).map(Cluster::getStatus);
    }

    /** Block until the cluster reports HEALTHY (the create → CREATED → … → HEALTHY machine settled). */
    protected void waitHealthy(String name) {
        waitStatus(name, ClusterStatus.CLUSTER_STATUS_HEALTHY);
    }

    /** Block until the cluster reports {@code status}. */
    protected void waitStatus(String name, ClusterStatus status) {
        Poll.until(TestConfig.HEALTHY_TIMEOUT, Duration.ofSeconds(1), name + " " + status,
                () -> clusterStatus(name).filter(s -> s == status).isPresent());
    }

    /** Block until the cluster no longer exists (delete settled). */
    protected void waitGone(String name) {
        Poll.until(TestConfig.COMMAND_TIMEOUT, Duration.ofMillis(500), name + " gone",
                () -> clusterStatus(name).isEmpty());
    }

    /** Skip unless the current target is the cloud — for scenarios that only exist there (aggregation, REST). */
    protected void requireCloud() {
        assumeTrue(TestConfig.TARGET.isCloud(), "cloud-only scenario");
    }

    /**
     * Skip on the cloud: a few commands still ride the old cli.proto and aren't routed over the cloud — the
     * PgWire data plane ({@code exec}/{@code tunnel}/{@code psql}) plus {@code slon}, {@code diagnostics} and
     * {@code checkpoints}. The CLI returns "not available over the cloud connection" for these; they only
     * work against a direct matriarch. (Cluster/node CRUD, logs and events are api.v1 and run everywhere.)
     */
    protected void requireDirect() {
        assumeTrue(!TestConfig.TARGET.isCloud(), "command is direct-only (not routed over the cloud yet)");
    }
}
