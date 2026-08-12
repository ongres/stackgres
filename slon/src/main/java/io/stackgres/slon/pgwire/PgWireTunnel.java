package io.stackgres.slon.pgwire;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.UUID;
import java.util.function.Consumer;

public class PgWireTunnel {

    private static final System.Logger logger = System.getLogger("PgWireTunnel");

    private final UUID tunnelId;
    private final Socket socket;
    private final Consumer<byte[]> tunnelDataConsumer;
    private final Runnable closeTunnel;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private volatile boolean closed = false;

    public PgWireTunnel(UUID tunnelId, int pgPort, Consumer<byte[]> tunnelDataConsumer, Runnable closeTunnel) throws IOException {
        this.tunnelId = tunnelId;
        this.socket = new Socket("127.0.0.1", pgPort);
        this.tunnelDataConsumer = tunnelDataConsumer;
        this.closeTunnel = closeTunnel;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    public void writeToPostgres(byte[] data) throws IOException {
        if (!closed) {
            outputStream.write(data);
            outputStream.flush();
        }
    }

    public void readFromPostgres() {
        byte[] buffer = new byte[8192];
        try {
            int bytesRead;
            while (!closed && (bytesRead = inputStream.read(buffer)) != -1) {
                byte[] data = new byte[bytesRead];
                System.arraycopy(buffer, 0, data, 0, bytesRead);
                tunnelDataConsumer.accept(data);
            }
        } catch (IOException e) {
            if (!closed) {
                logger.log(System.Logger.Level.ERROR, "Error reading from tunnel {0}: {1}", tunnelId, e.getMessage());
            }
        } finally {
            if (!closed) {
                closeTunnel.run();
            }
        }
    }

    public void close() {
        closed = true;
        try {
            socket.close();
        } catch (IOException e) {
            logger.log(System.Logger.Level.WARNING, "Error closing tunnel socket: {0}", e.getMessage());
        }
    }

}