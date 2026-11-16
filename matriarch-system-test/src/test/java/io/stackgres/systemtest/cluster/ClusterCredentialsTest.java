package io.stackgres.systemtest.cluster;

import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.cli.Cli;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Real-only: `cluster create -u/-P` set the superuser and its password. Proven by opening a tunnel and
 * connecting with EXACTLY that user + password — a generated or ignored password would be rejected, so a
 * successful {@code select current_user} returning the user proves both options were honored end to end.
 * (The CLI's own `cluster psql` auto-fetches the stored credential, so it can't isolate {@code -P}; here we
 * supply the password explicitly via {@code PGPASSWORD}.)
 */
class ClusterCredentialsTest extends BaseNodeTest {

    @Test
    void createdUserAndPasswordAuthenticate() throws Exception {
        requireDirect();   // the tunnel is the cli.proto data plane — not routed over the cloud
        requireReal();
        assumeTrue(onPath("psql"), "requires a local `psql` binary on PATH");

        String name = "systest-cred-" + Long.toHexString(System.nanoTime());
        String user = "systestuser";
        String password = "systestpw-" + Long.toHexString(System.nanoTime());
        int port = 15988;
        try {
            assertThat(cli.run("cluster", "create", "-n", name, "-u", user, "-P", password).isSuccess())
                    .as("create -u/-P").isTrue();
            waitHealthy(name);

            try (Cli.Background tunnel = cli.start(List.of(
                    "cluster", "tunnel", name, "-p", String.valueOf(port), "-b", "127.0.0.1"))) {
                awaitListening(tunnel, port);

                Process psql = psql(port, user, password, "select current_user");
                String out = new String(psql.getInputStream().readAllBytes());
                assertThat(psql.waitFor(30, TimeUnit.SECONDS)).as("psql finished").isTrue();
                assertThat(psql.exitValue()).as("psql connected with the provided credentials: %s", out).isZero();
                assertThat(out).as("authenticated as the requested superuser").contains(user);
            }
        } finally {
            cli.run("cluster", "delete", name, "-f");
        }
    }

    /** A direct psql (not via the CLI) with an explicit password, through the tunnel's local port. */
    private static Process psql(int port, String user, String password, String sql) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("psql", "-h", "127.0.0.1", "-p", String.valueOf(port),
                "-U", user, "-d", "postgres", "-tAc", sql);
        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(true);
        return pb.start();
    }

    private static void awaitListening(Cli.Background tunnel, int port) throws InterruptedException {
        long deadline = System.nanoTime() + TestConfig.COMMAND_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (!tunnel.isAlive()) {
                throw new IllegalStateException("tunnel exited before it started listening");
            }
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress("127.0.0.1", port), 300);
                return;
            } catch (Exception notYet) {
                Thread.sleep(200);
            }
        }
        throw new IllegalStateException("tunnel never listened on " + port);
    }

    private static boolean onPath(String binary) {
        for (String dir : System.getenv().getOrDefault("PATH", "").split(":")) {
            if (!dir.isBlank() && new File(dir, binary).canExecute()) {
                return true;
            }
        }
        return false;
    }
}
