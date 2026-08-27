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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Command(name = "tunnel", description = "Opens a tunnel to a PostgreSQL instance")
public class TunnelCommand extends StackGresSubCommand {

    private final MatriarchClient client = new MatriarchClient();
    private final Set<TunnelConnection> activeConnections = ConcurrentHashMap.newKeySet();

    private volatile boolean stopped = false;
    private volatile String error;
    private volatile TunnelConnection pendingConnection;

    @Spec
    CommandSpec spec;

    @Parameters(description = "The cluster name")
    String name;

    @Option(names = {"-p", "--port"}, description = "Local port to listen on (default: 15432 for PostgreSQL, 11521 for IvorySQL)")
    Integer port;

    @Option(names = {"-b", "--bind"}, description = "Bind address (default: 127.0.0.1)", defaultValue = "127.0.0.1")
    String bindAddress;

    @Option(names = {"-i", "--instance"}, description = "The PostgreSQL instance name for high available clusters (default: the only instance or a random one)")
    String instanceName;

    @Option(names = {"--target"}, description = "Which IvorySQL listener to connect to (postgres|ivorysql; default: matches the cluster's flavor). For IvorySQL clusters only.")
    String targetInput;

    private PostgresCluster cluster;
    private TunnelTarget target;
    private ServerSocket serverSocket;

    @Override
    public void run() {
        if (debug) client.setDebug();

        cluster = client.getCluster(name);

        target = TunnelTarget.fromId(targetInput);
        if (target == null)
            target = (cluster.getFlavor() == Flavor.IVORY_SQL) ? TunnelTarget.IVORY_SQL : TunnelTarget.POSTGRES;
        if (target == TunnelTarget.IVORY_SQL && cluster.getFlavor() != Flavor.IVORY_SQL)
            throw new IllegalArgumentException("--target ivorysql requires a cluster with flavor=ivorysql; cluster '" + cluster.getName() + "' has flavor=" + cluster.getFlavor());
        if (port == null)
            port = (target == TunnelTarget.IVORY_SQL) ? 11521 : 15432;
        PgWireTarget protoTarget = (target == TunnelTarget.IVORY_SQL) ? PgWireTarget.PG_WIRE_TARGET_IVORY_SQL : PgWireTarget.PG_WIRE_TARGET_POSTGRES;

        // Open tunnel immediately to validate cluster/instance/slon and reuse for the first client
        pendingConnection = new TunnelConnection();
        PgWireTunnel tunnel = client.openPgWireTunnel(
                cluster,
                instanceName,
                protoTarget,
                true,
                pendingConnection::handleTunnelData,
                pendingConnection::handleTunnelClosed,
                pendingConnection::handleTunnelAborted,
                pendingConnection::handleTunnelError
        );
        pendingConnection.setTunnel(tunnel);

        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try {
            InetAddress bindAddr = InetAddress.getByName(bindAddress);
            serverSocket = new ServerSocket(port, 50, bindAddr);
            outf("Listening on %s:%d -> cluster '%s'%n", bindAddress, port, name);
            outln("Press Ctrl+C to stop");
        } catch (IOException e) {
            throw new RuntimeException("Failed to listen on " + bindAddress + ":" + port + ": " + e.getMessage(), e);
        }

        while (!stopped) {
            try {
                Socket clientSocket = serverSocket.accept();
                if (stopped) {
                    clientSocket.close();
                    break;
                }
                Thread handler = new Thread(() -> handleClient(clientSocket));
                handler.setDaemon(true);
                handler.start();
            } catch (IOException e) {
                if (!stopped)
                    errln("Error accepting connection: " + e.getMessage(), spec);
            }
        }

        if (error != null)
            throw new RuntimeException(error);
    }

    private void handleClient(Socket clientSocket) {
        String clientAddr = clientSocket.getRemoteSocketAddress().toString();
        outln("Connection from " + clientAddr);

        TunnelConnection connection = claimPendingConnection();
        boolean tunnelOpened = connection != null;

        if (connection != null) {
            connection.setSocket(clientSocket);
        } else {
            connection = new TunnelConnection(clientSocket);
        }
        activeConnections.add(connection);

        try {
            if (!tunnelOpened) {
                PgWireTarget protoTarget = (target == TunnelTarget.IVORY_SQL)
                        ? PgWireTarget.PG_WIRE_TARGET_IVORY_SQL
                        : PgWireTarget.PG_WIRE_TARGET_POSTGRES;
                PgWireTunnel tunnel = client.openPgWireTunnel(
                        cluster,
                        instanceName,
                        protoTarget,
                        false,
                        connection::handleTunnelData,
                        connection::handleTunnelClosed,
                        connection::handleTunnelAborted,
                        connection::handleTunnelError
                );
                connection.setTunnel(tunnel);
                tunnelOpened = true;
            }

            InputStream fromClient = clientSocket.getInputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while (!stopped && !clientSocket.isClosed() && (bytesRead = fromClient.read(buffer)) != -1) {
                byte[] data = new byte[bytesRead];
                System.arraycopy(buffer, 0, data, 0, bytesRead);
                connection.tunnel.send(data);
            }
        } catch (Exception e) {
            if (!stopped) {
                if (!tunnelOpened && error == null) {
                    error = e.getMessage();
                    shutdown();
                } else {
                    errln("Connection error: " + e.getMessage(), spec);
                }
            }
        } finally {
            connection.close();
            activeConnections.remove(connection);
            outln("Connection closed: " + clientAddr);
        }
    }

    private synchronized TunnelConnection claimPendingConnection() {
        TunnelConnection connection = pendingConnection;
        pendingConnection = null;
        return connection;
    }

    private void shutdown() {
        if (stopped)
            return;
        stopped = true;
        outln("\nShutting down...");

        for (TunnelConnection connection : activeConnections) {
            connection.close();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed())
                serverSocket.close();
        } catch (IOException e) {
            if (debug) errln("Error closing server socket: " + e.getMessage(), spec);
        }
    }

    private class TunnelConnection {

        private volatile Socket clientSocket;
        private PgWireTunnel tunnel;

        TunnelConnection() {
        }

        TunnelConnection(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        void setSocket(Socket socket) {
            this.clientSocket = socket;
        }

        void setTunnel(PgWireTunnel tunnel) {
            this.tunnel = tunnel;
        }

        void handleTunnelData(byte[] data) {
            Socket socket = clientSocket;
            if (socket == null || socket.isClosed())
                return;
            try {
                OutputStream out = socket.getOutputStream();
                out.write(data);
                out.flush();
            } catch (IOException e) {
                if (!stopped)
                    errln("Error writing to client: " + e.getMessage(), spec);
            }
        }

        void handleTunnelClosed() {
            closeSocket();
        }

        void handleTunnelAborted(String reason) {
            errln("Tunnel aborted: " + reason, spec);
            closeSocket();
            error = reason;
            shutdown();
        }

        void handleTunnelError(String err) {
            errln("Tunnel error: " + err, spec);
            closeSocket();
        }

        void close() {
            if (tunnel != null)
                tunnel.close();
            closeSocket();
        }

        private void closeSocket() {
            Socket socket = clientSocket;
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    if (debug) errln("Error closing socket: " + e.getMessage(), spec);
                }
            }
        }
    }

}