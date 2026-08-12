package io.stackgres.slon.processes;

import io.stackgres.proto.slon.TlsConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.Consumer;

public abstract class Processes {

    private static final System.Logger logger = System.getLogger("Processes");

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public abstract void initDb();

    public abstract void configureTls(TlsConfig tlsConfig);

    public abstract void startPostgres(String port);

    public abstract void stopPostgres();

    public abstract boolean healthcheck(String port);

    public abstract String pgControlData();

    public abstract long dbSize(String port);

    protected static void runCommand(ProcessBuilder processBuilder) {
        runCommand(processBuilder, true, true);
    }

    protected static boolean runCommand(ProcessBuilder processBuilder, boolean alwaysLog, boolean throwExceptionOnFailure) {
        try {
            Process process = processBuilder.start();
            boolean success = process.waitFor() == 0;
            String stderr = new String(process.getErrorStream().readAllBytes());
            String stdout = new String(process.getInputStream().readAllBytes());
            if (alwaysLog || !success) {
                System.err.println(stderr);
                System.out.println(stdout);
            }
            if (throwExceptionOnFailure && !success)
                throw new IllegalStateException("Failed to run command: " + stderr);
            return success;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to run command", e);
        }
    }

    protected static String runCommand(ProcessBuilder processBuilder, boolean throwExceptionOnFailure) {
        try {
            Process process = processBuilder.start();
            boolean success = process.waitFor() == 0;
            String stderr = new String(process.getErrorStream().readAllBytes());
            String stdout = new String(process.getInputStream().readAllBytes());
            if (!success) {
                System.err.println(stderr);
                System.out.println(stdout);
            }
            if (throwExceptionOnFailure && !success)
                throw new IllegalStateException("Failed to run command: " + stderr);
            return stdout;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException("Failed to run command", e);
        }
    }

    public int exec(List<String> commands, Consumer<byte[]> consumer, SubmissionPublisher<byte[]> publisher) {
        ExecProcess execProcess = new ExecProcess(commands, consumer, publisher);
        executor.submit(execProcess::readOutput);
        publisher.subscribe(execProcess);
        return execProcess.waitForExit();
    }

    private static class ExecProcess implements Flow.Subscriber<byte[]> {

        private final Process process;
        private final Consumer<byte[]> consumer;
        private volatile boolean running = true;

        ExecProcess(List<String> commands, Consumer<byte[]> consumer, SubmissionPublisher<byte[]> publisher) {
            this.consumer = consumer;
            try {
                // use `script` command to create a PTY wrapper
                List<String> wrappedCommand = List.of("script", "-q", "/dev/null", "-c", String.join(" ", commands));
                ProcessBuilder pb = new ProcessBuilder(wrappedCommand);
                pb.environment().put("TERM", "xterm-256color");
                pb.redirectErrorStream(true);
                this.process = pb.start();
                logger.log(System.Logger.Level.INFO, "Started exec process with PTY: {0}", String.join(" ", commands));
            } catch (IOException e) {
                throw new RuntimeException("Failed to start process: " + e.getMessage(), e);
            }
        }

        void readOutput() {
            try {
                InputStream input = process.getInputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while (running && (bytesRead = input.read(buffer)) != -1) {
                    byte[] data = new byte[bytesRead + 1];
                    data[0] = 0;
                    System.arraycopy(buffer, 0, data, 1, bytesRead);
                    consumer.accept(data);
                }
            } catch (IOException e) {
                logger.log(System.Logger.Level.ERROR, "Error reading process output: {0}", e.getMessage());
            }
        }

        int waitForExit() {
            try {
                int exitCode = process.waitFor();
                running = false;
                return exitCode;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 1;
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(byte[] bytes) {
            if (bytes.length < 1) return;

            int streamType = bytes[0];
            byte[] payload = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, payload, 0, payload.length);

            try {
                switch (streamType) {
                    case 0 -> {
                        OutputStream out = process.getOutputStream();
                        out.write(payload);
                        out.flush();
                    }
                    case 4 -> logger.log(System.Logger.Level.DEBUG, "Resize requested (not supported with script wrapper)");
                    default -> logger.log(System.Logger.Level.WARNING, "Unknown stream type: {0}", streamType);
                }
            } catch (IOException e) {
                logger.log(System.Logger.Level.ERROR, "Cannot write to process: {0}", e.getMessage());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            logger.log(System.Logger.Level.ERROR, "Input stream error: {0}", throwable.getMessage());
        }

        @Override
        public void onComplete() {
            // Input closed
        }
    }

}