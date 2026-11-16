package io.stackgres.systemtest.cluster;

import io.stackgres.proto.types.v1.ClusterStatus;
import io.stackgres.systemtest.BaseNodeTest;
import io.stackgres.systemtest.Poll;
import io.stackgres.systemtest.TestConfig;
import io.stackgres.systemtest.cli.Cli;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data-plane: a tunnel pipes bytes CLI↔matriarch↔slon. Runs in both modes, verified differently:
 * <ul>
 *   <li><b>mock</b> — the slon echoes raw bytes, so we assert they come back (full CLI→matriarch→slon→back).</li>
 *   <li><b>real</b> — we don't speak the Postgres wire protocol from the test (a partial handshake trips the
 *       relay); instead we assert {@code cluster tunnel} establishes and <em>holds</em> the relay with a client
 *       connected. End-to-end data flow to real Postgres is asserted by {@code ClusterPsqlTest}.</li>
 * </ul>
 *
 * <p>Note: the CLI treats <em>any</em> connection to its listener as a client, so we must not probe the port
 * with a throwaway socket (that would claim and tear down the tunnel). Instead we retry-connect the real
 * socket until the listener is up and do the whole exchange on it.
 */
class ClusterTunnelTest extends BaseNodeTest {

    @Test
    void tunnelRelaysData() throws Exception {
        requireDirect();   // the PgWire tunnel is the cli.proto data plane — not routed over the cloud
        String name = "systest-tun-" + Long.toHexString(System.nanoTime());
        int port = 15987;
        try {
            assertThat(cli.run("cluster", "create", "-n", name).isSuccess()).as("create").isTrue();
            Poll.until(TestConfig.HEALTHY_TIMEOUT, Duration.ofSeconds(1), name + " HEALTHY",
                    () -> clusterStatus(name).filter(s -> s == ClusterStatus.CLUSTER_STATUS_HEALTHY).isPresent());

            try (Cli.Background tunnel = cli.start(List.of(
                    "cluster", "tunnel", name, "-p", String.valueOf(port), "-b", "127.0.0.1"));
                 Socket socket = connect(tunnel, port, TestConfig.COMMAND_TIMEOUT)) {

                // generous read timeout: the echo round-trips CLI→gRPC→matriarch→mock→back, which can lag
                // under full-suite load (the real branch below doesn't read, so this only bounds the mock echo)
                socket.setSoTimeout((int) TestConfig.COMMAND_TIMEOUT.toMillis());

                if (node != null) {
                    // mock: the slon echoes whatever we send
                    byte[] ping = "systest-ping".getBytes(StandardCharsets.UTF_8);
                    OutputStream out = socket.getOutputStream();
                    out.write(ping);
                    out.flush();
                    InputStream in = socket.getInputStream();
                    byte[] echoed = new byte[ping.length];
                    int read = in.readNBytes(echoed, 0, ping.length);
                    assertThat(new String(echoed, 0, read, StandardCharsets.UTF_8)).isEqualTo("systest-ping");
                } else {
                    // real: a client is connected — require the relay to Postgres to hold (not abort). We do
                    // NOT speak the PG wire protocol here (a partial handshake trips the relay); data flow to
                    // real PG is covered by ClusterPsqlTest.
                    Thread.sleep(1_000);
                    assertThat(tunnel.isAlive()).as("cluster tunnel stayed up with a client connected").isTrue();
                }
            }
        } finally {
            cli.run("cluster", "delete", name, "-f");
        }
    }

    /** Retry-connect to the CLI's listener until it accepts (first client reuses the eagerly-opened tunnel). */
    private static Socket connect(Cli.Background tunnel, int port, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (!tunnel.isAlive()) {
                throw new IllegalStateException("tunnel process exited before the listener came up");
            }
            Socket socket = new Socket();
            try {
                socket.connect(new InetSocketAddress("127.0.0.1", port), 300);
                return socket;
            } catch (Exception notYet) {
                closeQuietly(socket);
                Thread.sleep(200);
            }
        }
        throw new IllegalStateException("tunnel never started listening on " + port + " within " + timeout);
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (Exception ignored) {
            // best effort
        }
    }
}
