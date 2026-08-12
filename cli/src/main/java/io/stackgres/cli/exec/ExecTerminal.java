package io.stackgres.cli.exec;

import com.google.protobuf.ByteString;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;

public class ExecTerminal {

    private static final int SHUTDOWN_TIMEOUT_SECONDS = 2;
    private static final int RESPONSE_TIMEOUT_SECONDS = 2;

    private final Jsonb jsonb = JsonbBuilder.create();
    private final SubmissionPublisher<ByteString> publisher = new PreloadedSubmissionPublisher<>();
    private final Terminal terminal;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "exec-watchdog");
        t.setDaemon(true);
        return t;
    });

    private CompletableFuture<Integer> exit;
    private Runnable closeConnection;
    private volatile boolean shuttingDown;
    private volatile boolean connectionUnresponsive;
    private ScheduledFuture<?> responseWatchdog;

    public ExecTerminal() {
        try {
            this.terminal = TerminalBuilder.terminal();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int start(CompletableFuture<Integer> exit, Runnable closeConnection) {
        try {
            this.exit = exit;
            this.closeConnection = closeConnection;
            terminal.enterRawMode();

            Runtime.getRuntime().addShutdownHook(new Thread(this::handleShutdown));

            // Ctrl+C / SIGINT
            terminal.handle(Terminal.Signal.INT, signal -> handleIntSignal());

            // Ctrl+\ / SIGQUIT doesn't work right now, https://github.com/jline/jline3/issues/912
            terminal.handle(Terminal.Signal.QUIT, signal -> {
            });

            // Ctrl+Z / SIGTSTP (terminal stop) ignored
            terminal.handle(Terminal.Signal.TSTP, signal -> {
            });

            terminal.handle(Terminal.Signal.WINCH, signal -> resize());

            readSystemInput();

            return exit.join();
        } catch (Exception e) {
            if (shuttingDown) {
                // exit code for Ctrl+C (128 + 2 (SIGINT))
                return 130;
            }
            throw new RuntimeException(e);
        } finally {
            scheduler.shutdownNow();
        }
    }

    private void handleShutdown() {
        shuttingDown = true;
        if (closeConnection != null) {
            closeConnection.run();
        }
        publisher.close();
        try {
            exit.get(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            exit.complete(130);
        }
    }

    public void write(ByteString bytes) {
        cancelWatchdog();
        connectionUnresponsive = false;
        try {
            ByteString slice = bytes.substring(1);
            OutputStream output = terminal.output();
            output.write(slice.toByteArray());
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleIntSignal() {
        // Send Ctrl+C to the remote terminal
        publisher.submit(ByteString.copyFrom(new byte[]{0, 3}));
        startWatchdog();
    }

    private synchronized void startWatchdog() {
        if (responseWatchdog == null) {
            responseWatchdog = scheduler.schedule(this::handleUnresponsive, RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    private synchronized void cancelWatchdog() {
        if (responseWatchdog != null) {
            responseWatchdog.cancel(false);
            responseWatchdog = null;
        }
    }

    private void handleUnresponsive() {
        connectionUnresponsive = true;
        try {
            OutputStream output = terminal.output();
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            output.write("Connection unresponsive. Press Ctrl+D to exit.\r\n".getBytes(StandardCharsets.UTF_8));
            output.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    private void forceQuit() {
        shuttingDown = true;
        if (closeConnection != null) {
            closeConnection.run();
        }
        publisher.close();
        exit.complete(130);
    }

    private void readSystemInput() throws IOException {
        resize();
        while (!exit.isDone()) {
            int read = terminal.reader().read(200); // 200ms
            if (read == -1) break; // EOF
            if (read == -2) continue; // timeout

            if (connectionUnresponsive && read == (byte) 4) {
                // Ctrl+D
                forceQuit();
                return;
            }

            publisher.submit(ByteString.copyFrom(new byte[]{0, (byte) read}));
        }
    }

    private void resize() {
        Size size = terminal.getSize();
        resize(size.getColumns(), size.getRows());
    }

    public void resize(int columns, int rows) {
        Map<String, Integer> map = new HashMap<>(4);
        map.put("Height", rows);
        map.put("Width", columns);
        byte[] bytes = jsonb.toJson(map).getBytes(StandardCharsets.UTF_8);
        sendResize(bytes);
    }

    private void sendResize(byte[] buffer) {
        byte[] toSend = new byte[buffer.length + 1];
        toSend[0] = (byte) 4;
        System.arraycopy(buffer, 0, toSend, 1, buffer.length);
        publisher.submit(ByteString.copyFrom(toSend));
    }

    public SubmissionPublisher<ByteString> bytesPublisher() {
        return publisher;
    }

}
