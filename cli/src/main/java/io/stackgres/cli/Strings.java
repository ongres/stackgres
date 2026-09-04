package io.stackgres.cli;

import java.time.Instant;

import static picocli.CommandLine.Help.Ansi.AUTO;

public final class Strings {

    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    public static String errorAnsi(String string) {
        return AUTO.string("@|red,bold " + string + "|@");
    }

    // Warnings share one warm amber (256-color 214) across the CLI — distinct from errors (red,bold)
    // and comments (grey). picocli markup so AUTO strips it cleanly when output is not a terminal.
    public static String warnAnsi(String string) {
        return AUTO.string("@|fg(214) " + string + "|@");
    }

    public static String commentAnsi(String string) {
        return AUTO.string("\033[38:5:246m" + string + "\033[39m");
    }

    // Relative age lives in Times.ago now; kept here for the "Never" default on absent heartbeats.
    public static String formatTimeAgo(Instant instant, int nowThresholdSeconds) {
        return instant == null ? "Never" : Times.ago(instant, nowThresholdSeconds);
    }

    public static String formatMemory(long memory) {
        if (memory <= 0) return "0 B";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(memory) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format("%.1f %s", memory / Math.pow(1024, digitGroups), units[digitGroups]);
    }

}