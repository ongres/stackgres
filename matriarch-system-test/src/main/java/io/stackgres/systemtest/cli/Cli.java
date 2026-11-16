package io.stackgres.systemtest.cli;

import io.stackgres.systemtest.TestConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Runs the {@code stackgres} CLI as an external process (jar/native-agnostic — the binary is resolved
 * from {@link TestConfig#CLI}), pointed at the active target via the same env vars a user sets
 * ({@code STACKGRES_ENDPOINT_URL}, {@code STACKGRES_TOKEN}). stdout/stderr are drained concurrently to
 * avoid pipe-buffer deadlock. This is the one place the suite shells out.
 */
public final class Cli {

    /**
     * An empty, throwaway config home shared by all CLI invocations this run, so the CLI never reads the
     * developer's {@code ~/.stackgres/config.yaml} — whose saved (cloud) context would otherwise leak
     * TLS/token/environment into a direct-target run.
     */
    private static final String CONFIG_HOME = isolatedHome();

    public CommandResult run(String... args) {
        return run(List.of(args));
    }

    public CommandResult run(List<String> args) {
        try {
            Process process = processBuilder(args).start();
            StreamDrainer out = StreamDrainer.start(process.getInputStream());
            StreamDrainer err = StreamDrainer.start(process.getErrorStream());
            boolean finished = process.waitFor(TestConfig.COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("`stackgres " + String.join(" ", args) + "` timed out after "
                        + TestConfig.COMMAND_TIMEOUT);
            }
            return new CommandResult(process.exitValue(), out.get(), err.get());
        } catch (IOException e) {
            throw new IllegalStateException("failed to run `stackgres " + String.join(" ", args)
                    + "` (is '" + TestConfig.CLI + "' on the PATH?)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted running `stackgres " + String.join(" ", args) + "`", e);
        }
    }

    public CommandResult runPty(String... args) {
        return runPty(null, List.of(args));
    }

    /**
     * Run a CLI command that drives a terminal (JLine) — {@code cluster exec}, {@code psql}. Without a real
     * TTY those degrade to a "dumb terminal" and mangle the byte stream, so we allocate a PTY via the Linux
     * {@code script} command (same approach the old suite used). PTY output is CRLF + ANSI-decorated, so we
     * normalize both before returning.
     *
     * <p>{@code stdin} (when non-null) is fed to the command through the PTY via a heredoc — e.g. SQL for a
     * non-interactive {@code cluster psql} (psql reads stdin until EOF). {@code psql} inherits the CLI's
     * stdin/stdout, so this reaches it.
     */
    public CommandResult runPty(String stdin, List<String> args) {
        StringBuilder cmd = new StringBuilder(TestConfig.CLI);
        for (String arg : args) {
            cmd.append(' ').append(shellQuote(arg));
        }
        // script -q --return -c '<cmd>' /dev/null : -q silences the "Script started/done" banner,
        // --return propagates the wrapped command's exit code (without it `script` always exits 0).
        List<String> wrapped;
        if (stdin == null) {
            wrapped = List.of("script", "-q", "--return", "-c", cmd.toString(), "/dev/null");
        } else {
            // Feed stdin as terminal input: bash heredoc → script's PTY. The inner -c '<cmd>' needs its
            // single quotes escaped since it's embedded in a bash string.
            String bash = "script -q --return -c '" + cmd.toString().replace("'", "'\"'\"'")
                    + "' /dev/null <<'STDIN_EOF'\n" + stdin + "\nSTDIN_EOF";
            wrapped = List.of("bash", "-c", bash);
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(wrapped);
            applyEnv(pb);
            pb.environment().put("TERM", "xterm-256color");
            pb.environment().put("PAGER", "cat");       // keep a pager from intercepting psql output
            pb.environment().put("PSQL_PAGER", "cat");
            Process process = pb.start();
            StreamDrainer out = StreamDrainer.start(process.getInputStream());
            StreamDrainer err = StreamDrainer.start(process.getErrorStream());
            boolean finished = process.waitFor(TestConfig.COMMAND_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("`stackgres " + String.join(" ", args) + "` (pty) timed out after "
                        + TestConfig.COMMAND_TIMEOUT);
            }
            return new CommandResult(process.exitValue(), clean(out.get()), clean(err.get()));
        } catch (IOException e) {
            throw new IllegalStateException("failed to run `stackgres " + String.join(" ", args)
                    + "` under a pty (is 'script' available?)", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted running `stackgres " + String.join(" ", args) + "`", e);
        }
    }

    /** Start a long-lived CLI command (e.g. `cluster tunnel`) in the background; caller closes it. */
    public Background start(List<String> args) {
        try {
            return new Background(processBuilder(args).redirectErrorStream(true).start());
        } catch (IOException e) {
            throw new IllegalStateException("failed to start `stackgres " + String.join(" ", args) + "`", e);
        }
    }

    /**
     * A handle to a background CLI process (e.g. a tunnel, or `logs -f`) — kill it with {@link #close()}.
     * Its merged stdout/stderr is drained continuously so {@link #output()} can be polled while it runs.
     */
    public static final class Background implements AutoCloseable {
        private final Process process;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final Thread drain;

        Background(Process process) {
            this.process = process;
            this.drain = new Thread(() -> {
                try (InputStream in = process.getInputStream()) {
                    byte[] b = new byte[4096];
                    int n;
                    while ((n = in.read(b)) != -1) {
                        synchronized (buffer) {
                            buffer.write(b, 0, n);
                        }
                    }
                } catch (IOException ignored) {
                    // process ended / stream closed — whatever we captured is what output() returns
                }
            });
            this.drain.setDaemon(true);
            this.drain.start();
        }

        public boolean isAlive() {
            return process.isAlive();
        }

        /** Everything the process has written (stdout+stderr merged) so far. */
        public String output() {
            synchronized (buffer) {
                return buffer.toString();
            }
        }

        @Override
        public void close() {
            process.destroyForcibly();
        }
    }

    /** The CLI process with the hermetic env (isolated HOME, explicit endpoint/TLS/token/environment). */
    private ProcessBuilder processBuilder(List<String> args) {
        List<String> cmd = new ArrayList<>();
        cmd.add(TestConfig.CLI);
        cmd.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(cmd);
        applyEnv(pb);
        return pb;
    }

    /** Pin the hermetic env on {@code pb} — shared by the direct and PTY launch paths. */
    private void applyEnv(ProcessBuilder pb) {
        Map<String, String> env = pb.environment();
        // The target is exactly what TestConfig says — never a saved context or a STACKGRES_* the shell
        // carries. Isolate HOME, drop any inherited context, set endpoint + TLS explicitly.
        env.put("HOME", CONFIG_HOME);
        env.remove("STACKGRES_CONTEXT");
        env.put("STACKGRES_ENDPOINT_URL", TestConfig.ENDPOINT);
        env.put("STACKGRES_ENDPOINT_TLS", Boolean.toString(TestConfig.TLS));
        if (TestConfig.TOKEN.isBlank()) {
            env.remove("STACKGRES_TOKEN");
        } else {
            env.put("STACKGRES_TOKEN", TestConfig.TOKEN);
        }
        if (TestConfig.ENVIRONMENT.isBlank()) {
            env.remove("STACKGRES_ENVIRONMENT");
        } else {
            env.put("STACKGRES_ENVIRONMENT", TestConfig.ENVIRONMENT);
        }
    }

    /** Single-quote an arg for the {@code script -c '<cmd>'} shell string, escaping embedded quotes. */
    private static String shellQuote(String arg) {
        return "'" + arg.replace("'", "'\"'\"'") + "'";
    }

    private static final Pattern ANSI =
            Pattern.compile("\\x1B(?:\\[[0-9;?]*[ -/]*[@-~]|\\][^\\x07]*(?:\\x07|\\x1B\\\\)|[()][AB012])");

    /** Strip ANSI escapes and normalize the CRLF a PTY emits, so assertions see plain text. */
    private static String clean(String s) {
        return ANSI.matcher(s).replaceAll("").replace("\r\n", "\n").replace("\r", "\n");
    }

    private static String isolatedHome() {
        try {
            return Files.createTempDirectory("stackgres-systemtest-home").toString();
        } catch (IOException e) {
            throw new UncheckedIOException("could not create an isolated CLI config home", e);
        }
    }

    /** Drains a process stream on its own thread so a full OS pipe buffer can't deadlock waitFor(). */
    private static final class StreamDrainer {
        private final Thread thread;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private StreamDrainer(InputStream in) {
            this.thread = new Thread(() -> {
                try (in) {
                    in.transferTo(buffer);
                } catch (IOException ignored) {
                    // process ended / stream closed — whatever we captured is what we return
                }
            });
            this.thread.setDaemon(true);
        }

        static StreamDrainer start(InputStream in) {
            StreamDrainer d = new StreamDrainer(in);
            d.thread.start();
            return d;
        }

        String get() throws InterruptedException {
            thread.join(TimeUnit.SECONDS.toMillis(5));
            return buffer.toString();
        }
    }
}
