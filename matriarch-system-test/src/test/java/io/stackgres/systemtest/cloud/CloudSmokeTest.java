package io.stackgres.systemtest.cloud;

import io.stackgres.systemtest.BaseSystemTest;
import io.stackgres.systemtest.cli.CommandResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cloud reachability + auth smoke: the CLI and the gRPC api.v1 client reach the live cloud over TLS with the
 * configured JWT. If TLS or the token is wrong these calls fail, so this is the gate that proves the cloud
 * target is wired before any richer scenario. Read-only.
 */
class CloudSmokeTest extends BaseSystemTest {

    @Test
    void statusAuthenticates() {
        requireCloud();
        CommandResult r = cli.run("status");
        assertThat(r.isSuccess()).as("status failed (TLS/JWT?): %s", r.output()).isTrue();
    }

    @Test
    void serverVersionReachable() {
        requireCloud();
        CommandResult r = cli.run("version");
        assertThat(r.isSuccess()).as("version failed: %s", r.output()).isTrue();
    }

    @Test
    void apiListClustersAuthenticates() {
        requireCloud();
        assertThat(api.listClusters()).as("ListClusters over TLS+JWT").isNotNull();
    }

    @Test
    void apiListVersions() {
        requireCloud();
        assertThat(api.listVersions()).as("catalog over the cloud").isNotEmpty();
    }
}
