package io.stackgres.cli.commands.cluster;

import io.stackgres.cli.client.MatriarchClient;
import io.stackgres.cli.client.PgWireTunnel;
import io.stackgres.cli.commands.StackGresSubCommand;
import io.stackgres.postgres.Flavor;
import io.stackgres.postgres.PostgresCluster;
import io.stackgres.proto.cli.PgWireTarget;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Command(name = "psql", description = "Opens a psql terminal to a running PostgreSQL cluster via tunnel", usageHelpWidth = 160)
public class PsqlCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();
    private final CountDownLatch tunnelReady = new CountDownLatch(1);

    @Spec
    CommandSpec spec;

    @Parameters(description = "The cluster name")
    String name;

    @Option(names = {"-i", "--instance"}, description = "The PostgreSQL instance name for high available clusters (default: the only instance or a random one)")
    String instanceName;

    @Option(names = {"-d", "--dbname"}, description = "Database name to connect to")
    String dbname;

    @Option(names = {"--target"}, description = "Which IvorySQL listener to connect to (postgres|ivorysql; default: matches the cluster's flavor). For IvorySQL clusters only.")
    String targetInput;

    private Socket clientSocket;
    private PgWireTunnel tunnel;

    @Override
    public void run() {
        if (debug) client.setDebug();

        PostgresCluster cluster = client.getCluster(name);

        TunnelTarget target = TunnelTarget.fromId(targetInput);
        if (target == null)
            target = (cluster.getFlavor() == Flavor.IVORY_SQL) ? TunnelTarget.IVORY_SQL : TunnelTarget.POSTGRES;

        if (target == TunnelTarget.IVORY_SQL && cluster.getFlavor() != Flavor.IVORY_SQL)
            throw new IllegalArgumentException("--target ivorysql requires a cluster with flavor=ivorysql; cluster '" + cluster.getName() + "' has flavor=" + cluster.getFlavor());

        PgWireTarget protoTarget = (target == TunnelTarget.IVORY_SQL)
                ? PgWireTarget.PG_WIRE_TARGET_IVORY_SQL
                : PgWireTarget.PG_WIRE_TARGET_POSTGRES;

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int localPort = serverSocket.getLocalPort();

            tunnel = client.openPgWireTunnel(
                    cluster,
                    instanceName,
                    protoTarget,
                    true,
                    this::handleTunnelData,
                    this::handleTunnelClosed,
                    this::handleTunnelAborted,
                    this::handleTunnelError
            );

            Thread thread = new Thread(() -> runTunnel(serverSocket));
            thread.setDaemon(true);
            thread.start();

            tunnelReady.await();

            int exitCode = runPsql(cluster, localPort);
            cleanup();
            if (exitCode != 0)
                System.exit(exitCode);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start psql: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void runTunnel(ServerSocket serverSocket) {
        try {
            tunnelReady.countDown();
            clientSocket = serverSocket.accept();

            InputStream in = clientSocket.getInputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                byte[] data = new byte[bytesRead];
                System.arraycopy(buffer, 0, data, 0, bytesRead);
                tunnel.send(data);
            }
        } catch (IOException e) {
            if (debug) errln("Tunnel error: " + e.getMessage(), spec);
        } finally {
            tunnel.close();
        }
    }

    private int runPsql(PostgresCluster cluster, int localPort) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("psql");
        command.add("-h");
        command.add("localhost");
        command.add("-p");
        command.add(String.valueOf(localPort));
        command.add("-U");
        command.add(cluster.getUsername());
        if (dbname != null) {
            command.add("-d");
            command.add(dbname);
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        // Secret-by-reference (§3.7): the cluster object no longer carries the password — fetch it
        // out-of-band via GetClusterCredentials.
        String password = client.getClusterCredentials(cluster.getId().toString());
        if (password != null && !password.isBlank()) {
            pb.environment().put("PGPASSWORD", password);
        }

        // Ignore SIGINT in parent process so only psql handles Ctrl+C
        SignalHandler previousHandler = Signal.handle(new Signal("INT"), signal -> {});
        try {
            Process process = pb.start();
            return process.waitFor();
        } finally {
            Signal.handle(new Signal("INT"), previousHandler);
        }
    }

    private void handleTunnelData(byte[] data) {
        if (clientSocket == null || clientSocket.isClosed())
            return;
        try {
            OutputStream out = clientSocket.getOutputStream();
            out.write(data);
            out.flush();
        } catch (IOException e) {
            if (debug) errln("Error writing to psql: " + e.getMessage(), spec);
        }
    }

    private void handleTunnelClosed() {
        // tunnel closed by server
    }

    private void handleTunnelAborted(String reason) {
        errln("Tunnel aborted: " + reason, spec);
    }

    private void handleTunnelError(String error) {
        errln("Tunnel error: " + error, spec);
    }

    private void cleanup() {
        tunnel.close();
        if (clientSocket != null && !clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                if (debug)
                    errln("Error closing socket: " + e.getMessage(), spec);
            }
        }
    }

}
