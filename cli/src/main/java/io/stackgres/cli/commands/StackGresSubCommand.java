package io.stackgres.cli.commands;

import io.stackgres.cli.Strings;
import picocli.CommandLine;

public abstract class StackGresSubCommand extends StackGresBaseCommand implements Runnable {

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