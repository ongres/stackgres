package io.stackgres.cli.commands;

import io.stackgres.cli.Strings;
import picocli.CommandLine;

import java.util.List;

public abstract class StackGresSubCommand extends StackGresBaseCommand implements Runnable {

    /** An action on a single named target (cluster/environment) that may fail. */
    @FunctionalInterface
    public interface NamedAction {
        void run(String name) throws Exception;
    }

    /**
     * Apply {@code action} to every name best-effort: a per-item ✓/✗ line is added to {@code messages} and
     * failures don't stop the rest. On full success the block's first line becomes a green ✓ roll-up
     * ("{n} {noun}s {verbPast}"); on any failure it throws {@link StackGresPicocliException} with a
     * "{ok} of {n} {noun}s {verbPast}, {failed} failed" summary (rendered below the block by the handler,
     * for a non-zero exit). {@code noun} is the singular target ("cluster"/"environment"); {@code verbPast}
     * the verb ("deleted").
     */
    protected void runBatch(List<String> names, ProgressMessages messages, String noun, String verbPast, NamedAction action) {
        int failed = 0;
        for (String name : names) {
            try {
                action.run(name);
                messages.add("✓ " + name + " " + verbPast);
            } catch (Exception e) {
                failed++;
                messages.add("✗ " + name + ": " + rootMessage(e));
            }
        }
        int total = names.size();
        String nouns = total == 1 ? noun : noun + "s";
        if (failed > 0) {
            int ok = total - failed;
            throw new StackGresPicocliException(ok + " of " + total + " " + nouns + " " + verbPast + ", " + failed + " failed", messages);
        }
        messages.doneAddFirstLine(total + " " + nouns + " " + verbPast);
    }

    /** The exception's own (clean) message, falling back to its cause — for a one-line ✗ entry. */
    protected static String rootMessage(Throwable e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        Throwable cause = e.getCause();
        return cause != null && cause.getMessage() != null ? cause.getMessage() : e.toString();
    }

    protected void out(String message) {
        System.out.print(message);
    }

    protected void outln(String message) {
        System.out.println(message);
    }

    /** A warning (warm amber) on stderr, so it stays out of piped stdout. */
    protected void warn(String message) {
        System.err.println(Strings.warnAnsi(message));
    }

    protected void outf(String format, Object... args) {
        System.out.printf(format, args);
    }

    /** Width of the "Label:" column in every detail view, so labels line up identically across commands. */
    protected static final int LABEL_WIDTH = 14;

    /** Render one aligned "Label:   value" line — the single label style shared by all detail views. */
    protected void field(String label, Object value) {
        outf("%-" + LABEL_WIDTH + "s%s\n", label + ":", value == null ? "" : value);
    }

    protected void errln(String message, CommandLine.Model.CommandSpec spec) {
        System.err.println(spec.commandLine().getColorScheme().errorText(message));
    }

    protected static String formatCpu(double cpu) {
        return cpu == (int) cpu ? String.valueOf((int) cpu) : String.valueOf(cpu);
    }

    protected static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KiB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MiB", bytes / (1024.0 * 1024));
        if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GiB", bytes / (1024.0 * 1024 * 1024));
        return String.format("%.1f TiB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }

}